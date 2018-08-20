package fi.oph.koski.raportointikanta

import com.typesafe.config.Config
import fi.oph.koski.db.KoskiDatabase._
import fi.oph.koski.db.{KoskiDatabaseConfig, KoskiDatabaseMethods}
import fi.oph.koski.log.Logging
import scala.concurrent.duration._
import slick.driver.PostgresDriver
import slick.dbio.DBIO
import fi.oph.koski.db.PostgresDriverWithJsonSupport.api._
import fi.oph.koski.raportointikanta.RaportointiDatabaseSchema._
import fi.oph.koski.util.Futures
import java.sql.{Date, Timestamp}
import java.time.LocalDate
import fi.oph.koski.util.DateOrdering.sqlDateOrdering
import fi.oph.koski.schema.Organisaatio
import slick.lifted.QueryBase

object RaportointiDatabase {
  type DB = PostgresDriver.backend.DatabaseDef
}

class RaportointiDatabase(val config: Config) extends Logging with KoskiDatabaseMethods {
  val db: DB = KoskiDatabaseConfig(config, raportointi = true).toSlickDatabase

  private[raportointikanta] val ROpiskeluoikeudet = TableQuery[ROpiskeluoikeusTable]
  private[raportointikanta] val ROpiskeluoikeusAikajaksot = TableQuery[ROpiskeluoikeusAikajaksoTable]
  private[raportointikanta] val RPäätasonSuoritukset = TableQuery[RPäätasonSuoritusTable]
  private[raportointikanta] val ROsasuoritukset = TableQuery[ROsasuoritusTable]
  private[raportointikanta] val RHenkilöt = TableQuery[RHenkilöTable]
  private[raportointikanta] val ROrganisaatiot = TableQuery[ROrganisaatioTable]
  private[raportointikanta] val RKoodistoKoodit = TableQuery[RKoodistoKoodiTable]
  private[raportointikanta] val RaportointikantaStatus = TableQuery[RaportointikantaStatusTable]

  def dropAndCreateSchema: Unit = {
    runDbSync(DBIO.seq(
      RaportointiDatabaseSchema.dropAllIfExists,
      ROpiskeluoikeudet.schema.create,
      ROpiskeluoikeusAikajaksot.schema.create,
      RPäätasonSuoritukset.schema.create,
      ROsasuoritukset.schema.create,
      RHenkilöt.schema.create,
      ROrganisaatiot.schema.create,
      RKoodistoKoodit.schema.create,
      RaportointikantaStatus.schema.create,
      RaportointiDatabaseSchema.createOtherIndexes
    ))
  }
  def createOpiskeluoikeusIndexes: Unit = {
    // plain "runDbSync" times out after 1 minute, which is too short here
    Futures.await(db.run(RaportointiDatabaseSchema.createOpiskeluoikeusIndexes), atMost = 15.minutes)
  }

  def deleteOpiskeluoikeudet: Unit =
    runDbSync(ROpiskeluoikeudet.schema.truncate)
  def loadOpiskeluoikeudet(opiskeluoikeudet: Seq[ROpiskeluoikeusRow]): Unit =
    runDbSync(ROpiskeluoikeudet ++= opiskeluoikeudet)
  def oppijaOidsFromOpiskeluoikeudet: Seq[String] = {
    // plain "runDbSync" times out after 1 minute, which is too short here
    Futures.await(db.run(ROpiskeluoikeudet.map(_.oppijaOid).distinct.result), atMost = 15.minutes)
  }

  def deleteOpiskeluoikeusAikajaksot: Unit =
    runDbSync(ROpiskeluoikeusAikajaksot.schema.truncate)
  def loadOpiskeluoikeusAikajaksot(jaksot: Seq[ROpiskeluoikeusAikajaksoRow]): Unit =
    runDbSync(ROpiskeluoikeusAikajaksot ++= jaksot)

