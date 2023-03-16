package fi.oph.koski.raportointikanta

import fi.oph.koski.db.{DB, KoskiOpiskeluoikeusRow, OpiskeluoikeusRow, PoistettuOpiskeluoikeusRow, YtrOpiskeluoikeusRow}
import fi.oph.koski.json.JsonManipulation
import fi.oph.koski.koskiuser.KoskiSpecificSession
import fi.oph.koski.log.Logging
import fi.oph.koski.opiskeluoikeus.{OpiskeluoikeusQueryService, PäivitetytOpiskeluoikeudetJonoService}
import fi.oph.koski.raportointikanta.LoaderUtils.{convertKoodisto, convertLocalizedString}
import fi.oph.koski.schema._
import fi.oph.koski.suostumus.SuostumuksenPeruutusService
import fi.oph.koski.util.TimeConversions.toTimestamp
import fi.oph.koski.validation.MaksuttomuusValidation
import org.json4s.JValue
import rx.lang.scala.{Observable, Subscriber}

import java.sql.{Date, Timestamp}
import java.time.temporal.ChronoField
import java.time.{LocalDateTime, ZonedDateTime}
import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Try

object OpiskeluoikeusLoader extends Logging {
  val DefaultBatchSize = 500

  private val statusName = "opiskeluoikeudet"
  private val mitätöidytStatusName = "mitätöidyt_opiskeluoikeudet"

  // opiskeluOikeudenLatausEpäonnistui ja mitätöityError sisältö käytössä AWS hälytyksessä
  private val opiskeluOikeudenLatausEpäonnistui = "Opiskeluoikeuden lataus epaonnistui"
  private val mitätöityError = "[mitatoity]"

  def loadOpiskeluoikeudet(
    opiskeluoikeusQueryRepository: OpiskeluoikeusQueryService,
    suostumuksenPeruutusService: SuostumuksenPeruutusService,
    db: RaportointiDatabase,
    update: Option[RaportointiDatabaseUpdate] = None,
    batchSize: Int = DefaultBatchSize,
    onAfterPage: (Int, Seq[OpiskeluoikeusRow]) => Unit = (_, _) => ()
  ): Observable[LoadResult] = {
    val dueTime = update.map(_.dueTime).map(toTimestamp)
    db.setStatusLoadStarted(statusName, dueTime)
    db.setStatusLoadStarted(mitätöidytStatusName, dueTime)

    update.foreach(u => {
      u.service.alustaKaikkiKäsiteltäviksi()
      db.cloneUpdateableTables(u.previousRaportointiDatabase)
      createIndexesForIncrementalUpdate(db)
      suoritusIds.set(db.getLatestSuoritusId)
    })

    var loopCount = 0

    val result = mapOpiskeluoikeudetSivuittainWithoutAccessCheck(batchSize, update, opiskeluoikeusQueryRepository) { batch =>
      if (batch.nonEmpty) {
        val koskiBatch = batch.collect { case r: KoskiOpiskeluoikeusRow => r }
        val ytrBatch = batch.collect { case r: YtrOpiskeluoikeusRow => r }

        val result = (koskiBatch, ytrBatch) match {
          case (_, Seq()) if update.isDefined =>
            updateBatch(db, suostumuksenPeruutusService, koskiBatch)
          case _ if update.isDefined =>
            throw new InternalError("Inkrementaalista päivitystä ei tueta YTR-opiskeluoikeuksille")
          case (_, Seq()) =>
            loadKoskiBatch(db, suostumuksenPeruutusService, koskiBatch)
          case (Seq(), _) =>
            loadYtrBatch(db, ytrBatch)
          case _ =>
            throw new InternalError("Tuntematon tilanne, samassa batchissä YTR- ja Koski-opiskeluoikeuksia")
        }

        onAfterPage(loopCount, batch)
        loopCount = loopCount + 1
        result
      } else {
        // Last batch processed; finalize
        if (update.isEmpty) createIndexesForIncrementalUpdate(db)
        createIndexes(db)
        db.setStatusLoadCompleted(statusName)
        db.setStatusLoadCompleted(mitätöidytStatusName)
        Seq(LoadCompleted())
      }
    }
    result.doOnEach(progressLogger)
  }

  private def loadKoskiBatch(
    db: RaportointiDatabase,
    suostumuksenPeruutusService: SuostumuksenPeruutusService,
    batch: Seq[KoskiOpiskeluoikeusRow]
  ): Seq[LoadResult] = {
    val (mitätöidytOot, olemassaolevatOot) = batch.partition(_.mitätöity)
    val (poistetutOot, mitätöidytEiPoistetutOot) = mitätöidytOot.partition(_.poistettu)

    val resultOlemassaolevatOot = loadKoskiBatchOlemassaolevatOpiskeluoikeudet(db, olemassaolevatOot)
    val resultMitätöidyt = loadKoskiBatchMitätöidytOpiskeluoikeudet(db, mitätöidytEiPoistetutOot)
    val resultPoistetut = loadKoskiBatchPoistetutOpiskeluoikeudet(db, suostumuksenPeruutusService, poistetutOot)

    resultOlemassaolevatOot ++ resultMitätöidyt ++ resultPoistetut
  }

  private def loadYtrBatch(
    db: RaportointiDatabase,
    batch: Seq[YtrOpiskeluoikeusRow]
  ): Seq[LoadResult] = {
    val (mitätöidytOot, olemassaolevatOot) = batch.partition(_.mitätöity)

    val resultOlemassaolevatOot = loadYtrBatchOlemassaolevatOpiskeluoikeudet(db, olemassaolevatOot)

    // Mitätöityjä ei (toistaiseksi) käsitellä, koska sellaisia ei YTR-datassa voi olla.

    resultOlemassaolevatOot
  }


  private def updateBatch(
    db: RaportointiDatabase,
    suostumuksenPeruutusService: SuostumuksenPeruutusService,
    batch: Seq[KoskiOpiskeluoikeusRow]
  ): Seq[LoadResult] = {
    val (mitätöidytOot, olemassaolevatOot) = batch.partition(_.mitätöity)
    val (poistetutOot, mitätöidytEiPoistetutOot) = mitätöidytOot.partition(_.poistettu)

    val resultOlemassaolevatOot = updateBatchOlemassaolevatOpiskeluoikeudet(db, olemassaolevatOot, mitätöidytOot.map(_.oid))
    val resultMitätöidyt = updateBatchMitätöidytOpiskeluoikeudet(db, mitätöidytEiPoistetutOot, olemassaolevatOot.map(_.oid))
    val resultPoistetut = updateBatchPoistetutOpiskeluoikeudet(db, suostumuksenPeruutusService, poistetutOot)

    resultOlemassaolevatOot ++ resultMitätöidyt ++ resultPoistetut
  }

