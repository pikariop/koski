package fi.oph.koski.raportit

import java.time.LocalDate
import java.time.temporal.ChronoUnit

import fi.oph.koski.eperusteet.EPerusteetRepository
import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.raportointikanta._
import fi.oph.koski.schema._
import fi.oph.koski.util.Futures

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

case class LukioRaportti(repository: LukioRaportitRepository, ePerusteet: EPerusteetRepository) {

  private lazy val lukionoppiaineenoppimaara = "lukionoppiaineenoppimaara"
  private lazy val lukionoppiaine = "lukionoppiaine"
  private lazy val lukionmuuopinto = "lukionmuuopinto"

  def buildRaportti(repository: LukioRaportitRepository, oppilaitosOid: Organisaatio.Oid, alku: LocalDate, loppu: LocalDate): Seq[DynamicDataSheet] = {
    implicit val executionContext = ExecutionContext.fromExecutor(new java.util.concurrent.ForkJoinPool(10))

    val rows = repository.suoritustiedot(oppilaitosOid, alku, loppu)
    val oppiaineetJaKurssit = lukiossaOpetettavatOppiaineetJaNiidenKurssit(rows)

    val future = for {
      oppiaineJaLisätiedot <- oppiaineJaLisätiedotSheet(rows, oppiaineetJaKurssit, alku, loppu, ePerusteet)
      kurssit <- oppiaineKohtaisetSheetit(rows, oppiaineetJaKurssit)
    } yield (oppiaineJaLisätiedot +: kurssit)

    Futures.await(future, atMost = 6.minutes)
  }

  private def lukiossaOpetettavatOppiaineetJaNiidenKurssit(rows: Seq[LukioRaporttiRows]) = {
    rows.flatMap(oppianeetJaNiidenKurssit).groupBy(_.oppiaine).map { case (oppiaine, x) =>
      LukioRaporttiOppiaineJaKurssit(oppiaine, x.flatMap(_.kurssit).distinct)
    }.toSeq.sorted(LukioRaporttiOppiaineetOrdering)
  }

  private def oppianeetJaNiidenKurssit(row: LukioRaporttiRows): Seq[LukioRaporttiOppiaineJaKurssit] = {
    if (row.päätasonSuoritus.suorituksenTyyppi == lukionoppiaineenoppimaara) {
     Seq(LukioRaporttiOppiaineJaKurssit(toOppiaine(row.päätasonSuoritus), row.osasuoritukset.map(toKurssi)))
    } else {
      oppiaineetJaNiidenKurssitOppimäärästä(row)
    }
  }

  private def oppiaineetJaNiidenKurssitOppimäärästä(row: LukioRaporttiRows): Seq[LukioRaporttiOppiaineJaKurssit] = {
    val kurssit = row.osasuoritukset.filter(_.ylempiOsasuoritusId.isDefined).groupBy(_.ylempiOsasuoritusId.get)
    val oppiaineet = row.osasuoritukset.filter(isLukionOppiaine)
    val combineOppiaineWithKurssit = (oppiaine: ROsasuoritusRow) => LukioRaporttiOppiaineJaKurssit(toOppiaine(oppiaine), kurssit.getOrElse(oppiaine.osasuoritusId, Nil).map(toKurssi))

    oppiaineet.map(combineOppiaineWithKurssit)
  }

  private def isLukionOppiaine(osasuoritus: ROsasuoritusRow) = osasuoritus.suorituksenTyyppi == lukionoppiaine || osasuoritus.suorituksenTyyppi == lukionmuuopinto

  private def toOppiaine(row: RSuoritusRow) = row match {
    case s: RPäätasonSuoritusRow => LukioRaporttiOppiaine(s.suorituksestaKäytettäväNimi.getOrElse("ei nimeä"), s.koulutusmoduuliKoodiarvo, !s.koulutusmoduuliKoodisto.contains("koskioppiaineetyleissivistava"))
    case s: ROsasuoritusRow => LukioRaporttiOppiaine(s.suorituksestaKäytettäväNimi.getOrElse("ei nimeä"), s.koulutusmoduuliKoodiarvo, s.koulutusmoduuliPaikallinen)
  }
  private def toKurssi(row: ROsasuoritusRow) = LukioRaporttiKurssi(row.koulutusmoduuliNimi.getOrElse("ei nimeä"), row.koulutusmoduuliKoodiarvo, row.koulutusmoduuliPaikallinen)

  private def oppiaineJaLisätiedotSheet(opiskeluoikeusData: Seq[LukioRaporttiRows], oppiaineetJaKurssit: Seq[LukioRaporttiOppiaineJaKurssit], alku: LocalDate, loppu: LocalDate, ePerusteet: EPerusteetRepository)(implicit executionContext: ExecutionContextExecutor) = {
    Future {
      DynamicDataSheet(
        title = "Oppiaineet ja lisätiedot",
        rows = opiskeluoikeusData.map(kaikkiOppiaineetVälilehtiRow(_, oppiaineetJaKurssit, alku, loppu)).map(_.toSeq),
        columnSettings = oppiaineJaLisätiedotColumnSettings(oppiaineetJaKurssit)
      )
    }(executionContext)
  }

  private def oppiaineKohtaisetSheetit(rows: Seq[LukioRaporttiRows], oppiaineetJaKurssit: Seq[LukioRaporttiOppiaineJaKurssit])(implicit executionContext: ExecutionContextExecutor) = {
    Future {
      oppiaineetJaKurssit.map(oppiaineKohtainenSheet(_, rows))
    }(executionContext)
  }

  private def oppiaineKohtainenSheet(oppiaineJaKurssit: LukioRaporttiOppiaineJaKurssit, data: Seq[LukioRaporttiRows]) = {
    val oppiaine = oppiaineJaKurssit.oppiaine
    val kurssit = oppiaineJaKurssit.kurssit.sortBy(_.koulutusmoduuliKoodiarvo)
    val filtered = data.filter(notOppimääränOpiskelijaFromAnotherOppiaine(oppiaine))

    DynamicDataSheet(
      title = oppiaine.toSheetTitle,
      rows = filtered.map(oppiainekohtaisetKurssitiedot(_, kurssit)).map(_.toSeq),
      columnSettings = oppiaineKohtaisetColumnSettings(kurssit)
    )
  }

  private def notOppimääränOpiskelijaFromAnotherOppiaine(oppiaine: LukioRaporttiOppiaine)(data: LukioRaporttiRows) = {
    data.päätasonSuoritus.suorituksenTyyppi != lukionoppiaineenoppimaara || data.päätasonSuoritus.matchesWith(oppiaine)
  }

  private def kaikkiOppiaineetVälilehtiRow(row: LukioRaporttiRows, oppiaineet: Seq[LukioRaporttiOppiaineJaKurssit], alku: LocalDate, loppu: LocalDate) = {
    val lähdejärjestelmänId = JsonSerializer.extract[Option[LähdejärjestelmäId]](row.opiskeluoikeus.data \ "lähdejärjestelmänId")
    val lisätiedot = JsonSerializer.extract[Option[LukionOpiskeluoikeudenLisätiedot]](row.opiskeluoikeus.data \ "lisätiedot")

    LukioRaporttiKaikkiOppiaineetVälilehtiRow(
     muut = LukioRaporttiOppiaineetVälilehtiMuut(
       opiskeluoikeudenOid = row.opiskeluoikeus.opiskeluoikeusOid,
       lähdejärjestelmä = lähdejärjestelmänId.map(_.lähdejärjestelmä.koodiarvo),
       koulutustoimija = row.opiskeluoikeus.koulutustoimijaNimi,
       oppilaitoksenNimi = row.opiskeluoikeus.oppilaitosNimi,
       toimipiste = row.päätasonSuoritus.toimipisteNimi,
       opiskeluoikeuden_tunniste_lähdejärjestelmässä = lähdejärjestelmänId.flatMap(_.id),
       oppijanOid = row.opiskeluoikeus.oppijaOid,
       hetu = row.henkilo.flatMap(_.hetu),
       sukunimi = row.henkilo.map(_.sukunimi),
       etunimet = row.henkilo.map(_.etunimet),
       opiskeluoikeuden_alkamispäivä = row.opiskeluoikeus.alkamispäivä.map(_.toLocalDate),
       opiskeluoikeuden_viimeisin_tila = row.opiskeluoikeus.viimeisinTila,
       opiskeluoikeuden_tilat_aikajakson_aikana = removeContinuousSameTila(row.aikajaksot).map(_.tila).mkString(","),
       opetussuunnitelma = opetussuunnitelma(row.päätasonSuoritus),
       suorituksenTyyppi = row.päätasonSuoritus.suorituksenTyyppi,
       suorituksenTila = row.päätasonSuoritus.vahvistusPäivä.fold("kesken")(_ => "valmis"),
       suorituksenVahvistuspäivä = row.päätasonSuoritus.vahvistusPäivä.map(_.toLocalDate),
       läsnäolopäiviä_aikajakson_aikana = row.aikajaksot.filter(_.tila == "lasna").map(j => Aikajakso(j.alku.toLocalDate, Some(j.loppu.toLocalDate))).map(lengthInDaysInDateRange(_, alku, loppu)).sum,
       rahoitukset = row.aikajaksot.flatMap(_.opintojenRahoitus).mkString(","),
       ryhmä = JsonSerializer.extract[Option[String]](row.päätasonSuoritus.data \ "ryhmä"),
       pidennettyPäättymispäivä = lisätiedot.exists(_.pidennettyPäättymispäivä),
       ulkomainenVaihtoOpiskelija = lisätiedot.exists(_.ulkomainenVaihtoopiskelija),
       yksityisopiskelija = lisätiedot.exists(_.yksityisopiskelija),
       ulkomaanjaksot = lisätiedot.flatMap(_.ulkomaanjaksot.map(_.map(lengthInDaysInDateRange(_, alku, loppu)).sum)),
       erityisen_koulutustehtävän_tehtävät = lisätiedot.flatMap(_.erityisenKoulutustehtävänJaksot.map(_.flatMap(_.tehtävä.nimi.map(_.get("fi"))).mkString(","))),
       erityisen_koulutustehtävän_jaksot = lisätiedot.flatMap(_.erityisenKoulutustehtävänJaksot.map(_.map(lengthInDaysInDateRange(_, alku, loppu)).sum)),
       sisäoppilaitosmainenMajoitus = lisätiedot.flatMap(_.sisäoppilaitosmainenMajoitus.map(_.map(lengthInDaysInDateRange(_, alku, loppu)).sum)),
       syy_alle18vuotiaana_aloitettuun_opiskeluun_aikuisten_lukiokoulutuksessa = lisätiedot.flatMap(_.alle18vuotiaanAikuistenLukiokoulutuksenAloittamisenSyy.map(_.get("fi"))),
       yhteislaajuus = row.osasuoritukset.filter(_.suorituksenTyyppi == "lukionkurssi").flatMap(_.koulutusmoduuliLaajuusArvo.map(_.toDouble)).sum
     ),
      oppiaineet = oppiaineidentiedot(row.päätasonSuoritus, row.osasuoritukset, oppiaineet)
    )
  }

  private def oppiaineidentiedot(paatasonsuoritus: RPäätasonSuoritusRow, osasuoritukset: Seq[ROsasuoritusRow], oppiaineet: Seq[LukioRaporttiOppiaineJaKurssit]): Seq[String] =  {
    val oppiaineentiedot = if (paatasonsuoritus.suorituksenTyyppi == lukionoppiaineenoppimaara) {
      oppiaineenOppimääränOppiaineenTiedot(paatasonsuoritus, osasuoritukset, oppiaineet)
    } else {
      oppimääränOppiaineenTiedot(osasuoritukset, oppiaineet)
    }

    oppiaineet.map(x => oppiaineentiedot(x.oppiaine).map(_.toString).mkString(","))
  }

  private def oppiaineenOppimääränOppiaineenTiedot(päätasonSuoritus: RPäätasonSuoritusRow, osasuoritukset: Seq[ROsasuoritusRow], oppiaineet: Seq[LukioRaporttiOppiaineJaKurssit]): LukioRaporttiOppiaine => Seq[LukioOppiaineenTiedot] = {
    (oppiaine: LukioRaporttiOppiaine) => if (päätasonSuoritus.matchesWith(oppiaine)) { Seq(toLukioOppiaineenTiedot(päätasonSuoritus, osasuoritukset)) } else { Nil }
  }

  private def oppimääränOppiaineenTiedot(osasuoritukset: Seq[ROsasuoritusRow], oppiaineet: Seq[LukioRaporttiOppiaineJaKurssit]): LukioRaporttiOppiaine => Seq[LukioOppiaineenTiedot] = {
    val osasuorituksetMap = osasuoritukset.groupBy(_.koulutusmoduuliKoodiarvo)
    val oppiaineenSuoritukset = (oppiaine: LukioRaporttiOppiaine) => osasuorituksetMap.getOrElse(oppiaine.koulutusmoduuliKoodiarvo, Nil).filter(_.matchesWith(oppiaine))
    (oppiaine: LukioRaporttiOppiaine) => oppiaineenSuoritukset(oppiaine).map(s => LukioOppiaineenTiedot(s.arviointiArvosanaKoodiarvo, osasuoritukset.count(_.ylempiOsasuoritusId.contains(s.osasuoritusId))))
  }

  private def toLukioOppiaineenTiedot(suoritus: RSuoritusRow, osasuoritukset: Seq[ROsasuoritusRow]) = {
    val laajuus = suoritus match {
      case _: RPäätasonSuoritusRow => osasuoritukset.size
      case s: ROsasuoritusRow => osasuoritukset.count(_.ylempiOsasuoritusId.contains(s.osasuoritusId))
    }
    LukioOppiaineenTiedot(suoritus.arviointiArvosanaKoodiarvo, laajuus)
  }

  private def oppiainekohtaisetKurssitiedot(row:LukioRaporttiRows, kurssit: Seq[LukioRaporttiKurssi]) = {
    LukioRaportinOppiaineenKurssitRow(
      stattisetKolumnit = LukioOppiaineenKurssienVälilehtiStaattisetKolumnit(
        oppijanOid = row.opiskeluoikeus.oppijaOid,
        hetu = row.henkilo.flatMap(_.hetu),
        sukinimi =  row.henkilo.map(_.sukunimi),
        etunimet = row.henkilo.map(_.etunimet),
        toimipiste = row.päätasonSuoritus.toimipisteNimi,
        opetussuunnitelma = opetussuunnitelma(row.päätasonSuoritus),
        suorituksenTyyppi = row.päätasonSuoritus.suorituksenTyyppi
      ),
      kurssit = kurssienTiedot(row.osasuoritukset, kurssit)
    )
  }

  private def kurssienTiedot(osasuoritukset: Seq[ROsasuoritusRow], kurssit: Seq[LukioRaporttiKurssi]) = {
    val osasuorituksetMap = osasuoritukset.groupBy(_.koulutusmoduuliKoodiarvo)
    kurssit.map { kurssi =>
      osasuorituksetMap.getOrElse(kurssi.koulutusmoduuliKoodiarvo, Nil).filter(_.matchesWith(kurssi)).map(kurssisuoritus =>
        LukioKurssinTiedot(
          kurssintyyppi = JsonSerializer.extract[Option[Koodistokoodiviite]](kurssisuoritus.data \ "koulutusmoduuli" \ "kurssinTyyppi").map(_.koodiarvo),
          arvosana = kurssisuoritus.arviointiArvosanaKoodiarvo,
          laajuus = kurssisuoritus.koulutusmoduuliLaajuusArvo,
          tunnustettu = JsonSerializer.extract[Option[OsaamisenTunnustaminen]](kurssisuoritus.data \ "tunnustettu").isDefined
        ).toString
      ).mkString(",")
    }
  }

  private def opetussuunnitelma(suoritus: RPäätasonSuoritusRow) = {
    if (suoritus.suorituksenTyyppi == lukionoppiaineenoppimaara) {
      JsonSerializer.extract[Option[String]](suoritus.data \ "koulutusmoduuli" \ "perusteenDiaarinumero").map(ePerusteet.findPerusteetByDiaarinumero(_).map(_.nimi("fi")).mkString(","))
    } else {
      JsonSerializer.extract[Option[Koodistokoodiviite]](suoritus.data \ "oppimäärä").flatMap(_.nimi.map(_.get("fi")))
    }
  }

  private[raportit] def removeContinuousSameTila(aikajaksot: Seq[ROpiskeluoikeusAikajaksoRow]): Seq[ROpiskeluoikeusAikajaksoRow] = {
    if (aikajaksot.size < 2) {
      aikajaksot
    } else {
      val rest = aikajaksot.dropWhile(_.tila == aikajaksot.head.tila)
      aikajaksot.head +: removeContinuousSameTila(rest)
    }
  }

  private[raportit] def lengthInDaysInDateRange(jakso: Jakso, alku: LocalDate, loppu: LocalDate) = {
    val hakuvali = Aikajakso(alku, Some(loppu))
    if (jakso.overlaps(hakuvali)) {
      val start = if (jakso.alku.isBefore(alku)) alku else jakso.alku
      val end = if (jakso.loppu.exists(_.isBefore(loppu))) jakso.loppu.get else loppu
      ChronoUnit.DAYS.between(start, end).toInt + 1
    } else {
      0
    }
  }

  private def oppiaineJaLisätiedotColumnSettings(oppiaineet: Seq[LukioRaporttiOppiaineJaKurssit]) = {
    Seq(
      CompactColumn("Opiskeluoikeuden oid"),
      CompactColumn("Lähdejärjestelmä"),
      CompactColumn("Koulutustoimija"),
      CompactColumn("Oppilaitoksen nimi"),
      CompactColumn("Toimipiste"),
      CompactColumn("Opiskeluoikeuden tunniste lähdejärjestelmässä"),
      Column("Oppijan oid"),
      Column("Hetu"),
      Column("Sukunimi"),
      Column("Etunimet"),
      CompactColumn("Opiskeluoikeuden alkamispäivä"),
      CompactColumn("Opiskeluoikeuden viimeisin tila"),
      CompactColumn("Opiskeluoikeuden tilat aikajakson aikana"),
      CompactColumn("Opetussuunnitelma"),
      CompactColumn("Suorituksen tyyppi"),
      CompactColumn("Suorituksen tila"),
      CompactColumn("Suorituksen vahvistuspäivä"),
      CompactColumn("Läsnäolopäiviä aikajakson aikana"),
      CompactColumn("Rahoitukset"),
      CompactColumn("Ryhmä"),
      CompactColumn("Pidennetty Päättymispäivä"),
      CompactColumn("Ulkomainen vaihto-opiskelija"),
      CompactColumn("Yksityisopiskelija"),
      CompactColumn("Ulkomaanjaksot"),
      CompactColumn("Erityisen koulutustehtävän tehtävät"),
      CompactColumn("Erityisen koulutustehtävän jaksot"),
      CompactColumn("Sisäoppilaitosmainen majoitus"),
      CompactColumn("Syy alle 18-vuotiaana aloitettuun opiskeluun aikuisten lukiokoulutuksessa"),
      CompactColumn("Yhteislaajuus")
    ) ++ oppiaineet.map(x => CompactColumn(x.oppiaine.toColumnTitle))
  }

  private def oppiaineKohtaisetColumnSettings(kurssit: Seq[LukioRaporttiKurssi]) = {
    Seq(
     Column("Oppijan oid"),
     Column("Hetu"),
     Column("Sukunimi"),
     Column("Etunimet"),
     CompactColumn("Toimipiste"),
     CompactColumn("Opetussuunnitelma"),
     CompactColumn("Suorituksen tyyppi")
   ) ++ kurssit.map(k => CompactColumn(k.toColumnTitle))
  }
}

