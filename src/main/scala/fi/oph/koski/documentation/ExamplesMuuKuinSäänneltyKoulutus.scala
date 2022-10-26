package fi.oph.koski.documentation

import fi.oph.koski.documentation.ExampleData.opiskeluoikeusLäsnä
import fi.oph.koski.documentation.VapaaSivistystyöExample.opiskeluoikeusHyväksytystiSuoritettu
import fi.oph.koski.organisaatio.MockOrganisaatiot
import fi.oph.koski.schema.{Koodistokoodiviite, Koulutustoimija, LaajuusTunneissa, MuuKuinSäänneltyKoulutus, MuunKuinSäännellynKoulutuksenArviointi, MuunKuinSäännellynKoulutuksenOpiskeluoikeudenJakso, MuunKuinSäännellynKoulutuksenOpiskeluoikeus, MuunKuinSäännellynKoulutuksenOsasuorituksenKoulutusmoduuli, MuunKuinSäännellynKoulutuksenOsasuoritus, MuunKuinSäännellynKoulutuksenPäätasonSuoritus, MuunKuinSäännellynKoulutuksenTila, OidOrganisaatio, Oppilaitos, PaikallinenKoodi}
import fi.oph.koski.localization.LocalizedStringImplicits._
import fi.oph.koski.schema.LocalizedString.finnish

import java.time.LocalDate

object ExamplesMuuKuinSäänneltyKoulutus {
  lazy val jatkuvaKoulutusOyKoulutustoimija: Koulutustoimija = Koulutustoimija(
    oid = MockOrganisaatiot.MuuKuinSäänneltyKoulutusToimija.koulutustoimija,
    nimi = Some(finnish("Jatkuva Koulutus Oy")),
    yTunnus = Some("3199960-3"),
    kotipaikka = Some(Koodistokoodiviite("091", "kunta")),
  )

  lazy val jatkuvaKoulutusOyOppilaitos: Oppilaitos = Oppilaitos(
    oid = MockOrganisaatiot.MuuKuinSäänneltyKoulutusToimija.oppilaitos,
    nimi = Some(finnish("Jatkuva Koulutus Oy")),
    kotipaikka = Some(Koodistokoodiviite("091", "kunta")),
  )

  lazy val suomenKieli: Koodistokoodiviite =
    Koodistokoodiviite("FI", "kieli")

  object Opiskeluoikeus {
    lazy val kesken: MuunKuinSäännellynKoulutuksenOpiskeluoikeus = MuunKuinSäännellynKoulutuksenOpiskeluoikeus(
      aikaleima = None,
      oppilaitos = Some(jatkuvaKoulutusOyOppilaitos),
      koulutustoimija = Some(jatkuvaKoulutusOyKoulutustoimija),
      tila = MuunKuinSäännellynKoulutuksenTila(List(
        OpiskeluoikeudenJakso.läsnä(LocalDate.of(2023, 1, 1)),
      )),
      suoritukset = List(PäätasonSuoritus.suoritusIlmanOsasuorituksia),
    )

    lazy val suoritettu: MuunKuinSäännellynKoulutuksenOpiskeluoikeus = MuunKuinSäännellynKoulutuksenOpiskeluoikeus(
      aikaleima = None,
      oppilaitos = Some(jatkuvaKoulutusOyOppilaitos),
      tila = MuunKuinSäännellynKoulutuksenTila(List(
        OpiskeluoikeudenJakso.läsnä(LocalDate.of(2023, 1, 1)),
        OpiskeluoikeudenJakso.suoritettu(LocalDate.of(2023, 2, 1)),
      )),
      suoritukset = List(PäätasonSuoritus.suoritusIlmanOsasuorituksia),
    )

    object OpiskeluoikeudenJakso {
      def läsnä(alku: LocalDate): MuunKuinSäännellynKoulutuksenOpiskeluoikeudenJakso = MuunKuinSäännellynKoulutuksenOpiskeluoikeudenJakso(
        tila = opiskeluoikeusLäsnä,
        alku = alku,
        opintojenRahoitus = Some(Koodistokoodiviite("14", "opintojenrahoitus")),
      )

      def suoritettu(alku: LocalDate): MuunKuinSäännellynKoulutuksenOpiskeluoikeudenJakso = MuunKuinSäännellynKoulutuksenOpiskeluoikeudenJakso(
        tila = opiskeluoikeusHyväksytystiSuoritettu,
        alku = alku,
        opintojenRahoitus = Some(Koodistokoodiviite("14", "opintojenrahoitus")),
      )
    }
  }

  object PäätasonSuoritus {
    lazy val suoritusIlmanOsasuorituksia: MuunKuinSäännellynKoulutuksenPäätasonSuoritus = MuunKuinSäännellynKoulutuksenPäätasonSuoritus(
      koulutusmoduuli = Koulutusmoduuli.kuvallinenIlmaisu,
      toimipiste = jatkuvaKoulutusOyOppilaitos,
      suorituskieli = suomenKieli,
    )

    lazy val suoritusOsasuorituksilla: MuunKuinSäännellynKoulutuksenPäätasonSuoritus = MuunKuinSäännellynKoulutuksenPäätasonSuoritus(
      koulutusmoduuli = Koulutusmoduuli.kuvallinenIlmaisu,
      toimipiste = jatkuvaKoulutusOyOppilaitos,
      suorituskieli = suomenKieli,
      osasuoritukset = Some(List(

      ))
    )

    object Koulutusmoduuli {
      lazy val kuvallinenIlmaisu: MuuKuinSäänneltyKoulutus = MuuKuinSäänneltyKoulutus(
        opintokokonaisuus = VapaaSivistystyöExample.exampleOpintokokonaisuus
      )
    }

    object Osasuoritus {
      def maalaus(laajuus: Double, arviointiPvm: LocalDate): MuunKuinSäännellynKoulutuksenOsasuoritus = MuunKuinSäännellynKoulutuksenOsasuoritus(
        koulutusmoduuli = Koulutusmoduuli.maalaus(laajuus),
        arviointi = Some(List(Arviointi.hyväksytty(arviointiPvm))),
      )

      object Koulutusmoduuli {
        def maalaus(laajuus: Double): MuunKuinSäännellynKoulutuksenOsasuorituksenKoulutusmoduuli = MuunKuinSäännellynKoulutuksenOsasuorituksenKoulutusmoduuli(
          kuvaus = finnish("Maalaus"),
          tunniste = PaikallinenKoodi(
            koodiarvo = "Maalaus",
            nimi = finnish("Maalaus")
          ),
          laajuus = LaajuusTunneissa(laajuus),
        )
      }

      object Arviointi {
        def hyväksytty(pvm: LocalDate): MuunKuinSäännellynKoulutuksenArviointi = MuunKuinSäännellynKoulutuksenArviointi(
          arvosana = Koodistokoodiviite("hyvaksytty", "arviointiasteikkomuks"),
          arviointipäivä = Some(pvm),
        )
      }
    }
  }
}
