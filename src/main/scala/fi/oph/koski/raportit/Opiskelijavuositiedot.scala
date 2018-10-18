package fi.oph.koski.raportit

import java.sql.Timestamp
import java.time.temporal.ChronoUnit
import java.time.{LocalDate, LocalDateTime}

import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.raportointikanta._
import fi.oph.koski.schema.{LähdejärjestelmäId, OpiskeluoikeudenTyyppi, Organisaatio, Osaamisalajakso}
import fi.oph.koski.util.FinnishDateFormat.{finnishDateFormat, finnishDateTimeFormat}
import scala.math.{min, max}

case class OpiskelijavuositiedotRow(
  opiskeluoikeusOid: String,
  lähdejärjestelmä: Option[String],
  lähdejärjestelmänId: Option[String],
  sisältyyOpiskeluoikeuteenOid: String,
  sisältyvätOpiskeluoikeudetOidit: String,
  sisältyvätOpiskeluoikeudetOppilaitokset: String,
  aikaleima: LocalDate,
  toimipisteOidit: String,
  oppijaOid: String,
  hetu: Option[String],
  sukunimi: Option[String],
  etunimet: Option[String],
  koulutusmoduulit: String,
  osaamisalat: Option[String],
  viimeisinOpiskeluoikeudenTila: String,
  opintojenRahoitukset: String,
  opiskeluoikeusPäättynyt: Boolean,
  päättymispäivä: Option[LocalDate],
  opiskelijavuosikertymä: Double,
  läsnäTaiValmistunutPäivät: Int,
  opiskelijavuoteenKuuluvatLomaPäivät: Int,
  muutLomaPäivät: Int,
  majoitusPäivät: Int,
  sisäoppilaitosmainenMajoitusPäivät: Int,
  vaativanErityisenTuenYhteydessäJärjestettäväMajoitusPäivät: Int,
  erityinenTukiPäivät: Int,
  vaativanErityisenTuenErityinenTehtäväPäivät: Int,
  hojksPäivät: Int,
  vaikeastiVammainenPäivät: Int,
  vammainenJaAvustajaPäivät: Int,
  osaAikaisuusProsentit: Option[String],
  osaAikaisuusKeskimäärin: Double,
  opiskeluvalmiuksiaTukevatOpinnotPäivät: Int,
  vankilaopetuksessaPäivät: Int,
  oppisopimusJossainPäätasonSuorituksessaPäivät: Int,
  lisätiedotHenkilöstökoulutus: Boolean,
  lisätiedotKoulutusvienti: Boolean
)

object Opiskelijavuositiedot {

  private[raportit] def buildRaportti(raportointiDatabase: RaportointiDatabase, oppilaitosOid: Organisaatio.Oid, alku: LocalDate, loppu: LocalDate): Seq[OpiskelijavuositiedotRow] = {
    val result = raportointiDatabase.opiskeluoikeusAikajaksot(oppilaitosOid, OpiskeluoikeudenTyyppi.ammatillinenkoulutus.koodiarvo, alku, loppu)
    val rows = result.map(r => buildRow(alku, loppu, r))
    rows
  }