  private def loadKoskiBatchOlemassaolevatOpiskeluoikeudet(db: RaportointiDatabase, oot: Seq[KoskiOpiskeluoikeusRow]): Seq[LoadResult] = {
    val loadBatchStartTime = System.nanoTime()

    val (errors, outputRows) = oot.par
      .map(row => buildKoskiRow(row))
      .seq
      .partition(_.isLeft)

    db.loadOpiskeluoikeudet(outputRows.map(_.right.get.rOpiskeluoikeusRow))
    db.loadOrganisaatioHistoria(outputRows.flatMap(_.right.get.organisaatioHistoriaRows))
    val aikajaksoRows = outputRows.flatMap(_.right.get.rOpiskeluoikeusAikajaksoRows)
    val esiopetusOpiskeluoikeusAikajaksoRows = outputRows.flatMap(_.right.get.esiopetusOpiskeluoikeusAikajaksoRows)
    val päätasonSuoritusRows = outputRows.flatMap(_.right.get.rPäätasonSuoritusRows)
    val osasuoritusRows = outputRows.flatMap(_.right.get.rOsasuoritusRows)
    val muuAmmatillinenRaportointiRows = outputRows.flatMap(_.right.get.muuAmmatillinenOsasuoritusRaportointiRows)
    val topksAmmatillinenRaportointiRows = outputRows.flatMap(_.right.get.topksAmmatillinenRaportointiRows)
    db.loadOpiskeluoikeusAikajaksot(aikajaksoRows)
    db.loadEsiopetusOpiskeluoikeusAikajaksot(esiopetusOpiskeluoikeusAikajaksoRows)
    db.loadPäätasonSuoritukset(päätasonSuoritusRows)
    db.loadOsasuoritukset(osasuoritusRows)
    db.loadMuuAmmatillinenRaportointi(muuAmmatillinenRaportointiRows)
    db.loadTOPKSAmmatillinenRaportointi(topksAmmatillinenRaportointiRows)
    db.setLastUpdate(statusName)
    db.updateStatusCount(statusName, outputRows.size)
    val result = errors.map(_.left.get) :+ LoadProgressResult(outputRows.size, päätasonSuoritusRows.size + osasuoritusRows.size)

    val loadBatchDuration: Long = (System.nanoTime() - loadBatchStartTime) / 1000000
    val toOpiskeluoikeusUnsafeDuration: Long = outputRows.map(_.right.get.toOpiskeluoikeusUnsafeDuration).sum / 1000000
    logger.info(s"Koski batchin käsittely kesti ${loadBatchDuration} ms, jossa toOpiskeluOikeusUnsafe ${toOpiskeluoikeusUnsafeDuration} ms.")
    result
  }

  private def loadYtrBatchOlemassaolevatOpiskeluoikeudet(db: RaportointiDatabase, oot: Seq[YtrOpiskeluoikeusRow]): Seq[LoadResult] = {
    val loadBatchStartTime = System.nanoTime()

    val (errors, outputRows) = oot.par
      .map(row => buildYtrRow(row))
      .seq
      .partition(_.isLeft)

    db.loadOpiskeluoikeudet(outputRows.map(_.right.get.rOpiskeluoikeusRow))
    val päätasonSuoritusRows = outputRows.flatMap(_.right.get.rPäätasonSuoritusRows)
    val tutkintokokonaisuudenSuoritusRows = outputRows.flatMap(_.right.get.rTutkintokokonaisuudenSuoritusRows)
    val tutkintokerranSuoritusRows = outputRows.flatMap(_.right.get.rTutkintokerranSuoritusRows)
    val kokeenSuoritusRows = outputRows.flatMap(_.right.get.rKokeenSuoritusRows)
    val tutkintokokonaisuudenKokeenSuoritusRows = outputRows.flatMap(_.right.get.rTutkintokokonaisuudenKokeenSuoritusRows)
    db.loadPäätasonSuoritukset(päätasonSuoritusRows)
    db.loadYtrOsasuoritukset(
      tutkintokokonaisuudenSuoritusRows,
      tutkintokerranSuoritusRows,
      kokeenSuoritusRows,
      tutkintokokonaisuudenKokeenSuoritusRows
    )

    db.setLastUpdate(statusName)
    db.updateStatusCount(statusName, outputRows.size)

    val result = errors.map(_.left.get) :+
      LoadProgressResult(outputRows.size,
        päätasonSuoritusRows.size +
          tutkintokokonaisuudenSuoritusRows.size +
          tutkintokerranSuoritusRows.size +
          kokeenSuoritusRows.size
      )

    val loadBatchDuration: Long = (System.nanoTime() - loadBatchStartTime) / 1000000
    val toOpiskeluoikeusUnsafeDuration: Long = outputRows.map(_.right.get.toOpiskeluoikeusUnsafeDuration).sum / 1000000
    logger.info(s"YTR batchin käsittely kesti ${loadBatchDuration} ms, jossa toOpiskeluOikeusUnsafe ${toOpiskeluoikeusUnsafeDuration} ms.")
    result
  }

  private def updateBatchOlemassaolevatOpiskeluoikeudet(
    db: RaportointiDatabase,
    oot: Seq[KoskiOpiskeluoikeusRow],
    mitätöidytOot: Seq[Opiskeluoikeus.Oid],
  ) = {
    val loadBatchStartTime = System.nanoTime()

    val (errors, outputRows) = oot.par
      .map(row => buildKoskiRow(row))
      .seq
      .partition(_.isLeft)

    db.updateOpiskeluoikeudet(outputRows.map(_.right.get.rOpiskeluoikeusRow), mitätöidytOot)
    db.updateOrganisaatioHistoria(outputRows.flatMap(_.right.get.organisaatioHistoriaRows))

    val aikajaksoRows = outputRows.flatMap(_.right.get.rOpiskeluoikeusAikajaksoRows)
    db.updateOpiskeluoikeusAikajaksot(aikajaksoRows)

    val esiopetusOpiskeluoikeusAikajaksoRows = outputRows.flatMap(_.right.get.esiopetusOpiskeluoikeusAikajaksoRows)
    db.updateEsiopetusOpiskeluoikeusAikajaksot(esiopetusOpiskeluoikeusAikajaksoRows)

    val päätasonSuoritusRows = outputRows.flatMap(_.right.get.rPäätasonSuoritusRows)
    db.updatePäätasonSuoritukset(päätasonSuoritusRows)

    val osasuoritusRows = outputRows.flatMap(_.right.get.rOsasuoritusRows)
    db.updateOsasuoritukset(osasuoritusRows)

    val muuAmmatillinenRaportointiRows = outputRows.flatMap(_.right.get.muuAmmatillinenOsasuoritusRaportointiRows)
    db.updateMuuAmmatillinenRaportointi(muuAmmatillinenRaportointiRows)

    val topksAmmatillinenRaportointiRows = outputRows.flatMap(_.right.get.topksAmmatillinenRaportointiRows)
    db.updateTOPKSAmmatillinenRaportointi(topksAmmatillinenRaportointiRows)

    db.setLastUpdate(statusName)
    db.updateStatusCount(statusName, outputRows.size)
    val result = errors.map(_.left.get) :+ LoadProgressResult(outputRows.size, päätasonSuoritusRows.size + osasuoritusRows.size)

    val loadBatchDuration: Long = (System.nanoTime() - loadBatchStartTime) / 1000000
    val toOpiskeluoikeusUnsafeDuration: Long = outputRows.map(_.right.get.toOpiskeluoikeusUnsafeDuration).sum / 1000000
    logger.info(s"Batchin käsittely kesti ${loadBatchDuration} ms, jossa toOpiskeluOikeusUnsafe ${toOpiskeluoikeusUnsafeDuration} ms.")
    result
  }

