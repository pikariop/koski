package fi.oph.koski.opiskeluoikeus

import java.sql.{Date, Timestamp}
import fi.oph.koski.db.PostgresDriverWithJsonSupport.api._
import fi.oph.koski.db.PostgresDriverWithJsonSupport.jsonMethods.{parse => parseJson}
import fi.oph.koski.db.KoskiTables._
import fi.oph.koski.db.{DB, HenkilöRow, QueryMethods, OpiskeluoikeusRow, KoskiTables}
import fi.oph.koski.http.KoskiErrorCategory
import fi.oph.koski.koskiuser.KoskiSpecificSession
import fi.oph.koski.opiskeluoikeus.OpiskeluoikeusQueryFilter._
import fi.oph.koski.servlet.InvalidRequestException
import fi.oph.koski.util.SortOrder.{Ascending, Descending}
import fi.oph.koski.util.Retry.retryWithInterval
import fi.oph.koski.util.{PaginationSettings, QueryPagination, SortOrder}

import rx.Observable.{create => createObservable}
import rx.Observer
import rx.functions.{Func0, Func2}
import rx.lang.scala.Observable
import rx.observables.SyncOnSubscribe.createStateful
import scala.concurrent.duration.DurationInt

class OpiskeluoikeusQueryService(val db: DB) extends QueryMethods {
  private val defaultPagination = QueryPagination(0)

  def oppijaOidsQuery(pagination: Option[PaginationSettings])(implicit user: KoskiSpecificSession): Observable[String] = {
    streamingQuery(defaultPagination.applyPagination(OpiskeluOikeudetWithAccessCheck.map(_.oppijaOid), pagination))
  }

  def muuttuneetOpiskeluoikeudetWithoutAccessCheck(after: Timestamp, afterId: Int, limit: Int): Seq[MuuttunutOpiskeluoikeusRow] = {
    // huom, tässä halutaan myös mitätöidyt ja poistetut, sen takia OpiskeluOikeudet eikä OpiskeluOikeudetWithAccessCheck
    runDbSync(
      OpiskeluOikeudet
        .filter(r => (r.aikaleima === after && r.id > afterId) || (r.aikaleima > after))
        .sortBy(r => (r.aikaleima, r.id))
        .map(r => (r.id, r.aikaleima, r.oppijaOid))
        .take(limit)
        .result
    ).map(MuuttunutOpiskeluoikeusRow.tupled)
  }

  def serverCurrentTimestamp: Timestamp = {
    runDbSync(sql"select current_timestamp".as[Timestamp])(0)
  }

  def opiskeluoikeusQuery(
    filters: List[OpiskeluoikeusQueryFilter],
    sorting: Option[SortOrder],
    paginationSettings: Option[PaginationSettings],
    queryPagination: QueryPagination = defaultPagination
  )(implicit user: KoskiSpecificSession): Observable[(OpiskeluoikeusRow, HenkilöRow, Option[HenkilöRow])] = {
    val query = mkQuery(filters, sorting)
    val paginatedQuery = queryPagination.applyPagination(query, paginationSettings)
    streamingQuery(paginatedQuery)
  }

  def mapKaikkiOpiskeluoikeudetSivuittain[A]
    (pageSize: Int, user: KoskiSpecificSession)
    (mapFn: Seq[OpiskeluoikeusRow] => Seq[A])
  : Observable[A] = {
    mapKaikkiSivuittain(pageSize, user)(kaikkiSivuittain(OpiskeluOikeudetWithAccessCheck(user)))(mapFn)
  }

  private def mapKaikkiSivuittain[A]
    (pageSize: Int, user: KoskiSpecificSession)
    (queryFn: (PaginationSettings, KoskiSpecificSession) => Seq[OpiskeluoikeusRow])
    (mapFn: Seq[OpiskeluoikeusRow] => Seq[A])
  : Observable[A] = {
    processByPage[OpiskeluoikeusRow, A](page => queryFn(PaginationSettings(page, pageSize), user), mapFn)
  }