case class LukioRaporttiOppiaineetVälilehtiMuut(
  opiskeluoikeudenOid: String,
  lähdejärjestelmä: Option[String],
  koulutustoimija: String,
  oppilaitoksenNimi: String,
  toimipiste: String,
  opiskeluoikeuden_tunniste_lähdejärjestelmässä: Option[String],
  oppijanOid: String,
  hetu: Option[String],
  sukunimi: Option[String],
  etunimet: Option[String],
  opiskeluoikeuden_alkamispäivä: Option[LocalDate],
  opiskeluoikeuden_viimeisin_tila: Option[String],
  opiskeluoikeuden_tilat_aikajakson_aikana: String,
  opetussuunnitelma: Option[String],
  suorituksenTyyppi: String,
  suorituksenTila: String,
  suorituksenVahvistuspäivä: Option[LocalDate],
  läsnäolopäiviä_aikajakson_aikana: Int,
  rahoitukset: String,
  ryhmä: Option[String],
  pidennettyPäättymispäivä: Boolean,
  ulkomainenVaihtoOpiskelija: Boolean,
  yksityisopiskelija: Boolean,
  ulkomaanjaksot: Option[Int],
  erityisen_koulutustehtävän_tehtävät: Option[String],
  erityisen_koulutustehtävän_jaksot: Option[Int],
  sisäoppilaitosmainenMajoitus: Option[Int],
  syy_alle18vuotiaana_aloitettuun_opiskeluun_aikuisten_lukiokoulutuksessa: Option[String],
  yhteislaajuus: Double
)