  private def loadKoskiBatchMitätöidytOpiskeluoikeudet(db: RaportointiDatabase, oot: Seq[KoskiOpiskeluoikeusRow]) = {
    val (errors, outputRows) = oot.par.filterNot(_.poistettu).map(buildRowMitätöity).seq.partition(_.isLeft)
    db.loadMitätöidytOpiskeluoikeudet(outputRows.map(_.right.get))
    db.updateStatusCount(mitätöidytStatusName, outputRows.size)
    errors.map(_.left.get)
  }

  private def updateBatchMitätöidytOpiskeluoikeudet(
    db: RaportointiDatabase,
    oot: Seq[KoskiOpiskeluoikeusRow],
    olemassaolevatOot: Seq[Opiskeluoikeus.Oid],
  ) = {
    val (errors, outputRows) = oot.par.filterNot(_.poistettu).map(buildRowMitätöity).seq.partition(_.isLeft)
    db.updateMitätöidytOpiskeluoikeudet(outputRows.map(_.right.get), olemassaolevatOot)
    db.updateStatusCount(mitätöidytStatusName, outputRows.size)
    errors.map(_.left.get)
  }

  private def loadKoskiBatchPoistetutOpiskeluoikeudet(
    db: RaportointiDatabase,
    suostumuksenPeruutusService: SuostumuksenPeruutusService,
    oot: Seq[KoskiOpiskeluoikeusRow]
  ): Seq[LoadErrorResult] = {
    if (oot.nonEmpty) {
      val (errors, outputRows) = suostumuksenPeruutusService
        .etsiPoistetut(oot.map(_.oid))
        .map(buildRowMitätöity)
        .partition(_.isLeft)
      db.loadMitätöidytOpiskeluoikeudet(outputRows.map(_.right.get))
      db.updateStatusCount(mitätöidytStatusName, outputRows.size)
      errors.map(_.left.get)
    } else {
      Seq.empty
    }
  }

  private def updateBatchPoistetutOpiskeluoikeudet(
    db: RaportointiDatabase,
    suostumuksenPeruutusService: SuostumuksenPeruutusService,
    oot: Seq[KoskiOpiskeluoikeusRow]
  ): Seq[LoadErrorResult] = {
    if (oot.nonEmpty) {
      val (errors, outputRows) = suostumuksenPeruutusService
        .etsiPoistetut(oot.map(_.oid))
        .map(buildRowMitätöity)
        .partition(_.isLeft)
      db.updateMitätöidytOpiskeluoikeudet(outputRows.map(_.right.get), Seq.empty)
      db.updateStatusCount(mitätöidytStatusName, outputRows.size)
      errors.map(_.left.get)
    } else {
      Seq.empty
    }
  }

  private def progressLogger: Subscriber[LoadResult] = new Subscriber[LoadResult] {
    val LoggingInterval = 5.minutes.toMillis
    val startTime = System.currentTimeMillis
    logger.info("Ladataan opiskeluoikeuksia...")

    var opiskeluoikeusCount = 0
    var suoritusCount = 0
    var errors = 0
    var lastLogged = System.currentTimeMillis
    override def onNext(r: LoadResult) = {
      r match {
        case LoadErrorResult(oid, error) =>
          logger.warn(s"$opiskeluOikeudenLatausEpäonnistui: $oid $error")
          errors += 1
        case LoadProgressResult(o, s) => {
          opiskeluoikeusCount += o
          suoritusCount += s
        }
        case LoadCompleted(_) =>
      }
      val now = System.currentTimeMillis
      if ((now - lastLogged) > LoggingInterval) {
        logIt(false)
        lastLogged = now
      }
    }
    override def onError(e: Throwable) {
      logger.error(e)("Opiskeluoikeuksien lataus epäonnistui")
    }
    override def onCompleted() {
      logIt(true)
    }
    private def logIt(done: Boolean) = {
      val elapsedSeconds = (System.currentTimeMillis - startTime) / 1000.0
      val rate = (opiskeluoikeusCount + errors) / Math.max(1.0, elapsedSeconds)
      logger.info(s"${if (done) "Ladattiin" else "Ladattu tähän mennessä"} $opiskeluoikeusCount opiskeluoikeutta, $suoritusCount suoritusta, $errors virhettä, ${(rate*60).round} opiskeluoikeutta/min")
    }
  }

  private def createIndexesForIncrementalUpdate(raportointiDatabase: RaportointiDatabase): Unit = {
    val indexStartTime = System.currentTimeMillis
    logger.info("Luodaan osa indekseistä opiskeluoikeuksille...")
    raportointiDatabase.createIndexesForIncrementalUpdate()
    val indexElapsedSeconds = (System.currentTimeMillis - indexStartTime)/1000
    logger.info(s"Luotiin osa indekseistä opiskeluoikeuksille, ${indexElapsedSeconds} s")
  }

  private def createIndexes(raportointiDatabase: RaportointiDatabase): Unit = {
    val indexStartTime = System.currentTimeMillis
    logger.info("Luodaan indeksit opiskeluoikeuksille...")
    raportointiDatabase.createOpiskeluoikeusIndexes
    val indexElapsedSeconds = (System.currentTimeMillis - indexStartTime)/1000
    logger.info(s"Luotiin indeksit opiskeluoikeuksille, ${indexElapsedSeconds} s")
  }

  private val suoritusIds = new AtomicLong()

  type KoskiSuoritusRows = List[(
    RPäätasonSuoritusRow,
      List[ROsasuoritusRow],
      List[MuuAmmatillinenOsasuoritusRaportointiRow],
      List[TOPKSAmmatillinenRaportointiRow]
    )]

  type AikajaksoRows = (Seq[ROpiskeluoikeusAikajaksoRow], Seq[EsiopetusOpiskeluoikeusAikajaksoRow])