  def deletePäätasonSuoritukset: Unit =
    runDbSync(RPäätasonSuoritukset.schema.truncate)
  def loadPäätasonSuoritukset(suoritukset: Seq[RPäätasonSuoritusRow]): Unit =
    runDbSync(RPäätasonSuoritukset ++= suoritukset)
  def deleteOsasuoritukset: Unit =
    runDbSync(ROsasuoritukset.schema.truncate)
  def loadOsasuoritukset(suoritukset: Seq[ROsasuoritusRow]): Unit =
    runDbSync(ROsasuoritukset ++= suoritukset)


  def deleteHenkilöt: Unit =
    runDbSync(RHenkilöt.schema.truncate)
  def loadHenkilöt(henkilöt: Seq[RHenkilöRow]): Unit =
    runDbSync(RHenkilöt ++= henkilöt)

  def deleteOrganisaatiot: Unit =
    runDbSync(ROrganisaatiot.schema.truncate)
  def loadOrganisaatiot(organisaatiot: Seq[ROrganisaatioRow]): Unit =
    runDbSync(ROrganisaatiot ++= organisaatiot)

  def deleteKoodistoKoodit(koodistoUri: String): Unit =
    runDbSync(RKoodistoKoodit.filter(_.koodistoUri === koodistoUri).delete)
  def loadKoodistoKoodit(koodit: Seq[RKoodistoKoodiRow]): Unit =
    runDbSync(RKoodistoKoodit ++= koodit)

  def setStatusLoadStarted(name: String): Unit =
    runDbSync(sqlu"insert into raportointikanta_status (name, load_started, load_completed) values ($name, now(), null) on conflict (name) do update set load_started = now(), load_completed = null")
  def setStatusLoadCompleted(name: String): Unit =
    runDbSync(sqlu"update raportointikanta_status set load_completed=now() where name = $name")
  def statuses: Seq[RaportointikantaStatusRow] =
    runDbSync(RaportointikantaStatus.result)

  def opiskeluoikeusAikajaksot(oppilaitos: Organisaatio.Oid, alku: LocalDate, loppu: LocalDate): Seq[(ROpiskeluoikeusRow, Option[RHenkilöRow], Seq[ROpiskeluoikeusAikajaksoRow], Seq[RPäätasonSuoritusRow])] = {
    val alkuDate = Date.valueOf(alku)
    val loppuDate = Date.valueOf(loppu)
    val query1 = ROpiskeluoikeudet
      .filter(_.oppilaitosOid === oppilaitos)
      .join(ROpiskeluoikeusAikajaksot.filterNot(_.alku > loppuDate).filterNot(_.loppu < alkuDate))
      .on(_.opiskeluoikeusOid === _.opiskeluoikeusOid)
      .joinLeft(RHenkilöt)
      .on(_._1.oppijaOid === _.oppijaOid)
      .map { case ((opiskeluoikeus, aikajakso), henkilo) => (opiskeluoikeus, henkilo, aikajakso) }
      .sortBy(_._1.opiskeluoikeusOid)
    val result1: Seq[(ROpiskeluoikeusRow, Option[RHenkilöRow], ROpiskeluoikeusAikajaksoRow)] = runDbSync(query1.result)
    val query2 = RPäätasonSuoritukset.filter(_.opiskeluoikeusOid inSet result1.map(_._1.opiskeluoikeusOid).distinct)
    val result2: Map[String, Seq[RPäätasonSuoritusRow]] = runDbSync(query2.result).groupBy(_.opiskeluoikeusOid)
    // group rows belonging to same opiskeluoikeus
    result1
      .foldRight[List[(ROpiskeluoikeusRow, Option[RHenkilöRow], List[ROpiskeluoikeusAikajaksoRow])]](List.empty) {
        case (t, head :: tail) if t._1.opiskeluoikeusOid == head._1.opiskeluoikeusOid => (head._1, head._2, t._3 :: head._3) :: tail
        case (t, acc) => (t._1, t._2, List(t._3)) :: acc
      }
      .map(t => (t._1, t._2, t._3.map(_.truncateToDates(alkuDate, loppuDate)).sortBy(_.alku)(sqlDateOrdering), result2.getOrElse(t._1.opiskeluoikeusOid, Seq.empty).sortBy(_.päätasonSuoritusId)))
  }
}
