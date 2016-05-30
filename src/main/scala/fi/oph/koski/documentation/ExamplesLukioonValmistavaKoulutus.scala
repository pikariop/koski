package fi.oph.koski.documentation

import java.time.LocalDate.{of => date}
import fi.oph.koski.documentation.YleissivistavakoulutusExampleData._
import fi.oph.koski.localization.LocalizedString
import fi.oph.koski.oppija.MockOppijat
import fi.oph.koski.schema._
import ExampleData._
import LukioExampleData._

object ExamplesLukioonValmistavaKoulutus {
  val luvaTodistus = Oppija(
    MockOppijat.luva.vainHenkilötiedot,
    List(
      LukioonValmistavanKoulutuksenOpiskeluoikeus(
        alkamispäivä = Some(date(2008, 8, 15)),
        päättymispäivä = Some(date(2016, 6, 4)),
        oppilaitos = jyväskylänNormaalikoulu,
        koulutustoimija = None,
        läsnäolotiedot = None,
        tila = None,
        suoritukset = List(LukioonValmistavanKoulutuksenSuoritus(
          tila = tilaValmis,
          vahvistus = vahvistus,
          toimipiste = jyväskylänNormaalikoulu,
          koulutusmoduuli = LukioonValmistavaKoulutus(),
          osasuoritukset = Some(List(
            luvaKurssiSuoritus("STK", "Suomi toisena kielenä ja kirjallisuus", 2.0f),
            luvaKurssiSuoritus("STK", "Yhteiskuntatietous ja kulttuurintuntemus", 1.0f),
            luvaKurssiSuoritus("STK", "Opinto-ohjaus", 1.0f),
            kurssisuoritus(valtakunnallinenKurssi("KU1")).copy(arviointi = kurssinArviointi(7))
          ))
        ))
      )
    )
  )
  val examples = List(Example("lukioon valmistava koulutus", "Oppija on suorittanut lukioon valmistavan koulutuksen (LUVA)", luvaTodistus, 200))

  private def luvaKurssiSuoritus(koodi: String, kuvaus: String, laajuusKursseissa: Float) = LukioonValmistavanKurssinSuoritus(
    tila = tilaValmis,
    koulutusmoduuli = LukioonValmistavanKoulutuksenKurssi(
      tunniste = Paikallinenkoodi(koodi, LocalizedString.finnish(kuvaus), "jyväskylän-luva-kurssit"),
      laajuus = laajuus(laajuusKursseissa)
    ),
    arviointi = kurssinArviointi("S")
  )

}