  private def buildKoskiRow(inputRow: OpiskeluoikeusRow): Either[LoadErrorResult, KoskiOutputRows] = {
    Try {
      val toOpiskeluoikeusUnsafeStartTime = System.nanoTime()
      val oo = inputRow.toOpiskeluoikeusUnsafe(KoskiSpecificSession.systemUser)
      val toOpiskeluoikeusUnsafeDuration = System.nanoTime() - toOpiskeluoikeusUnsafeStartTime
      val ooRow = buildROpiskeluoikeusRow(inputRow.oppijaOid, inputRow.aikaleima, oo, inputRow.data)

      val aikajaksoRows: AikajaksoRows = buildAikajaksoRows(inputRow.oid, oo)
      val suoritusRows: KoskiSuoritusRows = oo.suoritukset.zipWithIndex.map {
        case (ps, i) => buildKoskiSuoritusRows(
          inputRow.oid,
          inputRow.sisältäväOpiskeluoikeusOid,
          oo.getOppilaitos,
          ps,
          (inputRow.data \ "suoritukset") (i),
          suoritusIds.incrementAndGet
        )
      }
      KoskiOutputRows(
        rOpiskeluoikeusRow = ooRow,
        organisaatioHistoriaRows = OrganisaatioHistoriaRowBuilder.buildOrganisaatioHistoriaRows(oo),
        rOpiskeluoikeusAikajaksoRows = aikajaksoRows._1,
        esiopetusOpiskeluoikeusAikajaksoRows = aikajaksoRows._2,
        rPäätasonSuoritusRows = suoritusRows.map(_._1),
        rOsasuoritusRows = suoritusRows.flatMap(_._2),
        muuAmmatillinenOsasuoritusRaportointiRows = suoritusRows.flatMap(_._3),
        topksAmmatillinenRaportointiRows = suoritusRows.flatMap(_._4),
        toOpiskeluoikeusUnsafeDuration = toOpiskeluoikeusUnsafeDuration
      )
    }.toEither.left.map(t => LoadErrorResult(inputRow.oid, t.toString))
  }

  type YtrSuoritusRows = List[(
    RPäätasonSuoritusRow,
      List[RYtrTutkintokokonaisuudenSuoritusRow],
      List[RYtrTutkintokerranSuoritusRow],
      List[RYtrKokeenSuoritusRow],
      List[RYtrTutkintokokonaisuudenKokeenSuoritusRow]
    )]

  private def buildYtrRow(inputRow: YtrOpiskeluoikeusRow): Either[LoadErrorResult, YtrOutputRows] = {
    Try {
      val toOpiskeluoikeusUnsafeStartTime = System.nanoTime()
      val oo = inputRow.toOpiskeluoikeusUnsafe(KoskiSpecificSession.systemUser)
      val toOpiskeluoikeusUnsafeDuration = System.nanoTime() - toOpiskeluoikeusUnsafeStartTime
      val ooRow = buildROpiskeluoikeusRow(inputRow.oppijaOid, inputRow.aikaleima, oo, inputRow.data)

      val suoritusRows: YtrSuoritusRows = oo.suoritukset.zipWithIndex.map {
        case (ps, i) => buildYtrSuoritusRows(
          oo,
          inputRow.sisältäväOpiskeluoikeusOid,
          oo.getOppilaitos,
          ps,
          inputRow.data \ "lisätiedot",
          (inputRow.data \ "suoritukset") (i),
          suoritusIds.incrementAndGet
        )
      }
      YtrOutputRows(
        rOpiskeluoikeusRow = ooRow,
        rPäätasonSuoritusRows = suoritusRows.map(_._1),
        rTutkintokokonaisuudenSuoritusRows = suoritusRows.flatMap(_._2),
        rTutkintokerranSuoritusRows = suoritusRows.flatMap(_._3),
        rKokeenSuoritusRows = suoritusRows.flatMap(_._4),
        rTutkintokokonaisuudenKokeenSuoritusRows = suoritusRows.flatMap(_._5),
        toOpiskeluoikeusUnsafeDuration = toOpiskeluoikeusUnsafeDuration
      )
    }.toEither.left.map(t => LoadErrorResult(inputRow.oid, t.toString))
  }

  private val fieldsToExcludeFromOpiskeluoikeusJson = Set("oid", "versionumero", "aikaleima", "oppilaitos", "koulutustoimija", "suoritukset", "tyyppi", "alkamispäivä", "päättymispäivä")

  private def buildROpiskeluoikeusRow(oppijaOid: String, aikaleima: Timestamp, o: KoskeenTallennettavaOpiskeluoikeus, data: JValue) = {
    ROpiskeluoikeusRow(
      opiskeluoikeusOid = o.oid.get,
      versionumero = o.versionumero.get,
      aikaleima = aikaleima,
      sisältyyOpiskeluoikeuteenOid = o.sisältyyOpiskeluoikeuteen.map(_.oid),
      oppijaOid = oppijaOid,
      oppilaitosOid = o.getOppilaitos.oid,
      oppilaitosNimi = convertLocalizedString(o.oppilaitos.flatMap(_.nimi), "fi"),
      oppilaitosNimiSv = convertLocalizedString(o.oppilaitos.flatMap(_.nimi), "sv"),
      oppilaitosKotipaikka = o.oppilaitos.flatMap(_.kotipaikka).map(_.koodiarvo.stripPrefix("kunta_")),
      oppilaitosnumero = o match {
        case _: YlioppilastutkinnonOpiskeluoikeus => None
        case _ => o.oppilaitos.flatMap(_.oppilaitosnumero).map(_.koodiarvo)
      },
      koulutustoimijaOid = o.koulutustoimija.map(_.oid).getOrElse(""),
      koulutustoimijaNimi = convertLocalizedString(o.koulutustoimija.flatMap(_.nimi), "fi"),
      koulutustoimijaNimiSv = convertLocalizedString(o.koulutustoimija.flatMap(_.nimi), "sv"),
      koulutusmuoto = o.tyyppi.koodiarvo,
      alkamispäivä = o match {
        case _: YlioppilastutkinnonOpiskeluoikeus => None
        case _ => o.alkamispäivä.map(Date.valueOf)
      },
      päättymispäivä = o match {
        case _: YlioppilastutkinnonOpiskeluoikeus => None
        case _ => o.tila.opiskeluoikeusjaksot.lastOption.filter(_.opiskeluoikeusPäättynyt).map(v => Date.valueOf(v.alku))
      },
      viimeisinTila = o match {
        case _: YlioppilastutkinnonOpiskeluoikeus => None
        case _ => o.tila.opiskeluoikeusjaksot.lastOption.map(_.tila.koodiarvo)
      },
      lisätiedotHenkilöstökoulutus = o.lisätiedot.collect {
        case l: AmmatillisenOpiskeluoikeudenLisätiedot => l.henkilöstökoulutus
      }.getOrElse(false),
      lisätiedotKoulutusvienti = o.lisätiedot.collect {
        case l: AmmatillisenOpiskeluoikeudenLisätiedot => l.koulutusvienti
      }.getOrElse(false),
      tuvaJärjestämislupa = o match {
        case l: TutkintokoulutukseenValmentavanOpiskeluoikeus => Some(l.järjestämislupa.koodiarvo)
        case _ => None
      },
      lähdejärjestelmäKoodiarvo = o.lähdejärjestelmänId.map(_.lähdejärjestelmä.koodiarvo),
      lähdejärjestelmäId = o.lähdejärjestelmänId.flatMap(_.id),
      luokka = o.luokka,
      oppivelvollisuudenSuorittamiseenKelpaava = oppivelvollisuudenSuorittamiseenKelpaava(o),
      data = JsonManipulation.removeFields(data, fieldsToExcludeFromOpiskeluoikeusJson)
    )
  }