  private def kaikkiSivuittain(
    query: Query[OpiskeluoikeusTable, OpiskeluoikeusRow, Seq]
  )(
    pagination: PaginationSettings,
    user: KoskiSpecificSession
  ): Seq[OpiskeluoikeusRow] = {
    if (!user.hasGlobalReadAccess) throw new RuntimeException("Query does not make sense without global read access")
    // this approach to pagination ("limit 500 offset 176500") is not perfect (the query gets slower as offset
    // increases), but seems tolerable here (with join to henkilot, as in mkQuery below, it's much slower)
    retryWithInterval(5, intervalMs = 30000) {
      runDbSync(defaultPagination.applyPagination(query.sortBy(_.id), pagination).result, timeout = 5.minutes)
    }
  }

  def mapKaikkiOpiskeluoikeudetSivuittainWithoutAccessCheck[A]
    (pageSize: Int)
      (mapFn: Seq[OpiskeluoikeusRow] => Seq[A])
  : Observable[A] = {
    mapKaikkiSivuittainWithoutAccessCheck(pageSize)(kaikkiSivuittainWithoutAccessCheck(OpiskeluOikeudet))(mapFn)
  }

  private def mapKaikkiSivuittainWithoutAccessCheck[A]
    (pageSize: Int)
      (queryFn: PaginationSettings => Seq[OpiskeluoikeusRow])
      (mapFn: Seq[OpiskeluoikeusRow] => Seq[A])
  : Observable[A] = {
    processByPage[OpiskeluoikeusRow, A](page => queryFn(PaginationSettings(page, pageSize)), mapFn)
  }

  private def kaikkiSivuittainWithoutAccessCheck(
    query: Query[OpiskeluoikeusTable, OpiskeluoikeusRow, Seq]
  )(
    pagination: PaginationSettings
  ): Seq[OpiskeluoikeusRow] = {
    // this approach to pagination ("limit 500 offset 176500") is not perfect (the query gets slower as offset
    // increases), but seems tolerable here (with join to henkilot, as in mkQuery below, it's much slower)
    retryWithInterval(5, intervalMs = 30000) {
      runDbSync(defaultPagination.applyPagination(query.sortBy(_.id), pagination).result, timeout = 5.minutes)
    }
  }

