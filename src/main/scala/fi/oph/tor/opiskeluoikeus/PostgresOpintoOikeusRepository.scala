package fi.oph.tor.opiskeluoikeus

import com.github.fge.jsonpatch.diff.JsonDiff
import fi.oph.tor.db.PostgresDriverWithJsonSupport.api._
import fi.oph.tor.db.Tables._
import fi.oph.tor.db.TorDatabase.DB
import fi.oph.tor.db._
import fi.oph.tor.history.OpiskeluoikeusHistoryRepository
import fi.oph.tor.http.HttpStatus
import fi.oph.tor.json.Json
import fi.oph.tor.oppija.PossiblyUnverifiedOppijaOid
import fi.oph.tor.schema.Henkilö._
import fi.oph.tor.schema.{FullHenkilö, OpiskeluOikeus}
import fi.oph.tor.tor.{OpiskeluoikeusPäättynytAikaisintaan, OpiskeluoikeusPäättynytViimeistään, QueryFilter, TutkinnonTila}
import fi.oph.tor.toruser.TorUser
import fi.oph.tor.util.ReactiveStreamsToRx
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods
import rx.lang.scala.Observable
import slick.dbio
import slick.dbio.Effect.{Read, Write}

class PostgresOpiskeluOikeusRepository(db: DB, historyRepository: OpiskeluoikeusHistoryRepository) extends OpiskeluOikeusRepository with Futures with GlobalExecutionContext with Logging {
  // Note: this is a naive implementation. All filtering should be moved to query-level instead of in-memory-level
  override def filterOppijat(oppijat: Seq[FullHenkilö])(implicit userContext: TorUser) = {
    val query: Query[OpiskeluOikeusTable, OpiskeluOikeusRow, Seq] = for {
      oo <- OpiskeluOikeudet
      if oo.oppijaOid inSetBind oppijat.map(_.oid)
    } yield {
      oo
    }

    //println(query.result.statements.head)

    val fullQuery: Query[Rep[String], String, Seq] = queryWithAccessCheck(query).map(_.oppijaOid)

    val oikeudet: Set[String] = await(db.run(fullQuery.result)).toSet

    oppijat.filter { oppija => oikeudet.contains(oppija.oid)}
  }


  override def findByOppijaOid(oid: String)(implicit userContext: TorUser): Seq[OpiskeluOikeus] = {
    await(db.run(findByOppijaOidAction(oid)))
  }

  override def find(identifier: OpiskeluOikeusIdentifier)(implicit userContext: TorUser): Either[HttpStatus, Option[OpiskeluOikeus]] = {
    await(db.run(findByIdentifierAction(identifier)))
  }