  private def oppivelvollisuudenSuorittamiseenKelpaava(o: KoskeenTallennettavaOpiskeluoikeus): Boolean =
    o.tyyppi.koodiarvo match {
      case "perusopetus" => true
      case "internationalschool" => true
      case "europeanschoolofhelsinki"
        if o.asInstanceOf[EuropeanSchoolOfHelsinkiOpiskeluoikeus].suoritukset.exists {
          case _: OppivelvollisuudenSuorittamiseenKelpaavaESHVuosiluokanSuoritus => true
          case _ => false
        } => true
      case "esiopetus" => true
      case "perusopetukseenvalmistavaopetus" => true
      case _ => MaksuttomuusValidation.oppivelvollisuudenSuorittamiseenKelpaavaMuuKuinPeruskoulunOpiskeluoikeus(o)
    }

  private def buildAikajaksoRows(opiskeluoikeusOid: String, opiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus): AikajaksoRows = {
    val opiskeluoikeusAikajaksot = AikajaksoRowBuilder.buildROpiskeluoikeusAikajaksoRows(opiskeluoikeusOid, opiskeluoikeus)
    val esiopetusOpiskeluoikeusAikajaksot = opiskeluoikeus match {
      case esiopetus: EsiopetuksenOpiskeluoikeus => AikajaksoRowBuilder.buildEsiopetusOpiskeluoikeusAikajaksoRows(opiskeluoikeusOid, esiopetus)
      case _ => Nil
    }

    (opiskeluoikeusAikajaksot, esiopetusOpiskeluoikeusAikajaksot)
  }

  private val fieldsToExcludeFromPäätasonSuoritusJson = Set("osasuoritukset", "tyyppi", "toimipiste", "koulutustyyppi")
  private val fieldsToExcludeFromOsasuoritusJson = Set("osasuoritukset", "tyyppi")
  private val fieldsToExcludeFromYtrTutkintokokonaisuudenSuoritusJson = Set("tutkintokerrat", "tunniste")
  private val fieldsToExcludeFromYtrTutkintokerranSuoritusJson: Set[String] = Set()
  private val fieldsToExcludeFromYtrKokeenSuoritusJson = Set("osasuoritukset", "tyyppi", "tutkintokokonaisuudenTunniste")

  private[raportointikanta] def buildKoskiSuoritusRows(opiskeluoikeusOid: String, sisältyyOpiskeluoikeuteenOid: Option[String], oppilaitos: OrganisaatioWithOid, ps: PäätasonSuoritus, data: JValue, idGenerator: => Long) =
  {
    val päätasonSuoritusId: Long = idGenerator
    val päätaso = buildRPäätasonSuoritusRow(opiskeluoikeusOid, sisältyyOpiskeluoikeuteenOid, oppilaitos, ps, data, päätasonSuoritusId)
    val osat = ps.osasuoritukset.getOrElse(List.empty).zipWithIndex.flatMap {
      case (os, i) => buildROsasuoritusRow(päätasonSuoritusId, None, opiskeluoikeusOid, sisältyyOpiskeluoikeuteenOid, os, (data \ "osasuoritukset")(i), idGenerator)
    }
    val muuAmmatillinenRaportointi = ps match {
      case s: MuunAmmatillisenKoulutuksenSuoritus => s.rekursiivisetOsasuoritukset.map(MuuAmmatillinenRaporttiRowBuilder.build(opiskeluoikeusOid, päätasonSuoritusId, _))
      case _ => Nil
    }
    val topksAmmatillinenRaportointi = ps match {
      case s: TutkinnonOsaaPienemmistäKokonaisuuksistaKoostuvaSuoritus => s.rekursiivisetOsasuoritukset.map(TOPKSAmmatillinenRaporttiRowBuilder.build(opiskeluoikeusOid, päätasonSuoritusId, _))
      case _ => Nil
    }
    (päätaso, osat, muuAmmatillinenRaportointi, topksAmmatillinenRaportointi)
  }


  private[raportointikanta] def buildYtrSuoritusRows(
    oo: YlioppilastutkinnonOpiskeluoikeus,
    sisältyyOpiskeluoikeuteenOid: Option[String],
    oppilaitos: OrganisaatioWithOid,
    ps: YlioppilastutkinnonSuoritus,
    lisätiedotData: JValue,
    psData: JValue,
    idGenerator: => Long
  ) =
  {
    val opiskeluoikeusOid = oo.oid.get

    val päätasonSuoritusId: Long = idGenerator
    val päätaso = buildRPäätasonSuoritusRow(opiskeluoikeusOid, sisältyyOpiskeluoikeuteenOid, oppilaitos, ps, psData, päätasonSuoritusId)

    val tutkintokokonaisuudet: Map[Int, (RYtrTutkintokokonaisuudenSuoritusRow, Map[String, RYtrTutkintokerranSuoritusRow])] =
      oo.lisätiedot.flatMap(_.tutkintokokonaisuudet).getOrElse(List.empty).zipWithIndex.map {
        case (tutkintokokonaisuus, i) =>
          val tutkintokokonaisuusId = idGenerator
          val tutkintokokonaisuusRow =
            buildRYtrTutkintokokonaisuudenSuoritusRow(
              tutkintokokonaisuusId,
              päätasonSuoritusId,
              opiskeluoikeusOid,
              tutkintokokonaisuus,
              (lisätiedotData \ "tutkintokokonaisuudet")(i)
            )

          val tutkintokerrat = tutkintokokonaisuus.tutkintokerrat.zipWithIndex.map {
            case (tutkintokerta, j) =>
              val tutkintokertaId = idGenerator
              val tutkintokertaRow =
                buildRYtrTutkintokerranSuoritusRow(
                  tutkintokertaId,
                  tutkintokokonaisuusId,
                  päätasonSuoritusId,
                  opiskeluoikeusOid,
                  tutkintokerta,
                  ((lisätiedotData \ "tutkintokokonaisuudet")(i) \ "tutkintokerrat")(j)
                )
              tutkintokerta.tutkintokerta.koodiarvo -> tutkintokertaRow
          }.toMap

          tutkintokokonaisuus.tunniste -> (tutkintokokonaisuusRow, tutkintokerrat)
      }.toMap

    val kokeet: Seq[(RYtrKokeenSuoritusRow, Seq[RYtrTutkintokokonaisuudenKokeenSuoritusRow])] =
      ps.osasuoritukset.getOrElse(List.empty).zipWithIndex.map {
        case (koe, i) =>
          val koeId = idGenerator
          val tutkintokertaId = tutkintokokonaisuudet(koe.tutkintokokonaisuudenTunniste.get)._2(koe.tutkintokerta.koodiarvo).ytrTutkintokerranSuoritusId
          val tutkintokokonaisuusId = tutkintokokonaisuudet(koe.tutkintokokonaisuudenTunniste.get)._1.ytrTutkintokokonaisuudenSuoritusId
          val koeRow = buildRYtrKokeenSuoritusRow(
            koeId,
            tutkintokertaId,
            tutkintokokonaisuusId,
            päätasonSuoritusId,
            opiskeluoikeusOid,
            koe,
            (psData \ "osasuoritukset")(i)
          )
          val tutkintokokonaisuudenKoeRow =
            buildRYtrTutkintokokonaisuudenKokeenSuoritusRow(
              tutkintokokonaisuusId,
              koeId,
              tutkintokertaId,
              sisällytetty = false
            )
          (koeRow, Seq(tutkintokokonaisuudenKoeRow))
      }

    (
      päätaso,
      tutkintokokonaisuudet.values.map(_._1).toList,
      tutkintokokonaisuudet.values.map(_._2).flatMap(_.values).toList,
      kokeet.map(_._1).toList,
      kokeet.flatMap(_._2).toList
    )
  }