  val columnSettings: Seq[(String, Column)] = Seq(
    "opiskeluoikeusOid" -> Column("Opiskeluoikeuden oid"),
    "lähdejärjestelmä" -> Column("Lähdejärjestelmä", width = Some(4000)),
    "lähdejärjestelmänId" -> Column("Opiskeluoikeuden tunniste lähdejärjestelmässä", width = Some(4000)),
    "sisältyyOpiskeluoikeuteenOid" -> Column("Sisältyy opiskeluoikeuteen", width = Some(4000)),
    "sisältyvätOpiskeluoikeudetOidit" -> Column("Sisältyvät opiskeluoikeudet", width = Some(4000)),
    "sisältyvätOpiskeluoikeudetOppilaitokset"-> Column("Sisältyvien opiskeluoikeuksien oppilaitokset", width = Some(4000)),
    "aikaleima" -> Column("Päivitetty"),
    "toimipisteOidit" -> Column("Toimipisteet"),
    "oppijaOid" -> Column("Oppijan oid"),
    "hetu" -> Column("Hetu"),
    "sukunimi" -> Column("Sukunimi"),
    "etunimet" -> Column("Etunimet"),
    "koulutusmoduulit" -> Column("Tutkinnot"),
    "osaamisalat" -> Column("Osaamisalat"),
    "viimeisinOpiskeluoikeudenTila" -> Column("Viimeisin tila"),
    "opintojenRahoitukset" -> Column("Rahoitukset"),
    "opiskeluoikeusPäättynyt" -> Column("Päättynyt", width = Some(2000)),
    "päättymispäivä" -> Column("Päättymispäivä"),
    "opiskelijavuosikertymä" -> Column("Opiskelijavuosikertymä (pv)", width = Some(2000)),
    "läsnäTaiValmistunutPäivät" -> Column("Läsnä tai valmistunut (pv)", width = Some(2000)),
    "opiskelijavuoteenKuuluvatLomaPäivät" -> Column("Opiskelijavuoteen kuuluvat lomat (pv)", width = Some(2000)),
    "muutLomaPäivät" -> Column("Muut lomat (pv)", width = Some(2000)),
    "majoitusPäivät" -> Column("Majoitus (pv)", width = Some(2000)),
    "sisäoppilaitosmainenMajoitusPäivät" -> Column("Sisäoppilaitosmainen majoitus (pv)", width = Some(2000)),
    "vaativanErityisenTuenYhteydessäJärjestettäväMajoitusPäivät" -> Column("Vaativan erityisen tuen yhteydessä järjestettävä majoitus (pv)", width = Some(2000)),
    "erityinenTukiPäivät" -> Column("Erityinen tuki (pv)", width = Some(2000)),
    "vaativanErityisenTuenErityinenTehtäväPäivät" -> Column("Vaativat erityisen tuen tehtävä (pv)", width = Some(2000)),
    "hojksPäivät" -> Column("Hojks (pv)", width = Some(2000)),
    "vaikeastiVammainenPäivät" -> Column("Vaikeasti vammainen (pv)", width = Some(2000)),
    "vammainenJaAvustajaPäivät" -> Column("Vammainen ja avustaja (pv)", width = Some(2000)),
    "osaAikaisuusProsentit" -> Column("Osa-aikaisuusjaksot (prosentit)", width = Some(2000)),
    "osaAikaisuusKeskimäärin" -> Column("Osa-aikaisuus keskimäärin (%)", width = Some(2000)),
    "opiskeluvalmiuksiaTukevatOpinnotPäivät" -> Column("Opiskeluvalmiuksia tukevat opinnot (pv)", width = Some(2000)),
    "vankilaopetuksessaPäivät" -> Column("Vankilaopetuksessa (pv)", width = Some(2000)),
    "oppisopimusJossainPäätasonSuorituksessaPäivät" -> Column("Oppisopimus (pv)", width = Some(2000)),
    "lisätiedotHenkilöstökoulutus" -> Column("Henkilöstökoulutus", width = Some(2000)),
    "lisätiedotKoulutusvienti" -> Column("Koulutusvienti", width = Some(2000))
  )

  def filename(oppilaitosOid: String, alku: LocalDate, loppu: LocalDate): String =
    s"opiskelijavuositiedot_${oppilaitosOid}_${alku.toString.replaceAll("-", "")}-${loppu.toString.replaceAll("-", "")}.xlsx"

  def title(oppilaitosOid: String, alku: LocalDate, loppu: LocalDate): String =
    s"Opiskelijavuositiedot $oppilaitosOid ${finnishDateFormat.format(alku)} - ${finnishDateFormat.format(loppu)}"