case class LukioOppiaineenTiedot(arvosana: Option[String], laajuus: Int) {
  override def toString: String = s"${arvosana.fold("Ei arvosanaa")("Arvosana " + _)}, $laajuus ${if (laajuus == 1) "kurssi" else "kurssia"}"
}

case class LukioRaporttiKaikkiOppiaineetVälilehtiRow(muut: LukioRaporttiOppiaineetVälilehtiMuut, oppiaineet: Seq[String]) {
 def toSeq: Seq[Any] = muut.productIterator.toList ++ oppiaineet
}

case class LukioOppiaineenKurssienVälilehtiStaattisetKolumnit(
  oppijanOid: String,
  hetu: Option[String],
  sukinimi: Option[String],
  etunimet: Option[String],
  toimipiste: String,
  opetussuunnitelma: Option[String],
  suorituksenTyyppi: String
)

case class LukioKurssinTiedot(kurssintyyppi: Option[String], arvosana: Option[String], laajuus: Option[Float], tunnustettu: Boolean) {
  override def toString: String = s"${kurssintyyppi.getOrElse("Ei tyyppiä")},${arvosana.map("Arvosana " + _).getOrElse("Ei arvosanaa")},${laajuus.map("Laajuus " + _).getOrElse("Ei laajuutta")}${if (tunnustettu) ",tunnustettu}" else ""}"
}