  private def buildRPäätasonSuoritusRow(opiskeluoikeusOid: String, sisältyyOpiskeluoikeuteenOid: Option[String], oppilaitos: OrganisaatioWithOid, ps: PäätasonSuoritus, data: JValue, päätasonSuoritusId: Long) = {
    val toimipiste = ps match {
      case tp: Toimipisteellinen => tp.toimipiste
      case stp: MahdollisestiToimipisteellinen if stp.toimipiste.nonEmpty => stp.toimipiste.get
      case _ => oppilaitos
    }
    val päätaso = RPäätasonSuoritusRow(
      päätasonSuoritusId = päätasonSuoritusId,
      opiskeluoikeusOid = opiskeluoikeusOid,
      suorituksenTyyppi = ps.tyyppi.koodiarvo,
      koulutusmoduuliKoodisto = convertKoodisto(ps.koulutusmoduuli.tunniste),
      koulutusmoduuliKoodiarvo = ps.koulutusmoduuli.tunniste.koodiarvo,
      koulutusmoduuliKoulutustyyppi = ps.koulutusmoduuli match {
        case k: KoulutustyypinSisältäväKoulutusmoduuli => k.koulutustyyppi.map(_.koodiarvo)
        case _ => None
      },
      koulutusmoduuliLaajuusArvo = ps.koulutusmoduuli.getLaajuus.map(_.arvo),
      koulutusmoduuliLaajuusYksikkö = ps.koulutusmoduuli.getLaajuus.map(_.yksikkö.koodiarvo),
      koulutusmoduuliNimi = ps.koulutusmoduuli.tunniste.getNimi.map(_.get("fi")),
      tutkinnonNimiPerusteessa = ps.koulutusmoduuli match {
        case k: PerusteenNimellinen => k.perusteenNimi.map(_.get("fi"))
        case _ => None
      },
      suorituskieliKoodiarvo = ps match {
        case s: Suorituskielellinen => Some(s.suorituskieli.koodiarvo)
        case m: MahdollisestiSuorituskielellinen => m.suorituskieli.map(_.koodiarvo)
        case _ => None
      },
      oppimääräKoodiarvo = ps match {
        case o: Oppimäärällinen => Some(o.oppimäärä.koodiarvo)
        //case l: LukionPäätasonSuoritus2015 => l.oppimääränKoodiarvo
        case _ => None
      },
      alkamispäivä = ps.alkamispäivä.map(v => Date.valueOf(v)),
      vahvistusPäivä = ps.vahvistus.map(v => Date.valueOf(v.päivä)),
      arviointiArvosanaKoodiarvo = ps.viimeisinArviointi.map(_.arvosana.koodiarvo),
      arviointiArvosanaKoodisto = ps.viimeisinArviointi.flatMap(a => convertKoodisto(a.arvosana)),
      arviointiHyväksytty = ps.viimeisinArviointi.map(_.hyväksytty),
      arviointiPäivä = ps.viimeisinArviointi.flatMap(_.arviointipäivä).map(v => Date.valueOf(v)),
      toimipisteOid = toimipiste.oid,
      toimipisteNimi = convertLocalizedString(toimipiste.nimi, "fi"),
      toimipisteNimiSv = convertLocalizedString(toimipiste.nimi, "sv"),
      data = JsonManipulation.removeFields(data, fieldsToExcludeFromPäätasonSuoritusJson),
      sisältyyOpiskeluoikeuteenOid = sisältyyOpiskeluoikeuteenOid
    )
    päätaso
  }