  def documentation(oppilaitosOid: String, alku: LocalDate, loppu: LocalDate, loadCompleted: Timestamp): String = s"""
    |Opiskelijavuositiedot (ammatillinen koulutus)
    |Oppilaitos: $oppilaitosOid
    |Aikajakso: ${finnishDateFormat.format(alku)} - ${finnishDateFormat.format(loppu)}
    |Raportti luotu: ${finnishDateTimeFormat.format(LocalDateTime.now)} (${finnishDateTimeFormat.format(loadCompleted.toLocalDateTime)} tietojen pohjalta)
    |
    |Tarkempia ohjeita taulukon sisällöstä:
    |
    |- Tutkinnot: kaikki opiskeluoikeudella olevat päätason suoritusten tutkinnot pilkulla erotettuna (myös ennen raportin aikajaksoa valmistuneet, ja raportin aikajakson jälkeen alkaneet)
    |  Valtakunnalliset tutkinnot käyttävät "koulutus" koodistoa, https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/koulutus/latest
    |- Osaamisalat: kaikkien ym. tutkintojen osaamisalat pilkulla erotettuna (myös ennen/jälkeen raportin aikajaksoa).
    |  Valtakunnalliset osaamisalat käyttävät "osaamisala" koodistoa, https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/osaamisala/latest
    |
    |- Viimeisin tila: opiskeluoikeuden tila raportin aikajakson lopussa
    |  Käyttää "koskiopiskeluoikeudentila" koodistoa, https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/koskiopiskeluoikeudentila/latest
    |- Rahoitukset: raportin aikajaksolla esiintyvät rahoitusmuodot pilkulla erotettuna
    |  Arvo on joko "-" (ei tiedossa), tai "opintojenrahoitus" koodistosta, https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/opintojenrahoitus/latest
    |- Päättynyt: kertoo onko opiskeluoikeus päättynyt raportin aikajaksolla
    |- Päättymispäivä: mukana vain jos opiskeluoikeus on päättynyt raportin aikajaksolla
    |
    |- Opiskelijavuosikertymä (pv): "läsnä tai valmistunut" + "opiskelijavuoteen kuuluvat lomat", kerrottuna kunkin päivän osa-aikaisuusprosentilla
    |- Läsnä tai valmistunut (pv): raportin aikajaksolle osuvat läsnä-päivät + valmistumispäivä (yksi päivä, jos se osuu aikajaksolle). Jos opiskeluoikeus päättyy muusta syystä, päättymispäivää ei lasketa tähän lukuun.
    |- Opiskelijavuoteen kuuluvat lomat (pv): raportin aikajaksolle osuvat lomapäivät, jotka ovat yhtenäisen loman ensimmäisten 28 pv joukossa (yhtenäinen loma on voinut alkaa ennen raportin aikajaksoa)
    |- Muut lomat (pv): raportin aikajaksolle osuvat lomapäivät, joita ei lasketa opiskelijavuoteen
    |- Osa-aikaisuusjaksot (prosentit): raportin aikajakson osa-aikaisuusprosentit pilkulla erotettuna
    |- Osa-aikaisuus keskimäärin (%): raportin aikajakson osa-aikaisuusprosenttien päivillä painotettu keskiarvo
    |- Oppisopimus (pv): opiskeluoikeuden jollain päätason suorituksella on oppisopimusjakso, joka mahtuu kokonaan tai osittain raportin aikajaksoon
    """.stripMargin.trim.stripPrefix("\n").stripSuffix("\n")

