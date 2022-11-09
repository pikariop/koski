package fi.oph.koski.documentation

import fi.oph.koski.documentation.ExampleData.{englanti, helsinki, laajuusVuosiviikkotunneissa, sloveeni}
import fi.oph.koski.localization.LocalizedStringImplicits._
import fi.oph.koski.organisaatio.MockOrganisaatiot
import fi.oph.koski.schema.{PrimaryOsasuoritus, _}

import java.time.LocalDate

object EuropeanSchoolOfHelsinkiExampleData {
  lazy val europeanSchoolOfHelsinki: Oppilaitos = Oppilaitos(MockOrganisaatiot.europeanSchoolOfHelsinki, Some(Koodistokoodiviite("03782", None, "oppilaitosnumero", None)), Some("European School of Helsinki"))
  lazy val europeanSchoolOfHelsinkiToimipiste: Toimipiste = Toimipiste("1.2.246.562.10.12798841685")

  def vahvistus(päivä: LocalDate) = ExampleData.vahvistusPaikkakunnalla(päivä, europeanSchoolOfHelsinki, ExampleData.helsinki)

  def nurserySuoritus(luokkaaste: String, alkamispäivä: LocalDate, jääLuokalle: Boolean = false) = NurseryVuosiluokanSuoritus(
    koulutusmoduuli = NurseryLuokkaAste(luokkaaste),
    luokka = Some(s"${luokkaaste}A"),
    alkamispäivä = Some(alkamispäivä),
    toimipiste = europeanSchoolOfHelsinki,
    vahvistus = suoritusVahvistus(alkamispäivä.plusYears(1).withMonth(5).withDayOfMonth(31)),
    suorituskieli = ExampleData.englanti,
    jääLuokalle = jääLuokalle
  )

  def primarySuoritus(luokkaaste: String, alkamispäivä: LocalDate, jääLuokalle: Boolean = false, todistuksellaNäkyvätLisätiedot: Option[LocalizedString] = None) = PrimaryVuosiluokanSuoritus(
    koulutusmoduuli = PrimaryLuokkaAste(luokkaaste),
    luokka = Some(s"${luokkaaste}A"),
    alkamispäivä = Some(alkamispäivä),
    toimipiste = europeanSchoolOfHelsinki,
    vahvistus = suoritusVahvistus(alkamispäivä.plusYears(1).withMonth(5).withDayOfMonth(31)),
    suorituskieli = ExampleData.englanti,
    jääLuokalle = jääLuokalle,
    todistuksellaNäkyvätLisätiedot = todistuksellaNäkyvätLisätiedot,
    osasuoritukset = Some(List(
      primaryOppimisalueenOsasuoritusKieli(
        oppiainekoodi = "ONL",
        kieli = ExampleData.ruotsinKieli,
        yksilöllistettyOppimäärä = true,
        suorituskieli = ExampleData.suomenKieli,
        alaosasuorituskoodit = Some(List(
          "Listening and understanding", "Speaking", "Reading and understanding", "Writing", "Linguistic development"
        )),
        arviointi = osasuoritusArviointi(
          päivä = alkamispäivä.plusDays(30),
          kuvaus = Some(LocalizedString.finnish("Du talar svenska bra")),
        ),
        alaosasuoritusArviointi = primaryAlaoppimisalueArviointi(
          arvosana = "1",
          päivä = alkamispäivä.plusDays(60)
        ),
      ),
      primaryLapsiOppimisalueenOsasuoritus(
        oppiainekoodi = "TCAAL",
        alaosasuorituskoodit = Some(List(
          "Engages in learning", "Listens attentively", "Develops working habits", "Works independently", "Perseveres with difficult tasks", "Uses ICT", "Presents work carefully", "Produces quality homework"
        )),
        arviointi = osasuoritusArviointi(
          päivä = alkamispäivä.plusDays(30),
          kuvaus = Some(LocalizedString.finnish("hyvää työtä!"))
        ),
        alaosasuoritusArviointi = primaryAlaoppimisalueArviointi(
          päivä = alkamispäivä.plusDays(30)
        )
      ),
      primaryOppimisalueenOsasuoritusKieliAncientGreek(
        suorituskieli = ExampleData.englanti,
        alaosasuorituskoodit = Some(List(
          "Listening and understanding", "Speaking", "Reading and understanding", "Writing", "Linguistic development"
        )),
        arviointi = osasuoritusArviointi(
          päivä = alkamispäivä.plusDays(30),
          kuvaus = Some(LocalizedString.finnish("Parempi onni ensi kerralla")),
        ),
        alaosasuoritusArviointi = primaryAlaoppimisalueArviointi(
          arvosana = "1",
          päivä = alkamispäivä.plusDays(60)
        ),
      ),
      primaryOppimisalueenOsasuoritus(oppiainekoodi = "MA",
        yksilöllistettyOppimäärä = true,
        alaosasuorituskoodit = Some(List(
          "Numbers & Number system", "Calculation", "Measurement", "Shape and Space", "Data handling", "Problem solving"
        )),
        arviointi = osasuoritusArviointi(
          päivä = alkamispäivä.plusDays(30),
          kuvaus = Some(LocalizedString.finnish("Parempi onni ensi kerralla")),
        ),
        alaosasuoritusArviointi = primaryAlaoppimisalueArviointi(
          arvosana = "2",
          päivä = alkamispäivä.plusDays(65)
        )
      )
      // TODO: TOR-1685 Lisää esimerkkejä, kun oikeat oppiainekoodit selvillä, tee myös erilaiset eri luokka-asteille
    ))
  )

