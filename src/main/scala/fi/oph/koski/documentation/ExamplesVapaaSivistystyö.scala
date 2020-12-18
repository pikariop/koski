package fi.oph.koski.documentation

import java.time.LocalDate.{of => date}

import fi.oph.koski.documentation.VapaaSivistystyöExampleData._
import fi.oph.koski.documentation.ExampleData._
import fi.oph.koski.localization.LocalizedStringImplicits._
import fi.oph.koski.henkilo.MockOppijat
import fi.oph.koski.henkilo.MockOppijat.asUusiOppija
import fi.oph.koski.organisaatio.MockOrganisaatiot
import fi.oph.koski.schema._

object ExamplesVapaaSivistystyö {
  lazy val examples = List(
    Example("vapaa sivistystyö - oppivelvollisille suunnattu koulutus", "Oppija suorittaa oppivelvollisille suunnattua koulutusta kansanopistossa", VapaaSivistystyöExample.oppivelvollisuuskoulutusExample)
  )
}

object VapaaSivistystyöExample {

  lazy val opiskeluoikeus = VapaanSivistystyönOpiskeluoikeus(
    arvioituPäättymispäivä = Some(date(2022, 5, 31)),
    tila = VapaanSivistystyönOpiskeluoikeudenTila(List(
      VapaanSivistystyönOpiskeluoikeusjakso(date(2021, 9, 1), opiskeluoikeusLäsnä)
    )),
    lisätiedot = None,
    oppilaitos = Some(varsinaisSuomenKansanopisto),
    suoritukset = List(OppivelvollisilleSuunnatunVapaanSivistystyönKoulutuksenSuoritus(
      toimipiste = varsinaisSuomenKansanopistoToimipiste,
      tyyppi = Koodistokoodiviite("lukionoppimaara", koodistoUri = "suorituksentyyppi"), // TODO, oikea tyyppi
      koulutusmoduuli = OppivelvollisilleSuunnattuVapaanSivistystyönKoulutus(perusteenDiaarinumero = Some("OPH-1234-2020")), // TODO: Oikea perusteen diaarinumero, kunhan oikea diaarinumero saatavilla ja sisältö tuotu Kosken mockdataan
      vahvistus = vahvistus(päivä = date(2022, 5, 31)),
      suorituskieli = suomenKieli,
      todistuksellaNäkyvätLisätiedot = Some("Opinnot suoritettu pandemian vuoksi etäopintoina"),
      osasuoritukset = Some(List(
        OppivelvollisilleSuunnatunVapaanSivistystyönOsaamiskokonaisuudenSuoritus(
          tyyppi = Koodistokoodiviite("lukionoppimaara", koodistoUri = "suorituksentyyppi"), // TODO, oikea tyyppi
          koulutusmoduuli = OppivelvollisilleSuunnattuVapaanSivistystyönOsaamiskokonaisuus(
            tunniste = Koodistokoodiviite("1002", "opintokokonaisuusnimet")
          ),
          osasuoritukset = Some(List(
            OppivelvollisilleSuunnatunVapaanSivistystyönOpintokokonaisuudenSuoritus(
              tyyppi = Koodistokoodiviite("lukionoppimaara", koodistoUri = "suorituksentyyppi"), // TODO, oikea tyyppi
              koulutusmoduuli = OppivelvollisilleSuunnattuVapaanSivistystyönOpintokokonaisuus(
                tunniste = PaikallinenKoodi(koodiarvo = "A01", nimi = "Arjen rahankäyttö"),
                kuvaus = "Arjen rahankäyttö",
                laajuus = laajuus(2.0)
              ),
              arviointi = Some(List(OppivelvollisilleSuunnatunVapaanSivistystyönOpintokokonaisuudenArviointi(
                arvosana = Koodistokoodiviite("Hyväksytty", "arviointiasteikkovst"),
                päivä = date(2021, 10, 30)
              )))
            )
          ))
        ),
        OppivelvollisilleSuunnatunVapaanSivistystyönValinnaistenSuuntautumisopintojenSuoritus(
          tyyppi = Koodistokoodiviite("lukionoppiaine", koodistoUri = "suorituksentyyppi"), // TODO, oikea tyyppi
          koulutusmoduuli = OppivelvollisilleSuunnatunVapaanSivistystyönValinnaisetSuuntautumisopinnot(),
          osasuoritukset = Some(List(
            OppivelvollisilleSuunnatunVapaanSivistystyönOpintokokonaisuudenSuoritus(
              tyyppi = Koodistokoodiviite("lukionoppimaara", koodistoUri = "suorituksentyyppi"), // TODO, oikea tyyppi
              koulutusmoduuli = OppivelvollisilleSuunnattuVapaanSivistystyönOpintokokonaisuus(
                tunniste = PaikallinenKoodi(koodiarvo = "ATX01", nimi = "Tietokoneen huolto"),
                kuvaus = "Nykyaikaisen tietokoneen tyypilliset huoltotoimenpiteet",
                laajuus = laajuus(5.0)
              ),
              arviointi = Some(List(OppivelvollisilleSuunnatunVapaanSivistystyönOpintokokonaisuudenArviointi(
                arvosana = Koodistokoodiviite("Hyväksytty", "arviointiasteikkovst"),
                päivä = date(2021, 11, 12)
              )))
            )
          ))
        )
      ))
    ))
  )

  lazy val oppivelvollisuuskoulutusExample = Oppija(
    VapaaSivistystyöExampleData.exampleHenkilö,
    List(opiskeluoikeus)
  )
}

object VapaaSivistystyöExampleData {
  val exampleHenkilö = asUusiOppija(MockOppijat.vapaaSivistystyöOppivelvollinen)

  lazy val varsinaisSuomenKansanopisto: Oppilaitos = Oppilaitos(MockOrganisaatiot.varsinaisSuomenKansanopisto, Some(Koodistokoodiviite("01694", None, "oppilaitosnumero", None)), Some("Varsinais-Suomen kansanopisto"))

  lazy val varsinaisSuomenKansanopistoToimipiste: Toimipiste = Toimipiste(MockOrganisaatiot.varsinaisSuomenKansanopistoToimipiste)

  def laajuus(arvo: Double): LaajuusOpintopisteissä = LaajuusOpintopisteissä(arvo = arvo, yksikkö = laajuusOpintopisteissä)
}