  override def query(filters: List[QueryFilter])(implicit userContext: TorUser): Observable[(Oid, List[OpiskeluOikeus])] = {
    import ReactiveStreamsToRx._

    val query: Query[OpiskeluOikeusTable, OpiskeluOikeusRow, Seq] = queryWithAccessCheck(filters.foldLeft(OpiskeluOikeudet.asInstanceOf[Query[OpiskeluOikeusTable, OpiskeluOikeusRow, Seq]]) {
      case (query, OpiskeluoikeusPäättynytAikaisintaan(päivä)) => query.filter(_.data.#>>(List("päättymispäivä")) >= päivä.toString)
      case (query, OpiskeluoikeusPäättynytViimeistään(päivä)) => query.filter(_.data.#>>(List("päättymispäivä")) <= päivä.toString)
      case (query, TutkinnonTila(tila)) => query.filter(_.data.#>>(List("suoritus", "tila", "koodiarvo")) === tila)
    }).sortBy(_.oppijaOid)

    // Note: it won't actually stream unless you use both `transactionally` and `fetchSize`. It'll collect all the data into memory.

    val observable: Observable[(String, OpiskeluOikeus)] = db.stream(query.result.transactionally.withStatementParameters(fetchSize = 1000)).mapResult { row =>
      (row.oppijaOid, row.toOpiskeluOikeus) // TODO: ehkä siirrä tämäkin käsittely Rx-puolelle
    }.publish.refCount

    val buffered: Observable[List[(String, OpiskeluOikeus)]] = observable.tumblingBuffer(observable.map(_._1).distinctUntilChanged.drop(1)).map(_.toList)

    buffered.flatMap {
      case oikeudet@((personOid, opiskeluOikeus) :: _) =>
        assert(oikeudet.map(_._1).toSet == Set(personOid), "Usean ja/tai väärien henkilöiden tietoja henkilöllä " + personOid + ": " + oikeudet)
        Observable.just((personOid, oikeudet.map(_._2)))
      case _ => Observable.empty
    }
  }

  override def createOrUpdate(oppijaOid: PossiblyUnverifiedOppijaOid, opiskeluOikeus: OpiskeluOikeus)(implicit userContext: TorUser): Either[HttpStatus, CreateOrUpdateResult] = {
    if (!userContext.userOrganisations.hasReadAccess(opiskeluOikeus.oppilaitos)) {
      Left(HttpStatus.forbidden("Ei oikeuksia organisatioon " + opiskeluOikeus.oppilaitos.oid))
    } else {
      await(db.run(createOrUpdateAction(oppijaOid, opiskeluOikeus)))
    }
  }

  private def findByOppijaOidAction(oid: String)(implicit userContext: TorUser): dbio.DBIOAction[Seq[OpiskeluOikeus], NoStream, Read] = {
    findAction(OpiskeluOikeudet.filter(_.oppijaOid === oid))
  }

  private def findByIdentifierAction(identifier: OpiskeluOikeusIdentifier)(implicit userContext: TorUser): dbio.DBIOAction[Either[HttpStatus, Option[OpiskeluOikeus]], NoStream, Read] = identifier match{
    case PrimaryKey(id) => {
      findAction(OpiskeluOikeudet.filter(_.id === id)).map { rows =>
        rows.headOption match {
          case Some(oikeus) => Right(Some(oikeus))
          case None => Left(HttpStatus.notFound("Opiskeluoikeus not found for id: " + id))
        }
      }
    }

    case IdentifyingSetOfFields(oppijaOid, _, _, _) => {
      findByOppijaOidAction(oppijaOid)
        .map(rows => Right(rows.find({ row =>
        new IdentifyingSetOfFields(oppijaOid, row) == identifier
      })))
    }
  }

  private def findAction(query: Query[OpiskeluOikeusTable, OpiskeluOikeusRow, Seq])(implicit userContext: TorUser): dbio.DBIOAction[Seq[OpiskeluOikeus], NoStream, Read] = {
    queryWithAccessCheck(query).result.map(rows => rows.map(_.toOpiskeluOikeus))
  }

  private def createOrUpdateAction(oppijaOid: PossiblyUnverifiedOppijaOid, opiskeluOikeus: OpiskeluOikeus)(implicit userContext: TorUser) = {
    findByIdentifierAction(OpiskeluOikeusIdentifier(oppijaOid.oppijaOid, opiskeluOikeus)).flatMap { opiskeluOikeudet: Either[HttpStatus, Option[OpiskeluOikeus]] =>
      opiskeluOikeudet match {
        case Right(Some(vanhaOpiskeluOikeus)) =>
          updateAction(oppijaOid.oppijaOid, vanhaOpiskeluOikeus, opiskeluOikeus.copy(id = vanhaOpiskeluOikeus.id)).map {
            case error if error.isError => Left(error)
            case _ => Right(Updated(vanhaOpiskeluOikeus.id.get))
          }
        case Right(None) =>
          oppijaOid.verifiedOid match {
            case Some(oid) => createAction(oid, opiskeluOikeus).map(result => result.right.map(Created(_)))
            case None => DBIO.successful(Left(HttpStatus.notFound("Oppija " + oppijaOid.oppijaOid + " not found")))
          }
        case Left(err) => DBIO.successful(Left(err))
      }
    }
  }

  private def createAction(oppijaOid: String, opiskeluOikeus: OpiskeluOikeus)(implicit userContext: TorUser): dbio.DBIOAction[Either[HttpStatus, Int], NoStream, Write] = {
    for {
      opiskeluoikeusId <- OpiskeluOikeudet.returning(OpiskeluOikeudet.map(_.id)) += new OpiskeluOikeusRow(oppijaOid, opiskeluOikeus)
      diff = Json.toJValue(List(Map("op" -> "add", "path" -> "", "value" -> opiskeluOikeus)))
      _ <- historyRepository.createAction(opiskeluoikeusId, userContext.user.oid, diff)
    } yield {
      Right(opiskeluoikeusId)
    }
  }

  private def updateAction(oppijaOid: String, vanhaOpiskeluoikeus: OpiskeluOikeus, uusiOpiskeluoikeus: OpiskeluOikeus)(implicit userContext: TorUser): dbio.DBIOAction[HttpStatus, NoStream, Write] = {
    // TODO: always overriding existing data can not be the eventual update strategy
    for {
      rowsUpdated <- OpiskeluOikeudet.filter(_.id === uusiOpiskeluoikeus.id.get).map(_.data).update(new OpiskeluOikeusRow(oppijaOid, uusiOpiskeluoikeus).data)
      diff = JsonMethods.fromJsonNode(JsonDiff.asJson(JsonMethods.asJsonNode(Json.toJValue(vanhaOpiskeluoikeus)), JsonMethods.asJsonNode(Json.toJValue(uusiOpiskeluoikeus))))
      _ <- historyRepository.createAction(vanhaOpiskeluoikeus.id.get, userContext.user.oid, diff)
    } yield {
      rowsUpdated match {
        case 1 => HttpStatus.ok
        case x =>
          logger.error("Unexpected number of updated rows: " + x)
          HttpStatus.internalError()
      }
    }
  }

  private def queryWithAccessCheck(query: PostgresDriverWithJsonSupport.api.Query[OpiskeluOikeusTable, OpiskeluOikeusRow, Seq])(implicit userContext: TorUser): Query[OpiskeluOikeusTable, OpiskeluOikeusRow, Seq] = {
    val oids = userContext.userOrganisations.oids
    val queryWithAccessCheck = for (
      oo <- query
      if oo.data.#>>(List("oppilaitos", "oid")) inSetBind oids)
    yield {
      oo
    }
    queryWithAccessCheck
  }
}