  def primaryLapsiOppimisalueenOsasuoritus(
    oppiainekoodi: String,
    arviointi: Option[List[EuropeanSchoolOfHelsinkiOsasuoritusArviointi]] = None,
    alaosasuorituskoodit: Option[List[String]] = None,
    alaosasuoritusArviointi: Option[List[PrimaryAlaoppimisalueArviointi]] = None
  ): PrimaryLapsiOppimisalueenSuoritus = {
    PrimaryLapsiOppimisalueenSuoritus(
      arviointi = arviointi,
      koulutusmoduuli = PrimaryLapsiOppimisalue(Koodistokoodiviite(oppiainekoodi, "europeanschoolofhelsinkilapsioppimisalue")),
      osasuoritukset = alaosasuorituskoodit.map(_.map(koodi => PrimaryLapsiOppimisalueenAlaosasuoritus(
        koulutusmoduuli = PrimaryLapsiAlaoppimisalue(
          tunniste = Koodistokoodiviite(koodi, "europeanschoolofhelsinkiprimarylapsialaoppimisalue")
        ),
        arviointi = alaosasuoritusArviointi
      )))
    )
  }

  def primaryOppimisalueenOsasuoritus(
    oppiainekoodi: String,
    laajuus: Int = 2,
    yksilöllistettyOppimäärä: Boolean = false,
    suorituskieli: Koodistokoodiviite = ExampleData.englanti,
    arviointi: Option[List[EuropeanSchoolOfHelsinkiOsasuoritusArviointi]] = None,
    alaosasuorituskoodit: Option[List[String]] = None,
    alaosasuoritusArviointi: Option[List[PrimaryAlaoppimisalueArviointi]] = None
  ): PrimaryOppimisalueenSuoritus = {
    PrimaryOppimisalueenSuoritus(
      koulutusmoduuli = EuropeanSchoolOfHelsinkiMuuOppiaine(
        Koodistokoodiviite(oppiainekoodi, "europeanschoolofhelsinkimuuoppiaine"),
        laajuus = LaajuusVuosiviikkotunneissa(laajuus)
      ),
      yksilöllistettyOppimäärä = yksilöllistettyOppimäärä,
      suorituskieli = suorituskieli,
      arviointi = arviointi,
      osasuoritukset = alaosasuorituskoodit.map(_.map(koodi => PrimaryOppimisalueenAlaosasuoritus(
        koulutusmoduuli = PrimaryAlaoppimisalue(
          tunniste = Koodistokoodiviite(koodi, "europeanschoolofhelsinkiprimaryalaoppimisalue"),
        ),
        arviointi = alaosasuoritusArviointi
      )))
    )
  }