  private def buildROsasuoritusRow(
    päätasonSuoritusId: Long,
    ylempiOsasuoritusId: Option[Long],
    opiskeluoikeusOid: String,
    sisältyyOpiskeluoikeuteenOid: Option[String],
    os: Suoritus,
    data: JValue,
    idGenerator: => Long
  ): Seq[ROsasuoritusRow] = {
    val osasuoritusId: Long = idGenerator
    ROsasuoritusRow(
      osasuoritusId = osasuoritusId,
      ylempiOsasuoritusId = ylempiOsasuoritusId,
      päätasonSuoritusId = päätasonSuoritusId,
      opiskeluoikeusOid = opiskeluoikeusOid,
      suorituksenTyyppi = os.tyyppi.koodiarvo,
      koulutusmoduuliKoodisto = convertKoodisto(os.koulutusmoduuli.tunniste),
      koulutusmoduuliKoodiarvo = os.koulutusmoduuli.tunniste.koodiarvo,
      koulutusmoduuliLaajuusArvo = os.koulutusmoduuli.getLaajuus.map(_.arvo),
      koulutusmoduuliLaajuusYksikkö = os.koulutusmoduuli.getLaajuus.map(_.yksikkö.koodiarvo),
      koulutusmoduuliPaikallinen = os.koulutusmoduuli.tunniste match {
        case k: Koodistokoodiviite => false
        case k: PaikallinenKoodi => true
      },
      koulutusmoduuliPakollinen = os.koulutusmoduuli match {
        case v: Valinnaisuus => Some(v.pakollinen)
        case _ => None
      },
      koulutusmoduuliNimi = os.koulutusmoduuli.tunniste.getNimi.map(_.get("fi")),
      koulutusmoduuliOppimääräNimi = os.koulutusmoduuli match {
        case k: Oppimäärä => k.oppimäärä.nimi.map(_.get("fi"))
        case k: Uskonto => k.uskonnonOppimäärä.flatMap(_.nimi.map(_.get("fi")))
        case _ => None
      },
      koulutusmoduuliKieliaineNimi = os.koulutusmoduuli match {
        case k: Kieliaine => k.kieli.nimi.map(_.get("fi"))
        case _ => None
      },
      koulutusmoduuliKurssinTyyppi = os.koulutusmoduuli match {
        case l: LukionKurssi2015 => Some(l.kurssinTyyppi.koodiarvo)
        case l: LukionModuuli2019 => l.pakollinen match {
          case true => Some("pakollinen")
          case false => None
        }
        case _ => None
      },
      vahvistusPäivä = os.vahvistus.map(v => Date.valueOf(v.päivä)),
      arviointiArvosanaKoodiarvo = os.parasArviointi.map(_.arvosana.koodiarvo),
      arviointiArvosanaKoodisto = os.parasArviointi.flatMap(a => convertKoodisto(a.arvosana)),
      arviointiHyväksytty = os.parasArviointi.map(_.hyväksytty),
      arviointiPäivä = os.parasArviointi.flatMap(_.arviointipäivä).map(v => Date.valueOf(v)),
      ensimmäinenArviointiPäivä = os.arviointi.toList.flatten.map(_.arviointipäivä).flatten.map(v => Date.valueOf(v)).reduceOption((a, b) => {
        if (a.toLocalDate.isBefore(b.toLocalDate)) {
          a
        } else {
          b
        }
      }),
      korotettuEriVuonna = (os.ensimmäinenArviointiPäivä, os.parasArviointiPäivä) match {
        case (Some(eka), Some(paras)) => {
          if (eka.get(ChronoField.YEAR) != paras.get(ChronoField.YEAR)) {
            true
          } else {
            false
          }
        }
        case _ => false
      },
      näytönArviointiPäivä = os match {
        case atos: AmmatillisenTutkinnonOsanSuoritus => atos.näyttö.flatMap(_.arviointi).map(v => Date.valueOf(v.päivä))
        case vkos: ValmaKoulutuksenOsanSuoritus => vkos.näyttö.flatMap(_.arviointi).map(v => Date.valueOf(v.päivä))
        case tkos: TelmaKoulutuksenOsanSuoritus => tkos.näyttö.flatMap(_.arviointi).map(v => Date.valueOf(v.päivä))
        case _ => None
      },
      tunnustettu = os match {
        case m: MahdollisestiTunnustettu => m.tunnustettu.isDefined
        case _ => false
      },
      tunnustettuRahoituksenPiirissä = os match {
        case m: MahdollisestiTunnustettu => m.tunnustettuRahoituksenPiirissä
        case _ => false
      },
      data = JsonManipulation.removeFields(data, fieldsToExcludeFromOsasuoritusJson),
      sisältyyOpiskeluoikeuteenOid = sisältyyOpiskeluoikeuteenOid
    ) +: os.osasuoritukset.getOrElse(List.empty).zipWithIndex.flatMap {
      case (os2, i) => buildROsasuoritusRow(
        päätasonSuoritusId,
        Some(osasuoritusId),
        opiskeluoikeusOid,
        sisältyyOpiskeluoikeuteenOid,
        os2,
        (data \ "osasuoritukset")(i),
        idGenerator
      )
    }
  }

  private def buildRYtrTutkintokokonaisuudenSuoritusRow(
    id: Long,
    päätasonSuoritusId: Long,
    opiskeluoikeusOid: String,
    tk: YlioppilastutkinnonTutkintokokonaisuudenLisätiedot,
    data: JValue
  ): RYtrTutkintokokonaisuudenSuoritusRow = {
    val tyyppiKoodiarvo = tk.tyyppi.map(_.koodiarvo)
    val tilaKoodiarvo = tk.tila.map(_.koodiarvo)
    val hyväksytystiValmistunutTutkinto =
      tyyppiKoodiarvo.exists(_ == "candidate") &&
        tilaKoodiarvo.exists(_ == "graduated")
    RYtrTutkintokokonaisuudenSuoritusRow(
      ytrTutkintokokonaisuudenSuoritusId = id,
      päätasonSuoritusId = päätasonSuoritusId,
      opiskeluoikeusOid = opiskeluoikeusOid,
      tyyppiKoodiarvo = tyyppiKoodiarvo,
      tilaKoodiarvo = tilaKoodiarvo,
      suorituskieliKoodiarvo = tk.suorituskieli.map(_.koodiarvo),
      hyväksytystiValmistunutTutkinto = Some(hyväksytystiValmistunutTutkinto),
      data = JsonManipulation.removeFields(data, fieldsToExcludeFromYtrTutkintokokonaisuudenSuoritusJson)
    )
  }

  private def buildRYtrTutkintokerranSuoritusRow(
    id: Long,
    tutkintokokonaisuusId: Long,
    päätasonSuoritusId: Long,
    opiskeluoikeusOid: String,
    tk:  YlioppilastutkinnonTutkintokerranLisätiedot,
    data: JValue
  ): RYtrTutkintokerranSuoritusRow = {
    RYtrTutkintokerranSuoritusRow(
      ytrTutkintokerranSuoritusId = id,
      ytrTutkintokokonaisuudenSuoritusId = tutkintokokonaisuusId,
      päätasonSuoritusId = päätasonSuoritusId,
      opiskeluoikeusOid = opiskeluoikeusOid,
      tutkintokertaKoodiarvo = tk.tutkintokerta.koodiarvo,
      vuosi = tk.tutkintokerta.vuosi,
      vuodenaikaKoodiarvo = tk.tutkintokerta.koodiarvo.takeRight(1),
      koulutustaustaKoodiarvo = tk.koulutustausta.map(_.koodiarvo),

      oppilaitosOid = tk.oppilaitos.map(_.oid),
      oppilaitosNimi = tk.oppilaitos.map(ol => convertLocalizedString(ol.nimi, "fi")),
      oppilaitosNimiSv = tk.oppilaitos.map(ol => convertLocalizedString(ol.nimi, "sv")),
      oppilaitosKotipaikka = tk.oppilaitos.flatMap(_.kotipaikka).map(_.koodiarvo.stripPrefix("kunta_")),
      oppilaitosnumero = tk.oppilaitos.flatMap(_.oppilaitosnumero).map(_.koodiarvo),
      data = JsonManipulation.removeFields(data, fieldsToExcludeFromYtrTutkintokerranSuoritusJson)
    )
  }