  private def mkQuery(filters: List[OpiskeluoikeusQueryFilter], sorting: Option[SortOrder])(implicit user: KoskiSpecificSession) = {
    val baseQuery = OpiskeluOikeudetWithAccessCheck
      .join(KoskiTables.Henkilöt).on(_.oppijaOid === _.oid)
      .joinLeft(KoskiTables.Henkilöt).on(_._2.masterOid === _.oid)
      .map(stuff => (stuff._1._1, stuff._1._2, stuff._2))

    val query = filters.foldLeft(baseQuery) {
      case (query, OpiskeluoikeusPäättynytAikaisintaan(päivä)) => query.filter(_._1.päättymispäivä >= Date.valueOf(päivä))
      case (query, OpiskeluoikeusPäättynytViimeistään(päivä)) => query.filter(_._1.päättymispäivä <= Date.valueOf(päivä))
      case (query, OpiskeluoikeusAlkanutAikaisintaan(päivä)) => query.filter(_._1.alkamispäivä >= Date.valueOf(päivä))
      case (query, OpiskeluoikeusAlkanutViimeistään(päivä)) => query.filter(_._1.alkamispäivä <= Date.valueOf(päivä))
      case (query, OpiskeluoikeudenTyyppi(tyyppi)) => query.filter(_._1.koulutusmuoto === tyyppi.koodiarvo)
      case (query, OneOfOpiskeluoikeudenTyypit(tyypit)) => query.filter(_._1.koulutusmuoto inSet tyypit.map(_.tyyppi.koodiarvo))
      case (query, SuorituksenTyyppi(tyyppi)) => query.filter(_._1.suoritustyypit.@>(List(tyyppi.koodiarvo)))
      case (query, NotSuorituksenTyyppi(tyyppi)) => query.filter(!_._1.suoritustyypit.@>(List(tyyppi.koodiarvo)))
      case (query, Poistettu(poistettu)) => query.filter(d => d._1.poistettu === poistettu)
      case (query, OpiskeluoikeudenTila(tila)) => query.filter(_._1.data.#>>(List("tila", "opiskeluoikeusjaksot", "-1", "tila", "koodiarvo")) === tila.koodiarvo)
      case (query, OpiskeluoikeusQueryFilter.Toimipiste(toimipisteet)) =>
        val matchers = toimipisteet.map { toimipiste =>
          parseJson(s"""[{"toimipiste":{"oid": "${toimipiste.oid}"}}]""")
        }
        query.filter(_._1.data.+>("suoritukset").@>(matchers.bind.any))
      case (query, Luokkahaku(hakusana)) =>
        query.filter({ case t: (OpiskeluoikeusTable, HenkilöTable, _) => t._1.luokka ilike (hakusana + "%") })
      case (query, IdHaku(ids)) => query.filter(_._1.id inSetBind ids)
      case (query, OppijaOidHaku(oids)) => query.filter {
        case (_, hlö, slave) => (hlö.oid inSetBind oids) || (slave.map(s => s.oid) inSetBind oids)
      }
      case (query, SuoritusJsonHaku(json)) => query.filter(_._1.data.+>("suoritukset").@>(json))
      case (query, MuuttunutEnnen(aikaleima)) => query.filter(_._1.aikaleima < Timestamp.from(aikaleima))
      case (query, MuuttunutJälkeen(aikaleima)) => query.filter(_._1.aikaleima >= Timestamp.from(aikaleima))
      case (query, filter) => throw new InvalidRequestException(KoskiErrorCategory.internalError("Hakua ei ole toteutettu: " + filter))
    }

    def alkamispäivä(tuple: (OpiskeluoikeusTable, HenkilöTable, Rep[Option[HenkilöTable]])) = tuple._1.data.#>>(List("alkamispäivä"))
    def luokka(tuple: (OpiskeluoikeusTable, HenkilöTable, Rep[Option[HenkilöTable]])) = tuple._1.luokka
    def nimi(tuple: (OpiskeluoikeusTable, HenkilöTable, Rep[Option[HenkilöTable]])) = (tuple._2.sukunimi.toLowerCase, tuple._2.etunimet.toLowerCase)
    def nimiDesc(tuple: (OpiskeluoikeusTable, HenkilöTable, Rep[Option[HenkilöTable]])) = (tuple._2.sukunimi.toLowerCase.desc, tuple._2.etunimet.toLowerCase.desc)

    sorting match {
      case None => query
      case Some(Ascending("id")) => query.sortBy(_._1.id)
      case Some(Descending("id")) => query.sortBy(_._1.id.desc)
      case Some(Ascending("oppijaOid")) => query.sortBy { case (oo, henkilö, masterHenkilö) => (masterHenkilö.map(_.oid).getOrElse(henkilö.oid), oo.id) } // slaves and master need to be adjacent for later grouping to work
      case Some(Ascending("nimi")) => query.sortBy(nimi)
      case Some(Descending("nimi")) => query.sortBy(nimiDesc)
      case Some(Ascending("alkamispäivä")) => query.sortBy(tuple => (alkamispäivä(tuple), nimi(tuple)))
      case Some(Descending("alkamispäivä")) => query.sortBy(tuple => (alkamispäivä(tuple).desc, nimiDesc(tuple)))
      case Some(Ascending("luokka")) => query.sortBy(tuple => (luokka(tuple), nimi(tuple)))
      case Some(Descending("luokka")) => query.sortBy(tuple => (luokka(tuple).desc, nimiDesc(tuple)))
      case Some(s) => throw new InvalidRequestException(KoskiErrorCategory.badRequest.queryParam("Epäkelpo järjestyskriteeri: " + s))
    }
  }

  private def processByPage[A, B](loadRows: Int => Seq[A], processRows: Seq[A] => Seq[B]): Observable[B] = {
    import rx.lang.scala.JavaConverters._
    def loadRowsInt(page: Int): (Seq[B], Int, Boolean) = {
      val rows = loadRows(page)
      (processRows(rows), page, rows.isEmpty)
    }
    createObservable(createStateful[(Seq[B], Int, Boolean), Seq[B]](
      (() => loadRowsInt(0)): Func0[_ <: (Seq[B], Int, Boolean)],
      ((state, observer) => {
        val (loadResults, page, done) = state
        observer.onNext(loadResults)
        if (done) {
          observer.onCompleted()
          (Nil, 0, true)
        } else {
          loadRowsInt(page + 1)
        }
      }): Func2[_ >: (Seq[B], Int, Boolean), _ >: Observer[_ >: Seq[B]], _ <: (Seq[B], Int, Boolean)]
    )).asScala.flatMap(Observable.from(_))
  }
}

case class MuuttunutOpiskeluoikeusRow(id: Int, aikaleima: Timestamp, oppijaOid: String)