  def primaryOppimisalueenOsasuoritusKieli(
    oppiainekoodi: String,
    laajuus: Int = 2,
    yksilöllistettyOppimäärä: Boolean = false,
    kieli: Koodistokoodiviite = ExampleData.englanti,
    suorituskieli: Koodistokoodiviite = ExampleData.englanti,
    arviointi: Option[List[EuropeanSchoolOfHelsinkiOsasuoritusArviointi]] = None,
    alaosasuoritusArviointi: Option[List[PrimaryAlaoppimisalueArviointi]] = None,
    alaosasuorituskoodit: Option[List[String]] = None
  ): PrimaryOppimisalueenSuoritus = {
    PrimaryOppimisalueenSuoritus(
      arviointi = arviointi,
      koulutusmoduuli = EuropeanSchoolOfHelsinkiKieliOppiaine(
        Koodistokoodiviite(oppiainekoodi, "europeanschoolofhelsinkikielioppiaine"),
        laajuus = LaajuusVuosiviikkotunneissa(laajuus),
        kieli = kieli
      ),
      yksilöllistettyOppimäärä = yksilöllistettyOppimäärä,
      suorituskieli = suorituskieli,
      osasuoritukset = alaosasuorituskoodit.map(_.map(koodi => PrimaryOppimisalueenAlaosasuoritus(
        koulutusmoduuli = PrimaryAlaoppimisalue(
          tunniste = Koodistokoodiviite(koodi, "europeanschoolofhelsinkiprimaryalaoppimisalue")
        ),
        arviointi = alaosasuoritusArviointi
      )))
    )
  }

  def primaryOppimisalueenOsasuoritusKieliAncientGreek(
    laajuus: Int = 2,
    yksilöllistettyOppimäärä: Boolean = false,
    suorituskieli: Koodistokoodiviite = ExampleData.englanti,
    arviointi: Option[List[EuropeanSchoolOfHelsinkiOsasuoritusArviointi]] = None,
    alaosasuoritusArviointi: Option[List[PrimaryAlaoppimisalueArviointi]] = None,
    alaosasuorituskoodit: Option[List[String]] = None
  ): PrimaryOppimisalueenSuoritus = {
    val oppiainekoodi = "GRC"
    val kieli = Koodistokoodiviite("EL", "kieli")
    PrimaryOppimisalueenSuoritus(
      arviointi = arviointi,
      koulutusmoduuli = EuropeanSchoolOfHelsinkiKielioppiaineAncientGreek(
        Koodistokoodiviite(oppiainekoodi, "europeanschoolofhelsinkikielioppiaine"),
        laajuus = LaajuusVuosiviikkotunneissa(laajuus),
        kieli = kieli
      ),
      yksilöllistettyOppimäärä = yksilöllistettyOppimäärä,
      suorituskieli = suorituskieli,
      osasuoritukset = alaosasuorituskoodit.map(_.map(koodi => PrimaryOppimisalueenAlaosasuoritus(
        koulutusmoduuli = PrimaryAlaoppimisalue(
          tunniste = Koodistokoodiviite(koodi, "europeanschoolofhelsinkiprimaryalaoppimisalue")
        ),
        arviointi = alaosasuoritusArviointi
      )))
    )
  }

  def osasuoritusArviointi(
    arvosana: String = "pass",
    kuvaus: Option[LocalizedString] = None,
    arvioitsijat: Option[List[Arvioitsija]] = Some(List(Arvioitsija("Pekka Paunanen"))),
    päivä: LocalDate
  ): Option[List[EuropeanSchoolOfHelsinkiOsasuoritusArviointi]] = {
    Some(List(EuropeanSchoolOfHelsinkiOsasuoritusArviointi(
      arvosana = Koodistokoodiviite(arvosana, "arviointiasteikkoeuropeanschoolofhelsinkiosasuoritus"),
      kuvaus = kuvaus,
      päivä = päivä,
      arvioitsijat = arvioitsijat
    )))
  }