  def buildRow(alku: LocalDate, loppu: LocalDate, data: (ROpiskeluoikeusRow, Option[RHenkilöRow], Seq[ROpiskeluoikeusAikajaksoRow], Seq[RPäätasonSuoritusRow], Seq[ROpiskeluoikeusRow])): OpiskelijavuositiedotRow = {
    val (opiskeluoikeus, henkilö, aikajaksot, päätasonSuoritukset, sisältyvätOpiskeluoikeudet) = data
    val osaamisalat = päätasonSuoritukset
      .flatMap(s => JsonSerializer.extract[Option[List[Osaamisalajakso]]](s.data \ "osaamisala"))
      .flatten
      .filterNot(j => j.alku.exists(_.isAfter(loppu)))
      .filterNot(j => j.loppu.exists(_.isBefore(alku)))
      .map(_.osaamisala.koodiarvo)
      .sorted
      .distinct
    // jätä turha päättävän jakson "-" pois rahoitusmuotolistasta
    val aikajaksotOpintojenRahoitukseen = if (aikajaksot.size > 1 && aikajaksot.last.opiskeluoikeusPäättynyt && aikajaksot.last.opintojenRahoitus.isEmpty) aikajaksot.dropRight(1) else aikajaksot
    val opintojenRahoitukset = distinctAdjacent(aikajaksotOpintojenRahoitukseen.map(_.opintojenRahoitus.getOrElse("-"))).mkString(",")
    val lähdejärjestelmänId = JsonSerializer.extract[Option[LähdejärjestelmäId]](opiskeluoikeus.data \ "lähdejärjestelmänId")
    val (opiskelijavuoteenKuuluvatLomaPäivät, muutLomaPäivät) = lomaPäivät(aikajaksot)
    new OpiskelijavuositiedotRow(
      opiskeluoikeusOid = opiskeluoikeus.opiskeluoikeusOid,
      lähdejärjestelmä = lähdejärjestelmänId.map(_.lähdejärjestelmä.koodiarvo),
      lähdejärjestelmänId = lähdejärjestelmänId.flatMap(_.id),
      sisältyyOpiskeluoikeuteenOid = opiskeluoikeus.sisältyyOpiskeluoikeuteenOid.getOrElse(""),
      sisältyvätOpiskeluoikeudetOidit = sisältyvätOpiskeluoikeudet.map(_.opiskeluoikeusOid).mkString(","),
      sisältyvätOpiskeluoikeudetOppilaitokset = sisältyvätOpiskeluoikeudet.map(_.oppilaitosNimi).mkString(","),
      aikaleima = opiskeluoikeus.aikaleima.toLocalDateTime.toLocalDate,
      toimipisteOidit = päätasonSuoritukset.map(_.toimipisteOid).sorted.distinct.mkString(","),
      oppijaOid = opiskeluoikeus.oppijaOid,
      hetu = henkilö.flatMap(_.hetu),
      sukunimi = henkilö.map(_.sukunimi),
      etunimet = henkilö.map(_.etunimet),
      koulutusmoduulit = päätasonSuoritukset.map(_.koulutusmoduuliKoodiarvo).sorted.mkString(","),
      osaamisalat = if (osaamisalat.isEmpty) None else Some(osaamisalat.mkString(",")),
      viimeisinOpiskeluoikeudenTila = aikajaksot.last.tila,
      opintojenRahoitukset = opintojenRahoitukset,
      opiskeluoikeusPäättynyt = aikajaksot.last.opiskeluoikeusPäättynyt,
      päättymispäivä = aikajaksot.lastOption.filter(_.opiskeluoikeusPäättynyt).map(_.alku.toLocalDate), // toimii koska päättävä jakso on aina yhden päivän mittainen, jolloin truncateToDates ei muuta sen alkupäivää
      opiskelijavuosikertymä = opiskelijavuosikertymä(aikajaksot),
      läsnäTaiValmistunutPäivät = aikajaksoPäivät(aikajaksot, a => if (a.tila == "lasna" || a.tila == "valmistunut") 1 else 0),
      opiskelijavuoteenKuuluvatLomaPäivät = opiskelijavuoteenKuuluvatLomaPäivät,
      muutLomaPäivät = muutLomaPäivät,
      majoitusPäivät = aikajaksoPäivät(aikajaksot, _.majoitus),
      sisäoppilaitosmainenMajoitusPäivät = aikajaksoPäivät(aikajaksot, _.sisäoppilaitosmainenMajoitus),
      vaativanErityisenTuenYhteydessäJärjestettäväMajoitusPäivät = aikajaksoPäivät(aikajaksot, _.vaativanErityisenTuenYhteydessäJärjestettäväMajoitus),
      erityinenTukiPäivät = aikajaksoPäivät(aikajaksot, _.erityinenTuki),
      vaativanErityisenTuenErityinenTehtäväPäivät = aikajaksoPäivät(aikajaksot, _.vaativanErityisenTuenErityinenTehtävä),
      hojksPäivät = aikajaksoPäivät(aikajaksot, _.hojks),
      vaikeastiVammainenPäivät = aikajaksoPäivät(aikajaksot, _.vaikeastiVammainen),
      vammainenJaAvustajaPäivät = aikajaksoPäivät(aikajaksot, _.vammainenJaAvustaja),
      osaAikaisuusProsentit = Some(distinctAdjacent(aikajaksot.map(_.osaAikaisuus)).mkString(",")).filter(_ != "100"),
      osaAikaisuusKeskimäärin = aikajaksoPäivät(aikajaksot, _.osaAikaisuus).toDouble / aikajaksoPäivät(aikajaksot, _ => 1),
      opiskeluvalmiuksiaTukevatOpinnotPäivät = aikajaksoPäivät(aikajaksot, _.opiskeluvalmiuksiaTukevatOpinnot),
      vankilaopetuksessaPäivät = aikajaksoPäivät(aikajaksot, _.vankilaopetuksessa),
      oppisopimusJossainPäätasonSuorituksessaPäivät = aikajaksoPäivät(aikajaksot, _.oppisopimusJossainPäätasonSuorituksessa),
      lisätiedotHenkilöstökoulutus = opiskeluoikeus.lisätiedotHenkilöstökoulutus,
      lisätiedotKoulutusvienti = opiskeluoikeus.lisätiedotKoulutusvienti
    )
  }

