package fi.oph.koski.documentation

import fi.oph.koski.documentation.EuropeanSchoolOfHelsinkiExampleData._
import fi.oph.koski.henkilo.KoskiSpecificMockOppijat
import fi.oph.koski.henkilo.MockOppijat.asUusiOppija
import fi.oph.koski.schema._

import java.time.LocalDate
import java.time.LocalDate.{of => date}

object ExamplesEuropeanSchoolOfHelsinki {
  val alkamispäivä = date(2022, 8, 1)
  val päättymispäivä = alkamispäivä.plusYears(16).withMonth(5).withDayOfMonth(31)
  val lisätiedot = EuropeanSchoolOfHelsinkiOpiskeluoikeudenLisätiedot(
    ulkomaanjaksot = Some(List(ExamplesLukio.ulkomaanjakso)),
    /*maksuttomuus = Some(
      List(
        Maksuttomuus(
          alku = alkamispaiva,
          loppu = None,
          maksuton = true
        )
      )
    ),
    oikeuttaMaksuttomuuteenPidennetty = Some(List(
      OikeuttaMaksuttomuuteenPidennetty(alkamispaiva, alkamispaiva.plusDays(9)),
      OikeuttaMaksuttomuuteenPidennetty(alkamispaiva.plusDays(19), alkamispaiva.plusDays(30))
    ))*/
  )

  val opiskeluoikeus = EuropeanSchoolOfHelsinkiOpiskeluoikeus(
    oppilaitos = Some(europeanSchoolOfHelsinki),
    lisätiedot = Some(lisätiedot),
    tila = EuropeanSchoolOfHelsinkiOpiskeluoikeudenTila(
      List(
        EuropeanSchoolOfHelsinkiOpiskeluoikeusjakso(alkamispäivä, LukioExampleData.opiskeluoikeusAktiivinen)
      )
    ),
    suoritukset = List(
      nurserySuoritus("N1", alkamispäivä.plusYears(0)),
      nurserySuoritus("N2", alkamispäivä.plusYears(1)),
      primarySuoritus("P1", alkamispäivä.plusYears(2)),
      primarySuoritus("P2", alkamispäivä.plusYears(3), jääLuokalle = true),
      primarySuoritus("P2", alkamispäivä.plusYears(4)),
      primarySuoritus("P3", alkamispäivä.plusYears(5)),
      primarySuoritus("P4", alkamispäivä.plusYears(6)),
      primarySuoritus("P5", alkamispäivä.plusYears(7)),
      secondaryLowerSuoritus("S1", alkamispäivä.plusYears(8)),
      secondaryLowerSuoritus("S2", alkamispäivä.plusYears(9)),
      secondaryLowerSuoritus("S3", alkamispäivä.plusYears(10)),
      secondaryLowerSuoritus("S4", alkamispäivä.plusYears(11)),
      secondaryLowerSuoritus("S5", alkamispäivä.plusYears(12)),
      secondaryUpperSuoritus("S6", alkamispäivä.plusYears(13)),
      secondaryUpperSuoritus("S7", alkamispäivä.plusYears(14), jääLuokalle = true),
      secondaryUpperSuoritusFinal("S7", alkamispäivä.plusYears(15)),
    )
  )

  val examples = List(
    Example("europeanschoolofhelsinki", "European School of Helsinki", Oppija(asUusiOppija(KoskiSpecificMockOppijat.europeanSchoolOfHelsinki), List(opiskeluoikeus)))
  )

}