  def primaryAlaoppimisalueArviointi(
    arvosana: String = "3",
    arvioitsijat: Option[List[Arvioitsija]] = Some(List(Arvioitsija("Pekka Paunanen"))),
    päivä: LocalDate
  ): Option[List[PrimaryAlaoppimisalueArviointi]] = {
    Some(List(PrimaryAlaoppimisalueArviointi(
      arvosana = Koodistokoodiviite(arvosana, "arviointiasteikkoeuropeanschoolofhelsinkiprimarymark"),
      päivä = päivä,
      arvioitsijat = arvioitsijat
    )))
  }

  def secondaryLowerSuoritus(luokkaaste: String, alkamispäivä: LocalDate, jääLuokalle: Boolean = false) = SecondaryLowerVuosiluokanSuoritus(
    koulutusmoduuli = SecondaryLowerLuokkaAste(luokkaaste),
    luokka = Some(s"${luokkaaste}A"),
    alkamispäivä = Some(alkamispäivä),
    toimipiste = europeanSchoolOfHelsinki,
    vahvistus = suoritusVahvistus(alkamispäivä.plusYears(1).withMonth(5).withDayOfMonth(31)),
    suorituskieli = ExampleData.englanti,
    jääLuokalle = jääLuokalle,
    osasuoritukset = Some(List(
      secondaryLowerMuunOppiaineenOsasuoritus(
        oppiainekoodi = "MA",
        arviointi = secondaryLowerArviointi(luokkaaste, alkamispäivä.plusMonths(2))
      ),
      secondaryLowerKieliOppiaineenOsasuoritus(
        oppiainekoodi = "L2",
        arviointi = secondaryLowerArviointi(luokkaaste, alkamispäivä.plusMonths(3))
      ),
      secondaryLowerKieliOppiaineenOsasuoritusLatin(
        arviointi = secondaryLowerArviointi(luokkaaste, alkamispäivä.plusMonths(4))
      )
    ))
    // TODO: TOR-1685 Lisää esimerkkejä, kun oikeat oppiainekoodit selvillä, tee myös erilaiset eri luokka-asteille
  )


  def secondaryLowerMuunOppiaineenOsasuoritus(
    oppiainekoodi: String,
    laajuus: Int = 2,
    suorituskieli: Koodistokoodiviite = ExampleData.englanti,
    arviointi: Option[List[SecondaryLowerArviointi]] = None
  ): SecondaryLowerOppiaineenSuoritus = {
    SecondaryLowerOppiaineenSuoritus(
      koulutusmoduuli = EuropeanSchoolOfHelsinkiMuuOppiaine(
        Koodistokoodiviite(oppiainekoodi, "europeanschoolofhelsinkimuuoppiaine"),
        laajuus = LaajuusVuosiviikkotunneissa(laajuus)
      ),
      suorituskieli = suorituskieli,
      arviointi = arviointi
    )
  }

  def secondaryLowerKieliOppiaineenOsasuoritus(
    oppiainekoodi: String,
    laajuus: Int = 2,
    kieli: Koodistokoodiviite = ExampleData.englanti,
    suorituskieli: Koodistokoodiviite = ExampleData.englanti,
    arviointi: Option[List[SecondaryLowerArviointi]] = None
  ): SecondaryLowerOppiaineenSuoritus = {
    SecondaryLowerOppiaineenSuoritus(
      koulutusmoduuli = EuropeanSchoolOfHelsinkiKieliOppiaine(
        Koodistokoodiviite(oppiainekoodi, "europeanschoolofhelsinkikielioppiaine"),
        laajuus = LaajuusVuosiviikkotunneissa(laajuus),
        kieli = kieli
      ),
      suorituskieli = suorituskieli,
      arviointi = arviointi
    )
  }