  private def distinctAdjacent[A](input: Seq[A]): Seq[A] = {
    if (input.size < 2) {
      input
    } else {
      val rest = input.dropWhile(_ == input.head)
      input.head +: distinctAdjacent(rest)
    }
  }

  private def aikajaksoPäivät(aikajaksot: Seq[ROpiskeluoikeusAikajaksoRow], f: ROpiskeluoikeusAikajaksoRow => Byte): Int =
    aikajaksot.map(j => f(j) * j.lengthInDays).sum

  private[raportit] def lomaPäivät(j: ROpiskeluoikeusAikajaksoRow): (Int, Int) = {
    // "opiskelijavuoteen kuuluviksi päiviksi ei lueta koulutuksen järjestäjän päättämää yhtäjaksoisesti vähintään
    // neljä viikkoa kestävää lomajaksoa siltä osin, kuin loma-aika ylittää neljä viikkoa."
    // https://www.finlex.fi/fi/laki/alkup/2017/20170682)
    val NeljäViikkoa = 28
    if (j.tila != "loma") {
      (0, 0)
    } else {
      val lomapäiviäKäytettyEnnenTätäAikajaksoa = ChronoUnit.DAYS.between(j.tilaAlkanut.toLocalDate, j.alku.toLocalDate).toInt
      val päiviäTässäJaksossa = j.lengthInDays
      val opiskelijavuoteenKuuluvatLomaPäivät = max(min(päiviäTässäJaksossa, NeljäViikkoa - lomapäiviäKäytettyEnnenTätäAikajaksoa), 0)
      (opiskelijavuoteenKuuluvatLomaPäivät, päiviäTässäJaksossa - opiskelijavuoteenKuuluvatLomaPäivät)
    }
  }

  private[raportit] def lomaPäivät(aikajaksot: Seq[ROpiskeluoikeusAikajaksoRow]): (Int, Int) = {
    aikajaksot.map(lomaPäivät).reduce((a, b) => (a._1 + b._1, a._2 + b._2))
  }

  private def opiskelijavuosikertymä(aikajaksot: Seq[ROpiskeluoikeusAikajaksoRow]): Double = {
    aikajaksot.map(j => (j.tila match {
      case "loma" => lomaPäivät(j)._1
      case "lasna" | "valmistunut" => j.lengthInDays
      case _ => 0
    }) * (j.osaAikaisuus.toDouble / 100.0)).sum
  }
}