  private def buildRYtrKokeenSuoritusRow(
    id: Long,
    tutkintokertaId: Long,
    tutkintokokonaisuusId: Long,
    päätasonSuoritusId: Long,
    opiskeluoikeusOid: String,
    ks: YlioppilastutkinnonKokeenSuoritus,
    data: JValue
  ) = {
    RYtrKokeenSuoritusRow(
      ytrKokeenSuoritusId = id,
      ytrTutkintokerranSuoritusId = tutkintokertaId,
      ytrTutkintokokonaisuudenSuoritusId = tutkintokokonaisuusId,
      päätasonSuoritusId = päätasonSuoritusId,
      opiskeluoikeusOid = opiskeluoikeusOid,
      suorituksenTyyppi = ks.tyyppi.koodiarvo,
      koulutusmoduuliKoodisto = convertKoodisto(ks.koulutusmoduuli.tunniste),
      koulutusmoduuliKoodiarvo = ks.koulutusmoduuli.tunniste.koodiarvo,
      koulutusmoduuliNimi = ks.koulutusmoduuli.tunniste.getNimi.map(_.get("fi")),
      arviointiArvosanaKoodiarvo = ks.viimeisinArviointi.map(_.arvosana.koodiarvo),
      arviointiArvosanaKoodisto = ks.viimeisinArviointi.flatMap(a => convertKoodisto(a.arvosana)),
      arviointiHyväksytty = ks.viimeisinArviointi.map(_.hyväksytty),
      arviointiPisteet = ks.viimeisinArviointi.flatMap(_.pisteet),
      keskeytynyt = ks.keskeytynyt,
      maksuton = ks.maksuton,
      data = JsonManipulation.removeFields(data, fieldsToExcludeFromYtrKokeenSuoritusJson),
    )
  }

  private def buildRYtrTutkintokokonaisuudenKokeenSuoritusRow(
    tutkintokokonaisuusId: Long,
    koeId: Long,
    tutkintokertaId: Long,
    sisällytetty: Boolean
  ) =
    RYtrTutkintokokonaisuudenKokeenSuoritusRow(
      ytrTutkintokokonaisuudenSuoritusId = tutkintokokonaisuusId,
      ytrKokeenSuoritusId = koeId,
      ytrTutkintokerranSuoritusId = tutkintokertaId,
      sisällytetty = sisällytetty
    )

  private[raportointikanta] def buildRowMitätöity(raw: KoskiOpiskeluoikeusRow): Either[LoadErrorResult, RMitätöityOpiskeluoikeusRow] = {
    for {
      oo <- raw.toOpiskeluoikeus(KoskiSpecificSession.systemUser).left.map(e => LoadErrorResult(raw.oid, mitätöityError + " " + e.toString()))
      mitätöityPvm <- oo.mitätöintiPäivä.toRight(LoadErrorResult(raw.oid, "Mitätöintipäivämäärän haku epäonnistui"))
    } yield RMitätöityOpiskeluoikeusRow(
      opiskeluoikeusOid = raw.oid,
      versionumero = raw.versionumero,
      aikaleima = raw.aikaleima,
      oppijaOid = raw.oppijaOid,
      mitätöity = Some(mitätöityPvm),
      suostumusPeruttu = None,
      tyyppi = raw.koulutusmuoto,
      päätasonSuoritusTyypit = oo.suoritukset.map(_.tyyppi.koodiarvo).distinct
    )
  }

  private[raportointikanta] def buildRowMitätöity(row: PoistettuOpiskeluoikeusRow): Either[LoadErrorResult, RMitätöityOpiskeluoikeusRow] = {
    val aikaleima = row
      .mitätöityAikaleima
      .orElse(row.suostumusPeruttuAikaleima)
      .toRight(LoadErrorResult(
        row.oid,
        "Poistetun opiskeluoikeuden aikaleimaa ei ollut olemassa"
      ))

    aikaleima.map(al =>
      RMitätöityOpiskeluoikeusRow(
        opiskeluoikeusOid = row.oid,
        versionumero = row.versio.getOrElse(0),
        aikaleima = al,
        oppijaOid = row.oppijaOid,
        mitätöity = row.mitätöityAikaleima.map(_.toLocalDateTime.toLocalDate),
        suostumusPeruttu = row.suostumusPeruttuAikaleima.map(_.toLocalDateTime.toLocalDate),
        tyyppi = row.koulutusmuoto,
        päätasonSuoritusTyypit = row.suoritustyypit
      )
    )
  }

  def mapOpiskeluoikeudetSivuittainWithoutAccessCheck[A]
    (
      pageSize: Int,
      update: Option[RaportointiDatabaseUpdate],
      opiskeluoikeusQueryRepository: OpiskeluoikeusQueryService,
    )
    (mapFn: Seq[OpiskeluoikeusRow] => Seq[A])
  : Observable[A] =
    update match {
      case Some(update) =>
        update.loader.load(pageSize, update)(mapFn)
      case _ =>
        opiskeluoikeusQueryRepository.mapKoskiJaYtrOpiskeluoikeudetSivuittainWithoutAccessCheck(pageSize)(mapFn)
    }
}

sealed trait LoadResult
case class LoadErrorResult(oid: String, error: String) extends LoadResult
case class LoadProgressResult(opiskeluoikeusCount: Int, suoritusCount: Int) extends LoadResult
case class LoadCompleted(done: Boolean = true) extends LoadResult

case class KoskiOutputRows(
  rOpiskeluoikeusRow: ROpiskeluoikeusRow,
  organisaatioHistoriaRows: Seq[ROrganisaatioHistoriaRow],
  rOpiskeluoikeusAikajaksoRows: Seq[ROpiskeluoikeusAikajaksoRow],
  esiopetusOpiskeluoikeusAikajaksoRows: Seq[EsiopetusOpiskeluoikeusAikajaksoRow],
  rPäätasonSuoritusRows: Seq[RPäätasonSuoritusRow],
  rOsasuoritusRows: Seq[ROsasuoritusRow],
  muuAmmatillinenOsasuoritusRaportointiRows: Seq[MuuAmmatillinenOsasuoritusRaportointiRow],
  topksAmmatillinenRaportointiRows: Seq[TOPKSAmmatillinenRaportointiRow],
  toOpiskeluoikeusUnsafeDuration: Long = 0
)

case class YtrOutputRows(
  rOpiskeluoikeusRow: ROpiskeluoikeusRow,
  rPäätasonSuoritusRows: Seq[RPäätasonSuoritusRow],
  rTutkintokokonaisuudenSuoritusRows: Seq[RYtrTutkintokokonaisuudenSuoritusRow],
  rTutkintokerranSuoritusRows: Seq[RYtrTutkintokerranSuoritusRow],
  rKokeenSuoritusRows: Seq[RYtrKokeenSuoritusRow],
  rTutkintokokonaisuudenKokeenSuoritusRows: Seq[RYtrTutkintokokonaisuudenKokeenSuoritusRow],
  toOpiskeluoikeusUnsafeDuration: Long = 0
)

case class RaportointiDatabaseUpdate(
  previousRaportointiDatabase: RaportointiDatabase,
  readReplicaDb: DB,
  dueTime: ZonedDateTime,
  sleepDuration: FiniteDuration,
  service: PäivitetytOpiskeluoikeudetJonoService,
) {
  def dueTimeExpired: Boolean = {
    dueTime.toLocalDateTime.isBefore(LocalDateTime.now())
  }

  val loader: PäivitettyOpiskeluoikeusLoader =
    new PäivitettyOpiskeluoikeusLoader(readReplicaDb, service)
}