  def secondaryLowerKieliOppiaineenOsasuoritusLatin(
    laajuus: Int = 2,
    suorituskieli: Koodistokoodiviite = ExampleData.englanti,
    arviointi: Option[List[SecondaryLowerArviointi]] = None
  ): SecondaryLowerOppiaineenSuoritus = {
    val oppiainekoodi = "LA"
    val kieli = "LA"
    SecondaryLowerOppiaineenSuoritus(
      koulutusmoduuli = EuropeanSchoolOfHelsinkiKieliOppiaineLatin(
        Koodistokoodiviite(oppiainekoodi, "europeanschoolofhelsinkikielioppiaine"),
        laajuus = LaajuusVuosiviikkotunneissa(laajuus),
        kieli = Koodistokoodiviite(kieli, "kieli")
      ),
      suorituskieli = suorituskieli,
      arviointi = arviointi
    )
  }

  def secondaryGradeArviointi(
    arvosana: String = "B",
    kuvaus: Option[LocalizedString] = None,
    arvioitsijat: Option[List[Arvioitsija]] = Some(List(Arvioitsija("Pekka Paunanen"))),
    päivä: LocalDate
  ): Option[List[SecondaryGradeArviointi]] = {
    Some(List(SecondaryGradeArviointi(
      arvosana = Koodistokoodiviite(arvosana, "arviointiasteikkoeuropeanschoolofhelsinkisecondarygrade"),
      kuvaus = kuvaus,
      päivä = päivä,
      arvioitsijat = arvioitsijat
    )))
  }

  def secondaryUpperSuoritusS6(luokkaaste: String, alkamispäivä: LocalDate, jääLuokalle: Boolean = false) = {
    SecondaryUpperVuosiluokanSuoritus(
      koulutusmoduuli = SecondaryUpperLuokkaAste(luokkaaste),
      luokka = Some(s"${luokkaaste}A"),
      alkamispäivä = Some(alkamispäivä),
      toimipiste = europeanSchoolOfHelsinki,
      vahvistus = suoritusVahvistus(alkamispäivä.plusYears(1).withMonth(5).withDayOfMonth(31)),
      suorituskieli = ExampleData.englanti,
      jääLuokalle = jääLuokalle,
      osasuoritukset = Some(List(
        secondaryUpperMuunOppiaineenOsasuoritusS6(
          oppiainekoodi = "PE",
          arviointi = secondaryNumericalMarkArviointi(päivä = alkamispäivä.plusMonths(3))
        ),
        secondaryUpperKieliOppiaineenOsasuoritusS6(
          oppiainekoodi = "L1",
          kieli = sloveeni,
          arviointi = secondaryNumericalMarkArviointi(päivä = alkamispäivä.plusMonths(4))
        ),
        secondaryUpperMuunOppiaineenOsasuoritusS6(
          oppiainekoodi = "MA",
          arviointi = secondaryNumericalMarkArviointi(päivä = alkamispäivä.plusMonths(5))
        )
      ))
      // TODO: TOR-1685 Lisää esimerkkejä, kun oikeat oppiainekoodit selvillä, tee myös erilaiset eri luokka-asteille
    )
  }