case class LukioRaportinOppiaineenKurssitRow(stattisetKolumnit: LukioOppiaineenKurssienVälilehtiStaattisetKolumnit, kurssit: Seq[String]) {
  def toSeq: Seq[Any] = stattisetKolumnit.productIterator.toList ++ kurssit
}

sealed trait LukioRaporttiOppiaineTaiKurssi {
  def nimi: String
  def koulutusmoduuliKoodiarvo: String
  def koulutusmoduuliPaikallinen: Boolean
}

case class LukioRaporttiOppiaine(nimi: String, koulutusmoduuliKoodiarvo: String, koulutusmoduuliPaikallinen: Boolean) extends LukioRaporttiOppiaineTaiKurssi {
  def toSheetTitle: String = s"$koulutusmoduuliKoodiarvo ${if (koulutusmoduuliPaikallinen) "p" else "v"} ${nimi.capitalize}"
  def toColumnTitle: String = s"$koulutusmoduuliKoodiarvo ${nimi.capitalize} ${if (koulutusmoduuliPaikallinen) "paikallinen" else "valtakunnallinen"}"
}

case class LukioRaporttiKurssi(nimi: String, koulutusmoduuliKoodiarvo: String, koulutusmoduuliPaikallinen: Boolean) extends LukioRaporttiOppiaineTaiKurssi {
  def toColumnTitle: String = s"$koulutusmoduuliKoodiarvo ${nimi.capitalize} ${if (koulutusmoduuliPaikallinen) "paikallinen" else "valtakunnallinen"}"
}

case class LukioRaporttiOppiaineJaKurssit(oppiaine: LukioRaporttiOppiaine, kurssit: Seq[LukioRaporttiKurssi])
