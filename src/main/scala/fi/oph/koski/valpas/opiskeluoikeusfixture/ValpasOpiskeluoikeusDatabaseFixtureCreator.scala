package fi.oph.koski.valpas.opiskeluoikeusfixture

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.fixture.DatabaseFixtureCreator
import fi.oph.koski.henkilo.OppijaHenkilö
import fi.oph.koski.schema._

class ValpasOpiskeluoikeusDatabaseFixtureCreator(application: KoskiApplication) extends DatabaseFixtureCreator(application, "opiskeluoikeus_valpas_fixture", "opiskeluoikeushistoria_valpas_fixture") {
  protected def oppijat = ValpasMockOppijat.defaultOppijat

  protected lazy val invalidOpiskeluoikeudet: List[(OppijaHenkilö, KoskeenTallennettavaOpiskeluoikeus)] = List()

  protected def defaultOpiskeluOikeudet: List[(OppijaHenkilö, KoskeenTallennettavaOpiskeluoikeus)] = List(
    (ValpasMockOppijat.oppivelvollinenYsiluokkaKeskenKeväällä2021, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.eiOppivelvollinenSyntynytEnnen2004, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.päällekkäisiäOpiskeluoikeuksia, ValpasOpiskeluoikeusExampleData.oppivelvollinenVaihtanutKouluaMuttaOpiskeluoikeusMerkkaamattaOikein1),
    (ValpasMockOppijat.päällekkäisiäOpiskeluoikeuksia, ValpasOpiskeluoikeusExampleData.oppivelvollinenVaihtanutKouluaMuttaOpiskeluoikeusMerkkaamattaOikein2),
    (ValpasMockOppijat.lukioOpiskelija, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeus),
    (ValpasMockOppijat.kasiluokkaKeskenKeväällä2021, ValpasOpiskeluoikeusExampleData.kasiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.kotiopetusMeneilläänOppija, ValpasOpiskeluoikeusExampleData.kotiopetusMeneilläänOpiskeluoikeus),
    (ValpasMockOppijat.kotiopetusMenneisyydessäOppija, ValpasOpiskeluoikeusExampleData.kotiopetusMenneisyydessäOpiskeluoikeus),
    (ValpasMockOppijat.eronnutOppija, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusTarkastelupäivääEnnen),
    (ValpasMockOppijat.eronnutOppijaTarkastelupäivänä, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusTarkastelupäivänä),
    (ValpasMockOppijat.eronnutOppijaTarkastelupäivänJälkeen, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusTarkastelupäivänJälkeen),
    (ValpasMockOppijat.valmistunutYsiluokkalainen, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.valmistunutYsiluokkalainenJollaIlmoitus, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.luokalleJäänytYsiluokkalainen, ValpasOpiskeluoikeusExampleData.luokallejäänytYsiluokkalainen),
    (ValpasMockOppijat.luokallejäänytYsiluokkalainenJollaUusiYsiluokka, ValpasOpiskeluoikeusExampleData.luokallejäänytYsiluokkalainenJollaUusiYsiluokka),
    (ValpasMockOppijat.luokalleJäänytYsiluokkalainenVaihtanutKoulua, ValpasOpiskeluoikeusExampleData.luokallejäänytYsiluokkalainenVaihtanutKouluaEdellinen),
    (ValpasMockOppijat.luokalleJäänytYsiluokkalainenVaihtanutKoulua, ValpasOpiskeluoikeusExampleData.luokallejäänytYsiluokkalainenVaihtanutKouluaJälkimmäinen),
    (ValpasMockOppijat.luokalleJäänytYsiluokkalainenVaihtanutKouluaMuualta, ValpasOpiskeluoikeusExampleData.luokallejäänytYsiluokkalainenVaihtanutKouluaEdellinen2),
    (ValpasMockOppijat.luokalleJäänytYsiluokkalainenVaihtanutKouluaMuualta, ValpasOpiskeluoikeusExampleData.luokallejäänytYsiluokkalainenVaihtanutKouluaJälkimmäinen2),
    (ValpasMockOppijat.kasiinAstiToisessaKoulussaOllut, ValpasOpiskeluoikeusExampleData.kasiluokkaEronnutKeväällä2020Opiskeluoikeus),
    (ValpasMockOppijat.kasiinAstiToisessaKoulussaOllut, ValpasOpiskeluoikeusExampleData.pelkkäYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.kasiinAstiToisessaKoulussaOllutJollaIlmoitus, ValpasOpiskeluoikeusExampleData.kasiluokkaEronnutKeväällä2020Opiskeluoikeus),
    (ValpasMockOppijat.kasiinAstiToisessaKoulussaOllutJollaIlmoitus, ValpasOpiskeluoikeusExampleData.pelkkäYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.lukionAloittanut, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.lukionAloittanut, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusAlkaa2021Syksyllä()),
    (ValpasMockOppijat.lukionLokakuussaAloittanut, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.lukionLokakuussaAloittanut, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusAlkaa2021Lokakuussa()),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaMaster, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaToinen, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeus),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaKolmas, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenToinenKoulu),
    (ValpasMockOppijat.aapajoenPeruskoulustaValmistunut, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenToinenKoulu),
    (ValpasMockOppijat.ennenLainRajapäivääPeruskoulustaValmistunut, ValpasOpiskeluoikeusExampleData.ennenLainRajapäivääToisestaKoulustaValmistunutYsiluokkalainen),
    (ValpasMockOppijat.ennenLainRajapäivääPeruskoulustaValmistunut, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.yli2kkAiemminPeruskoulustaValmistunut, ValpasOpiskeluoikeusExampleData.yli2kkAiemminPeruskoulustaValmistunut),
    (ValpasMockOppijat.oppivelvollinenAloittanutJaEronnutTarkastelupäivänJälkeen, ValpasOpiskeluoikeusExampleData.oppivelvollinenAloittanutJaEronnutTarkastelupäivänJälkeenOpiskeluoikeus),
    (ValpasMockOppijat.useampiYsiluokkaSamassaKoulussa, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.useampiYsiluokkaSamassaKoulussa, ValpasOpiskeluoikeusExampleData.kesäYsiluokkaKesken), // Tämä on vähän huono esimerkki, mutta varmistelee sitä, että homma toimii myös sitten, kun aletaan tukea nivelvaihetta, jossa nämä tapaukset voivat olla yleisempiä
    (ValpasMockOppijat.turvakieltoOppija, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.hakukohteidenHakuEpäonnistuu, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.kulosaarenYsiluokkalainen, ValpasOpiskeluoikeusExampleData.kulosaarelainenYsiluokkalainenOpiskeluoikeus),
    (ValpasMockOppijat.kulosaarenYsiluokkalainenJaJyväskylänLukiolainen, ValpasOpiskeluoikeusExampleData.kulosaarelainenYsiluokkalainenOpiskeluoikeus),
    (ValpasMockOppijat.kulosaarenYsiluokkalainenJaJyväskylänLukiolainen, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeus),
    (ValpasMockOppijat.kulosaarenYsiluokkalainenJaJyväskylänNivelvaiheinen, ValpasOpiskeluoikeusExampleData.kulosaarelainenYsiluokkalainenOpiskeluoikeus),
    (ValpasMockOppijat.kulosaarenYsiluokkalainenJaJyväskylänNivelvaiheinen, ValpasOpiskeluoikeusExampleData.kymppiluokanOpiskeluoikeus),
    (ValpasMockOppijat.kulosaarenYsiluokkalainenJaJyväskylänEsikoululainen, ValpasOpiskeluoikeusExampleData.kulosaarelainenYsiluokkalainenOpiskeluoikeus),
    (ValpasMockOppijat.kulosaarenYsiluokkalainenJaJyväskylänEsikoululainen, ValpasOpiskeluoikeusExampleData.esiopetuksenOpiskeluoikeus),
    (ValpasMockOppijat.kahdenKoulunYsiluokkalainenJollaIlmoitus, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.kahdenKoulunYsiluokkalainenJollaIlmoitus, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenToinenKoulu),
    (ValpasMockOppijat.lukionAineopinnotAloittanut, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.lukionAineopinnotAloittanut, ValpasOpiskeluoikeusExampleData.lukionAineopintojenOpiskeluoikeusAlkaa2021Syksyllä(None)),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaJollaIlmoitusMaster, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaJollaIlmoitusToinen, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeus),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaJollaIlmoitusKolmas, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenToinenKoulu),
    (ValpasMockOppijat.lukionAloittanutJollaVanhaIlmoitus, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.lukionAloittanutJollaVanhaIlmoitus, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusAlkaa2021Syksyllä()),
    (ValpasMockOppijat.lukionAloittanutJaLopettanutJollaIlmoituksia, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.lukionAloittanutJaLopettanutJollaIlmoituksia, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusAlkaaJaLoppuu2021Syksyllä()),
    (ValpasMockOppijat.ammattikoulustaValmistunutOpiskelija, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.ammattikoulustaValmistunutOpiskelija, ValpasOpiskeluoikeusExampleData.ammattikouluValmistunutOpiskeluoikeus),
    (ValpasMockOppijat.eronnutMaaliskuussa17VuottaTäyttäväKasiluokkalainen, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusEiYsiluokkaaKeväänAlussa),
    (ValpasMockOppijat.eronnutKeväänValmistumisJaksolla17VuottaTäyttäväKasiluokkalainen, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusEiYsiluokkaaKeväänJaksolla),
    (ValpasMockOppijat.eronnutElokuussa17VuottaTäyttäväKasiluokkalainen, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusEiYsiluokkaaElokuussa),
    (ValpasMockOppijat.valmistunutYsiluokkalainenVsop, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenVsop),
    (ValpasMockOppijat.ysiluokkaKeskenVsop, ValpasOpiskeluoikeusExampleData.ysiluokkaKeskenVsop),
    (ValpasMockOppijat.valmistunutKasiluokkalainen, ValpasOpiskeluoikeusExampleData.valmistunutKasiluokkalainen),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaJollaIlmoitusMaster2, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeus),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaJollaIlmoitusToinen2, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.ilmoituksenLisätiedotPoistettu, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.lukiostaValmistunutOpiskelija, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusValmistunut),
    (ValpasMockOppijat.ammattikouluOpiskelija, ValpasOpiskeluoikeusExampleData.ammattikouluOpiskeluoikeus),
    (ValpasMockOppijat.ammattikouluOpiskelijaValma, ValpasOpiskeluoikeusExampleData.ammattikouluValmaOpiskeluoikeus),
    (ValpasMockOppijat.ammattikouluOpiskelijaTelma, ValpasOpiskeluoikeusExampleData.ammattikouluTelmaOpiskeluoikeus),
    (ValpasMockOppijat.kaksoistutkinnostaValmistunutOpiskelija, ValpasOpiskeluoikeusExampleData.ammattikouluValmistunutOpiskeluoikeus),
    (ValpasMockOppijat.kaksoistutkinnostaValmistunutOpiskelija, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusValmistunut),
    (ValpasMockOppijat.nivelvaiheestaValmistunutOpiskelija, ValpasOpiskeluoikeusExampleData.valmistunutKymppiluokkalainen),
    (ValpasMockOppijat.oppivelvollisuusKeskeytetty, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.oppivelvollisuusKeskeytettyToistaiseksi, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.eiOppivelvollisuudenSuorittamiseenKelpaaviaOpiskeluoikeuksia, ValpasOpiskeluoikeusExampleData.esiopetuksenOpiskeluoikeus),
    (ValpasMockOppijat.hetuton, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.oppivelvollinenJollaHetu, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.oppivelvollinenJollaHetuHetutonSlave, ValpasOpiskeluoikeusExampleData.kulosaarelainenYsiluokkalainenOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutEiUuttaOpiskeluoikeutta, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKoulu),
    (ValpasMockOppijat.amisEronnutEiUuttaOpiskeluoikeutta, ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusTulevaisuudessaKeskeyttänyt, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKoulu),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusTulevaisuudessaKeskeyttänyt, ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusTulevaisuudessaKeskeyttänyt, ValpasOpiskeluoikeusExampleData.ammattikouluAlkaaOmniaLoka2021),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusVoimassa, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKoulu),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusVoimassa, ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusVoimassa, ValpasOpiskeluoikeusExampleData.ammattikouluAlkaaOmniaSyys2021),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusPeruskoulussaKeskeyttänytTulevaisuudessa, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKoulu),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusPeruskoulussaKeskeyttänytTulevaisuudessa, ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusPeruskoulussaKeskeyttänytTulevaisuudessa, ValpasOpiskeluoikeusExampleData.alkaaYsiluokkalainenSaksalainenKouluSyys2021),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusNivelvaiheessa, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKoulu),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusNivelvaiheessa, ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusNivelvaiheessa, ValpasOpiskeluoikeusExampleData.kymppiluokkaAlkaaSyys2021),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusNivelvaiheessa2, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKoulu),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusNivelvaiheessa2, ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusNivelvaiheessa2, ValpasOpiskeluoikeusExampleData.valmaOpiskeluoikeusAlkaaOmniassaSyys2021),
    (ValpasMockOppijat.amisEronnutMontaUuttaOpiskeluoikeutta, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKoulu),
    (ValpasMockOppijat.amisEronnutMontaUuttaOpiskeluoikeutta, ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutMontaUuttaOpiskeluoikeutta, ValpasOpiskeluoikeusExampleData.kymppiluokkaAlkaaSyys2021),
    (ValpasMockOppijat.amisEronnutMontaUuttaOpiskeluoikeutta, ValpasOpiskeluoikeusExampleData.ammattikouluAlkaaOmniaSyys2021),
    (ValpasMockOppijat.amisEronnutUusiKelpaamatonOpiskeluoikeusNivelvaiheessa, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKoulu),
    (ValpasMockOppijat.amisEronnutUusiKelpaamatonOpiskeluoikeusNivelvaiheessa, ValpasOpiskeluoikeusExampleData.valmistunutKymppiluokkalainen),
    (ValpasMockOppijat.amisEronnutUusiKelpaamatonOpiskeluoikeusNivelvaiheessa, ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutUusiKelpaamatonOpiskeluoikeusNivelvaiheessa, ValpasOpiskeluoikeusExampleData.kymppiluokkaAlkaaSyys2021),
    (ValpasMockOppijat.amisEronnutUusiKelpaamatonOpiskeluoikeusNivelvaiheessa2, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKoulu),
    (ValpasMockOppijat.amisEronnutUusiKelpaamatonOpiskeluoikeusNivelvaiheessa2, ValpasOpiskeluoikeusExampleData.valmistunutKymppiluokkalainen),
    (ValpasMockOppijat.amisEronnutUusiKelpaamatonOpiskeluoikeusNivelvaiheessa2, ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutUusiKelpaamatonOpiskeluoikeusNivelvaiheessa2, ValpasOpiskeluoikeusExampleData.vstAlkaaSyys2021),
    (ValpasMockOppijat.ammattikouluOpiskelijaMontaOpiskeluoikeutta, ValpasOpiskeluoikeusExampleData.ammattikouluValmaOpiskeluoikeus),
    (ValpasMockOppijat.ammattikouluOpiskelijaMontaOpiskeluoikeutta, ValpasOpiskeluoikeusExampleData.ammattikouluOpiskeluoikeus),
    (ValpasMockOppijat.amisAmmatillinenJaNäyttötutkintoonValmistava, ValpasOpiskeluoikeusExampleData.amisAmmatillinenJaNäyttötutkintoonValmistavaOpiskeluoikeus),
  )
}