  def secondaryUpperSuoritusS7(luokkaaste: String, alkamispäivä: LocalDate, jääLuokalle: Boolean = false) = {
    SecondaryUpperVuosiluokanSuoritus(
      koulutusmoduuli = SecondaryUpperLuokkaAste(luokkaaste),
      luokka = Some(s"${luokkaaste}A"),
      alkamispäivä = Some(alkamispäivä),
      toimipiste = europeanSchoolOfHelsinki,
      vahvistus = suoritusVahvistus(alkamispäivä.plusYears(1).withMonth(5).withDayOfMonth(31)),
      suorituskieli = ExampleData.englanti,
      jääLuokalle = jääLuokalle,
      osasuoritukset = Some(List(
        secondaryUpperMuunOppiaineenOsasuoritusS7(
          oppiainekoodi = "PE",
          arviointi = secondaryS7PreliminaryMarkArviointi(päivä = alkamispäivä.plusMonths(3))
        ),
        secondaryUpperKieliOppiaineenOsasuoritusS7(
          oppiainekoodi = "L1",
          kieli = sloveeni,
          arviointi = secondaryS7PreliminaryMarkArviointi(päivä = alkamispäivä.plusMonths(4))
        ),
        secondaryUpperMuunOppiaineenOsasuoritusS7(
          oppiainekoodi = "MA",
          arviointi = secondaryS7PreliminaryMarkArviointi(päivä = alkamispäivä.plusMonths(5))
        )
      ))
      // TODO: TOR-1685 Lisää esimerkkejä, kun oikeat oppiainekoodit selvillä, tee myös erilaiset eri luokka-asteille
    )
  }

  def secondaryLowerArviointi(luokkaaste: String, päivä: LocalDate): Option[List[SecondaryLowerArviointi]] = {
    luokkaaste match {
      case "S4" | "S5" => secondaryNumericalMarkArviointi(
        päivä = päivä
      )
      case "S1" | "S2" | "S3" => secondaryGradeArviointi(
        päivä = päivä
      )
    }
  }

  def secondaryUpperMuunOppiaineenOsasuoritusS6(
    oppiainekoodi: String,
    laajuus: Int = 2,
    suorituskieli: Koodistokoodiviite = ExampleData.englanti,
    arviointi: Option[List[SecondaryNumericalMarkArviointi]] = None
  ): SecondaryUpperOppiaineenSuoritus = {
    SecondaryUpperOppiaineenSuoritusS6(
      koulutusmoduuli = EuropeanSchoolOfHelsinkiMuuOppiaine(
        Koodistokoodiviite(oppiainekoodi, "europeanschoolofhelsinkimuuoppiaine"),
        laajuus = LaajuusVuosiviikkotunneissa(laajuus)
      ),
      suorituskieli = suorituskieli,
      arviointi = arviointi
    )
  }

  def secondaryUpperKieliOppiaineenOsasuoritusS6(
    oppiainekoodi: String,
    laajuus: Int = 2,
    kieli: Koodistokoodiviite = ExampleData.englanti,
    suorituskieli: Koodistokoodiviite = ExampleData.englanti,
    arviointi: Option[List[SecondaryNumericalMarkArviointi]] = None
  ): SecondaryUpperOppiaineenSuoritus = {
    SecondaryUpperOppiaineenSuoritusS6(
      koulutusmoduuli = EuropeanSchoolOfHelsinkiKieliOppiaine(
        Koodistokoodiviite(oppiainekoodi, "europeanschoolofhelsinkikielioppiaine"),
        laajuus = LaajuusVuosiviikkotunneissa(laajuus),
        kieli = kieli
      ),
      suorituskieli = suorituskieli,
      arviointi = arviointi
    )
  }

  def secondaryUpperMuunOppiaineenOsasuoritusS7(
    oppiainekoodi: String,
    laajuus: Int = 2,
    suorituskieli: Koodistokoodiviite = ExampleData.englanti,
    arviointi: Option[List[SecondaryS7PreliminaryMarkArviointi]] = None
  ): SecondaryUpperOppiaineenSuoritus = {
    SecondaryUpperOppiaineenSuoritusS7(
      koulutusmoduuli = EuropeanSchoolOfHelsinkiMuuOppiaine(
        Koodistokoodiviite(oppiainekoodi, "europeanschoolofhelsinkimuuoppiaine"),
        laajuus = LaajuusVuosiviikkotunneissa(laajuus)
      ),
      suorituskieli = suorituskieli,
      osasuoritukset = Some(List(
        S7OppiaineenAlaosasuoritus(
          koulutusmoduuli = S7OppiaineKomponenttiA(
            Koodistokoodiviite("A", "europeanschoolofhelsinkis7oppiaineenkomponentti")
          ),
          arviointi = arviointi
        ),
        S7OppiaineenAlaosasuoritus(
          koulutusmoduuli = S7OppiaineKomponenttiB(
            Koodistokoodiviite("B", "europeanschoolofhelsinkis7oppiaineenkomponentti")
          ),
          arviointi = arviointi
        ),
        S7OppiaineenAlaosasuoritus(
          koulutusmoduuli = S7OppiaineKomponenttiYearMark(
            Koodistokoodiviite("yearmark", "europeanschoolofhelsinkis7oppiaineenkomponentti")
          ),
          arviointi = arviointi
        )
      ))
    )
  }

  def secondaryUpperKieliOppiaineenOsasuoritusS7(
    oppiainekoodi: String,
    laajuus: Int = 2,
    kieli: Koodistokoodiviite = ExampleData.englanti,
    suorituskieli: Koodistokoodiviite = ExampleData.englanti,
    arviointi: Option[List[SecondaryS7PreliminaryMarkArviointi]] = None
  ): SecondaryUpperOppiaineenSuoritus = {
    SecondaryUpperOppiaineenSuoritusS7(
      koulutusmoduuli = EuropeanSchoolOfHelsinkiKieliOppiaine(
        Koodistokoodiviite(oppiainekoodi, "europeanschoolofhelsinkikielioppiaine"),
        laajuus = LaajuusVuosiviikkotunneissa(laajuus),
        kieli = kieli
      ),
      suorituskieli = suorituskieli,
      osasuoritukset = Some(List(
        S7OppiaineenAlaosasuoritus(
          koulutusmoduuli = S7OppiaineKomponenttiA(
            Koodistokoodiviite("A", "europeanschoolofhelsinkis7oppiaineenkomponentti")
          ),
          arviointi = arviointi
        ),
        S7OppiaineenAlaosasuoritus(
          koulutusmoduuli = S7OppiaineKomponenttiB(
            Koodistokoodiviite("B", "europeanschoolofhelsinkis7oppiaineenkomponentti")
          ),
          arviointi = arviointi
        ),
        S7OppiaineenAlaosasuoritus(
          koulutusmoduuli = S7OppiaineKomponenttiYearMark(
            Koodistokoodiviite("yearmark", "europeanschoolofhelsinkis7oppiaineenkomponentti")
          ),
          arviointi = arviointi
        )
      ))
    )
  }

  def secondaryNumericalMarkArviointi(
    arvosana: String = "7.5",
    kuvaus: Option[LocalizedString] = None,
    arvioitsijat: Option[List[Arvioitsija]] = Some(List(Arvioitsija("Pekka Paunanen"))),
    päivä: LocalDate
  ): Option[List[SecondaryNumericalMarkArviointi]] = {
    Some(List(SecondaryNumericalMarkArviointi(
      arvosana = SynteettinenKoodiviite(koodiarvo = arvosana, koodistoUri = "esh/numericalmark"),
      kuvaus = kuvaus,
      päivä = päivä,
      arvioitsijat = arvioitsijat
    )))
  }

  def secondaryS7PreliminaryMarkArviointi(
    arvosana: String = "8.9",
    kuvaus: Option[LocalizedString] = None,
    arvioitsijat: Option[List[Arvioitsija]] = Some(List(Arvioitsija("Pekka Paunanen"))),
    päivä: LocalDate
  ): Option[List[SecondaryS7PreliminaryMarkArviointi]] = {
    Some(List(SecondaryS7PreliminaryMarkArviointi(
      arvosana = SynteettinenKoodiviite(koodiarvo = arvosana, koodistoUri = "esh/s7preliminarymark"),
      kuvaus = kuvaus,
      päivä = päivä,
      arvioitsijat = arvioitsijat
    )))
  }

  def suoritusVahvistus(päivä: LocalDate) = ExampleData.vahvistusPaikkakunnalla(päivä, europeanSchoolOfHelsinki, helsinki)
}