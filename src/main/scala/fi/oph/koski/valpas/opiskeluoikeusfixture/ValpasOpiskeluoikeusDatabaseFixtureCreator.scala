package fi.oph.koski.valpas.opiskeluoikeusfixture

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.fixture.DatabaseFixtureCreator
import fi.oph.koski.henkilo.OppijaHenkilö
import fi.oph.koski.schema._

import java.time.LocalDate

class ValpasOpiskeluoikeusDatabaseFixtureCreator(application: KoskiApplication) extends DatabaseFixtureCreator(application, "opiskeluoikeus_valpas_fixture", "opiskeluoikeushistoria_valpas_fixture") {
  protected def oppijat = ValpasMockOppijat.defaultOppijat

  protected lazy val invalidOpiskeluoikeudet: List[(OppijaHenkilö, KoskeenTallennettavaOpiskeluoikeus)] = List(
    (ValpasMockOppijat.intSchool10LuokallaIlmanAlkamispäivää, ValpasOpiskeluoikeusExampleData.intSchool9LuokaltaValmistunut2021ja10LuokallaIlmanAlkamispäivää),
    (ValpasMockOppijat.telmaJaAmis, ValpasOpiskeluoikeusExampleData.telmaJaAmisRessussa),
    (ValpasMockOppijat.oppivelvollinenYsiluokkaKeskenKeväällä2021Puuttuva7LuokanAlkamispäivä, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021OpiskeluoikeusPuuttuva7LuokanAlkamispäivä),
  )

  protected def defaultOpiskeluOikeudet: List[(OppijaHenkilö, KoskeenTallennettavaOpiskeluoikeus)] = List(
    (ValpasMockOppijat.oppivelvollinenYsiluokkaKeskenKeväällä2021, ValpasOpiskeluoikeusExampleData.esiopetusValmistunutOpiskeluoikeus),
    (ValpasMockOppijat.oppivelvollinenYsiluokkaKeskenKeväällä2021, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.oppivelvollinenIntSchoolYsiluokkaKeskenKeväällä2021, ValpasOpiskeluoikeusExampleData.oppivelvollinenIntSchoolYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.oppivelvollinenESHS5KeskenKeväällä2021, ValpasOpiskeluoikeusExampleData.oppivelvollinenESHS5KeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.eiOppivelvollinenSyntynytEnnen2004, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.päällekkäisiäOpiskeluoikeuksia, ValpasOpiskeluoikeusExampleData.oppivelvollinenVaihtanutKouluaMuttaOpiskeluoikeusMerkkaamattaOikein1),
    (ValpasMockOppijat.päällekkäisiäOpiskeluoikeuksia, ValpasOpiskeluoikeusExampleData.oppivelvollinenVaihtanutKouluaMuttaOpiskeluoikeusMerkkaamattaOikein2),
    (ValpasMockOppijat.lukioOpiskelija, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeus),
    (ValpasMockOppijat.kasiluokkaKeskenKeväällä2021, ValpasOpiskeluoikeusExampleData.kasiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.intSchoolKasiluokkaKeskenKeväällä2021, ValpasOpiskeluoikeusExampleData.intSchoolKasiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.eshS4KeskenKeväällä2021, ValpasOpiskeluoikeusExampleData.eshKasiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.kotiopetusMeneilläänOppija, ValpasOpiskeluoikeusExampleData.kotiopetusMeneilläänOpiskeluoikeus),
    (ValpasMockOppijat.kotiopetusMeneilläänVanhallaRakenteellaOppija, ValpasOpiskeluoikeusExampleData.kotiopetusMeneilläänVanhallaRakenteellaOpiskeluoikeus),
    (ValpasMockOppijat.kotiopetusMenneisyydessäOppija, ValpasOpiskeluoikeusExampleData.kotiopetusMenneisyydessäOpiskeluoikeus),
    (ValpasMockOppijat.eronnutOppija, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusTarkastelupäivääEnnen),
    (ValpasMockOppijat.intSchool9LuokaltaKeskenEronnutOppija, ValpasOpiskeluoikeusExampleData.intSchool9LuokaltaKeskenEronnutOpiskeluoikeusTarkastelupäivääEnnen),
    (ValpasMockOppijat.intSchool9LuokaltaValmistumisenJälkeenEronnutOppija, ValpasOpiskeluoikeusExampleData.intSchool9LuokaltaValmistumisenJälkeenEronnutOpiskeluoikeusTarkastelupäivääEnnen),
    (ValpasMockOppijat.eronnutOppijaTarkastelupäivänä, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusTarkastelupäivänä),
    (ValpasMockOppijat.eronnutOppijaTarkastelupäivänJälkeen, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusTarkastelupäivänJälkeen),
    (ValpasMockOppijat.intSchool9LuokaltaKeskenEronnutOppijaTarkastelupäivänä, ValpasOpiskeluoikeusExampleData.intSchool9LuokaltaKeskenEronnutOpiskeluoikeusTarkastelupäivänä),
    (ValpasMockOppijat.intSchool9LuokaltaKeskenEronnutOppijaTarkastelupäivänJälkeen, ValpasOpiskeluoikeusExampleData.intSchool9LuokaltaKeskenEronnutOpiskeluoikeusTarkastelupäivänJälkeen),
    (ValpasMockOppijat.intSchool9LuokaltaValmistumisenJälkeenEronnutOppijaTarkastelupäivänä, ValpasOpiskeluoikeusExampleData.intSchool9LuokaltaValmistumisenJälkeenEronnutOpiskeluoikeusTarkastelupäivänä),
    (ValpasMockOppijat.intSchool9LuokaltaValmistumisenJälkeenEronnutOppijaTarkastelupäivänJälkeen, ValpasOpiskeluoikeusExampleData.intSchool9LuokaltaValmistumisenJälkeenEronnutOpiskeluoikeusTarkastelupäivänJälkeen),
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
    (ValpasMockOppijat.intSchool9LuokanJälkeenLukionAloittanut, ValpasOpiskeluoikeusExampleData.internationalSchool9LuokaltaValmistunut2021),
    (ValpasMockOppijat.intSchool9LuokanJälkeenLukionAloittanut, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusAlkaa2021Syksyllä()),
    (ValpasMockOppijat.intSchool9LuokanJälkeenIntSchoolin10LuokallaAloittanut, ValpasOpiskeluoikeusExampleData.intSchool9LuokaltaValmistunut2021ja10LuokallaAloittanut),
    (ValpasMockOppijat.eshS4JälkeenS5Aloittanut, ValpasOpiskeluoikeusExampleData.eshS4S5Opiskeluoikeus),
    (ValpasMockOppijat.eshS5JälkeenLukiossaAloittanut, ValpasOpiskeluoikeusExampleData.oppivelvollinenESHS5ValmisKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.eshS5JälkeenLukiossaAloittanut, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusAlkaa2021Syksyllä(None)),
    (ValpasMockOppijat.intSchoolLokakuussaPerusopetuksenSuorittanut, ValpasOpiskeluoikeusExampleData.intSchool9LuokaltaValmistunutLokakuussa2021ja10LuokallaAloittanut),
    (ValpasMockOppijat.lukionLokakuussaAloittanut, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.lukionLokakuussaAloittanut, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusAlkaa2021Lokakuussa()),
    (ValpasMockOppijat.intSchool9LuokanJälkeenLukionLokakuussaAloittanut, ValpasOpiskeluoikeusExampleData.internationalSchool9LuokaltaValmistunut2021),
    (ValpasMockOppijat.intSchool9LuokanJälkeenLukionLokakuussaAloittanut, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusAlkaa2021Lokakuussa()),
    (ValpasMockOppijat.intSchool9LuokanJälkeenIntSchoolin10LuokallaLokakuussaAloittanut, ValpasOpiskeluoikeusExampleData.intSchool9LuokaltaValmistunut2021ja10LuokallaLokakuussaAloittanut),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaMaster, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaToinen, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeus),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaKolmas, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenToinenKoulu),
    (ValpasMockOppijat.aapajoenPeruskoulustaValmistunut, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenToinenKoulu),
    (ValpasMockOppijat.ennenLainRajapäivääPeruskoulustaValmistunut, ValpasOpiskeluoikeusExampleData.ennenLainRajapäivääToisestaKoulustaValmistunutYsiluokkalainen),
    (ValpasMockOppijat.ennenLainRajapäivääPeruskoulustaValmistunut, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.yli2kkAiemminPeruskoulustaValmistunut, ValpasOpiskeluoikeusExampleData.yli2kkAiemminPeruskoulustaValmistunut),
    (ValpasMockOppijat.intSchoolin9LuokaltaYli2kkAiemminValmistunut, ValpasOpiskeluoikeusExampleData.yli2kkAiemminIntSchoolin9LuokaltaValmistunut),
    (ValpasMockOppijat.intSchoolin9LuokaltaYli2kkAiemminValmistunut10Jatkanut, ValpasOpiskeluoikeusExampleData.yli2kkAiemminIntSchoolin9LuokaltaValmistunut10Jatkanut),
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
    (ValpasMockOppijat.kulosaarenYsiluokkalainenJaJyväskylänEsikoululainen, ValpasOpiskeluoikeusExampleData.esiopetusValmistunutOpiskeluoikeus),
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
    (ValpasMockOppijat.ammattikoulustaValmistunutOpiskelija, ValpasOpiskeluoikeusExampleData.ammattikouluValmistunutOpiskeluoikeus()),
    (ValpasMockOppijat.eronnutMaaliskuussa17VuottaTäyttäväKasiluokkalainen, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusEiYsiluokkaaKeväänAlussa),
    (ValpasMockOppijat.eronnutKeväänValmistumisJaksolla17VuottaTäyttäväKasiluokkalainen, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusEiYsiluokkaaKeväänJaksolla),
    (ValpasMockOppijat.eronnutElokuussa17VuottaTäyttäväKasiluokkalainen, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusEiYsiluokkaaElokuussa),
    (ValpasMockOppijat.läsnä17VuottaTäyttäväKasiluokkalainen, ValpasOpiskeluoikeusExampleData.kasiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.keskeyttänyt17VuottaTäyttäväKasiluokkalainen, ValpasOpiskeluoikeusExampleData.kasiluokkaKeskeytetty2021Opiskeluoikeus),
    (ValpasMockOppijat.keväänUlkopuolellaValmistunut17v, ValpasOpiskeluoikeusExampleData.keväänUlkopuolellaValmistunutYsiluokkalainen),
    (ValpasMockOppijat.keväänUlkopuolellaEronnut17v, ValpasOpiskeluoikeusExampleData.keväänUlkopuolellaEronnutYsiluokkalainen),
    (ValpasMockOppijat.intSchoolistaEronnutMaaliskuussa17VuottaTäyttäväKasiluokkalainen, ValpasOpiskeluoikeusExampleData.intSchoolistaEronnutOpiskeluoikeusEiYsiluokkaaKeväänAlussa),
    (ValpasMockOppijat.intSchoolistaEronnutElokuussa17VuottaTäyttäväKasiluokkalainen, ValpasOpiskeluoikeusExampleData.intSchoolistaEronnutOpiskeluoikeusEiYsiluokkaaElokuussa),
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
    (ValpasMockOppijat.kolmoistutkinnostaValmistunutOpiskelija, ValpasOpiskeluoikeusExampleData.ammattikouluValmistunutOpiskeluoikeus()),
    (ValpasMockOppijat.kolmoistutkinnostaValmistunutOpiskelija, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusValmistunut),
    (ValpasMockOppijat.nivelvaiheestaValmistunutOpiskelija, ValpasOpiskeluoikeusExampleData.valmistunutKymppiluokkalainen),
    (ValpasMockOppijat.oppivelvollisuusKeskeytetty, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.oppivelvollisuusKeskeytettyToistaiseksi, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.eiOppivelvollisuudenSuorittamiseenYksinäänKelpaaviaOpiskeluoikeuksia, ValpasOpiskeluoikeusExampleData.lukionAineopintojenOpiskeluoikeusAlkaa2021Syksyllä(None)),
    (ValpasMockOppijat.hetuton, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.oppivelvollinenJollaHetu, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.oppivelvollinenJollaHetuHetutonSlave, ValpasOpiskeluoikeusExampleData.kulosaarelainenYsiluokkalainenOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutEiUuttaOpiskeluoikeutta, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKouluVäliaikaisestiKeskeytynytToukokuussa),
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
    (ValpasMockOppijat.lukioVäliaikaisestiKeskeytynyt, ValpasOpiskeluoikeusExampleData.lukionVäliaikaisestiKeskeytettyOpiskeluoikeus),
    (ValpasMockOppijat.amisLomalla, ValpasOpiskeluoikeusExampleData.ammattikouluLomallaOpiskeluoikeus),
    (ValpasMockOppijat.internationalSchoolista9LuokaltaEnnen2021Valmistunut, ValpasOpiskeluoikeusExampleData.internationalSchool9LuokaltaValmistunut2020),
    (ValpasMockOppijat.internationalSchoolista9Luokalta2021Valmistunut, ValpasOpiskeluoikeusExampleData.internationalSchool9LuokaltaValmistunut2021),
    (ValpasMockOppijat.peruskoulustaValmistunutIlman9Luokkaa, ValpasOpiskeluoikeusExampleData.valmistunutIlmanYsiluokkaa),
    (ValpasMockOppijat.peruskoulustaLokakuussaValmistunutIlman9Luokkaa, ValpasOpiskeluoikeusExampleData.valmistunutLokakuussaIlmanYsiluokkaa),
    (ValpasMockOppijat.lukioVanhallaOpsilla, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.lukioVanhallaOpsilla, ValpasOpiskeluoikeusExampleData.lukionVanhanOpsinOpiskeluoikeusAlkaa2021Keväällä()),
    (ValpasMockOppijat.muuttanutUlkomaille, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.turvakieltoOppijaTyhjälläKotikunnalla, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.intSchool10LuokaltaAloittanut, ValpasOpiskeluoikeusExampleData.internationalSchool10LuokaltaAloittanut),
    (ValpasMockOppijat.intSchool11LuokaltaAloittanut, ValpasOpiskeluoikeusExampleData.internationalSchool11LuokaltaAloittanut),
    (ValpasMockOppijat.intSchool8LuokanSyksyllä2021Aloittanut, ValpasOpiskeluoikeusExampleData.internationalSchool8LuokanSyksyllä2021Aloittanut),
    (ValpasMockOppijat.intSchool9LuokanSyksyllä2021Aloittanut, ValpasOpiskeluoikeusExampleData.internationalSchool9LuokanSyksyllä2021Aloittanut),
    (ValpasMockOppijat.aikuistenPerusopetuksessa, ValpasOpiskeluoikeusExampleData.aikuistenPerusopetuksessa),
    (ValpasMockOppijat.aikuistenPerusopetuksessaSyksynRajapäivänJälkeenAloittava, ValpasOpiskeluoikeusExampleData.aikuistenPerusopetuksessaSyksynRajapäivänJälkeenAloittava),
    (ValpasMockOppijat.aikuistenPerusopetuksessaPeruskoulustaValmistunut, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenRessunLukio),
    (ValpasMockOppijat.aikuistenPerusopetuksessaPeruskoulustaValmistunut, ValpasOpiskeluoikeusExampleData.aikuistenPerusopetuksessa),
    (ValpasMockOppijat.aikuistenPerusopetuksestaKeväänValmistujaksollaValmistunut, ValpasOpiskeluoikeusExampleData.aikuistenPerusopetuksestaKeväänValmistujaksollaValmistunut),
    (ValpasMockOppijat.aikuistenPerusopetuksestaEronnut, ValpasOpiskeluoikeusExampleData.aikuistenPerusopetuksestaEronnut),
    (ValpasMockOppijat.aikuistenPerusopetuksestaYli2kkAiemminValmistunut, ValpasOpiskeluoikeusExampleData.aikuistenPerusopetuksestaYli2kkAiemminValmistunut),
    (ValpasMockOppijat.aikuistenPerusopetuksestaAlle2kkAiemminValmistunut, ValpasOpiskeluoikeusExampleData.aikuistenPerusopetuksestaAlle2kkAiemminValmistunut),
    (ValpasMockOppijat.aikuistenPerusopetuksestaTulevaisuudessaValmistuva, ValpasOpiskeluoikeusExampleData.aikuistenPerusopetuksestaTulevaisuudessaValmistuva),
    (ValpasMockOppijat.aikuistenPerusopetuksestaLähitulevaisuudessaValmistuva, ValpasOpiskeluoikeusExampleData.aikuistenPerusopetuksestaLähitulevaisuudessaValmistuva),
    (ValpasMockOppijat.aikuistenPerusopetuksessaAineopiskelija, ValpasOpiskeluoikeusExampleData.aikuistenPerusopetuksessaAineopiskelija),
    (ValpasMockOppijat.luva, ValpasOpiskeluoikeusExampleData.luva),
    (ValpasMockOppijat.kymppiluokka, ValpasOpiskeluoikeusExampleData.kymppiluokkaRessussa),
    (ValpasMockOppijat.vstKops, ValpasOpiskeluoikeusExampleData.vstKopsRessussa),
    (ValpasMockOppijat.valma, ValpasOpiskeluoikeusExampleData.valmaRessussa),
    (ValpasMockOppijat.telma, ValpasOpiskeluoikeusExampleData.telmaRessussa),
    (ValpasMockOppijat.kaksiToisenAsteenOpiskelua, ValpasOpiskeluoikeusExampleData.valmaRessussa),
    (ValpasMockOppijat.kaksiToisenAsteenOpiskelua, ValpasOpiskeluoikeusExampleData.ammattikouluValmaOpiskeluoikeus),
    (ValpasMockOppijat.esikoululainen, ValpasOpiskeluoikeusExampleData.esiopetusLäsnäOpiskeluoikeus),
    (ValpasMockOppijat.maksuttomuuttaPidennetty, ValpasOpiskeluoikeusExampleData.ammattikouluMaksuttomuuttaPidennetty),
    (ValpasMockOppijat.oppivelvollisuusKeskeytettyEiOpiskele, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenToukokuun15),
    (ValpasMockOppijat.perusopetukseenValmistautuva, ValpasOpiskeluoikeusExampleData.perusopetukseenValmistavanOpetuksenOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutUusiKelpaamatonOpiskeluoikeusPerusopetukseenValmistavassa, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKoulu),
    (ValpasMockOppijat.amisEronnutUusiKelpaamatonOpiskeluoikeusPerusopetukseenValmistavassa, ValpasOpiskeluoikeusExampleData.valmistunutKymppiluokkalainen),
    (ValpasMockOppijat.amisEronnutUusiKelpaamatonOpiskeluoikeusPerusopetukseenValmistavassa, ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutUusiKelpaamatonOpiskeluoikeusPerusopetukseenValmistavassa, ValpasOpiskeluoikeusExampleData.perusopetukseenValmistavanOpetuksenOpiskeluoikeusAlkaaSyys2021),
    (ValpasMockOppijat.casIntegraationTestaus, ValpasOpiskeluoikeusExampleData.esiopetusValmistunutOpiskeluoikeus),
    (ValpasMockOppijat.preIbAloitettu, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.preIbAloitettu, ValpasOpiskeluoikeusExampleData.ibOpiskeluoikeusPreIbSuoritus),
    (ValpasMockOppijat.perusopetukseenValmistautuva17VuottaTäyttävä, ValpasOpiskeluoikeusExampleData.perusopetukseenValmistavanOpetuksenOpiskeluoikeus),
    (ValpasMockOppijat.perusopetukseenValmistavastaValmistunut17Vuotias, ValpasOpiskeluoikeusExampleData.perusopetukseenValmistavanOpetuksenOpiskeluoikeusValmistunut),
    (ValpasMockOppijat.perusopetukseenValmistavastaEronnut17Vuotias, ValpasOpiskeluoikeusExampleData.perusopetukseenValmistavanOpetuksenOpiskeluoikeusEronnut),
    (ValpasMockOppijat.sureHautAinaEpäonnistuvaOppija, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021SaksalainenKouluOpiskeluoikeus),
    (ValpasMockOppijat.valmistunutNivelvaiheenOpiskelija2022, ValpasOpiskeluoikeusExampleData.valmistunutKymppiluokkalainenKeväällä2022),
    (ValpasMockOppijat.alkukesästäEronnutNivelvaiheenOpiskelija, ValpasOpiskeluoikeusExampleData.alkukesästäEronnutKymppiluokkalainen),
    (ValpasMockOppijat.alkukesästäEronneeksiKatsottuNivelvaiheenOpiskelija, ValpasOpiskeluoikeusExampleData.alkukesästäEronneeksiKatsottuKymppiluokkalainen),
    (ValpasMockOppijat.alkuvuodestaEronnutNivelvaiheenOpiskelija, ValpasOpiskeluoikeusExampleData.alkuvuodestaEronnutKymppiluokkalainen),
    (ValpasMockOppijat.alkuvuodestaEronneeksiKatsottuNivelvaiheenOpiskelija, ValpasOpiskeluoikeusExampleData.alkuvuodestaEronneeksiKatsottuKymppiluokkalainen),
    (ValpasMockOppijat.oppivelvollisuudestaVapautettu, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.amisEronnutTuvalainen, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKouluVäliaikaisestiKeskeytynytToukokuussa),
    (ValpasMockOppijat.amisEronnutTuvalainen, ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutTuvalainen, ValpasOpiskeluoikeusExampleData.tuvaOpiskeluoikeusKesken),
    (ValpasMockOppijat.valmistunutTuvalainen, ValpasOpiskeluoikeusExampleData.tuvaOpiskeluoikeusValmis),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusAlkanutEroamispäivänä, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKoulu),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusAlkanutEroamispäivänä, ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusAlkanutEroamispäivänä, ValpasOpiskeluoikeusExampleData.ammattikouluAlkaaOmnia(ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus.päättymispäivä.get)),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusAlkanutEroamispäivänäJaPäättynyt, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKoulu),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusAlkanutEroamispäivänäJaPäättynyt, ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusAlkanutEroamispäivänäJaPäättynyt, ValpasOpiskeluoikeusExampleData.ammattikouluAlkaaJaEroaaOmnia(
      ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus.päättymispäivä.get.plusDays(1),
      ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus.päättymispäivä.get.plusMonths(6))),
    (ValpasMockOppijat.amisEronnutUusiNivelvaiheOpiskeluoikeusAlkanutEroamispäivänäJaPäättynyt, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKoulu),
    (ValpasMockOppijat.amisEronnutUusiNivelvaiheOpiskeluoikeusAlkanutEroamispäivänäJaPäättynyt, ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutUusiNivelvaiheOpiskeluoikeusAlkanutEroamispäivänäJaPäättynyt, ValpasOpiskeluoikeusExampleData.valmaRessussaEronnut(
      ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus.päättymispäivä.get.plusDays(1),
      ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus.päättymispäivä.get.plusMonths(6))),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusAlkanutJaPäättynytEroonKeskellä, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKoulu),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusAlkanutJaPäättynytEroonKeskellä, ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus),
    (ValpasMockOppijat.amisEronnutUusiOpiskeluoikeusAlkanutJaPäättynytEroonKeskellä, ValpasOpiskeluoikeusExampleData.ammattikouluAlkaaJaEroaaOmnia(
      ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus.päättymispäivä.get.minusMonths(6),
      ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus.päättymispäivä.get.minusMonths(3))),
    (ValpasMockOppijat.valmistunutAmiksenOsittainen, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKoulu),
    (ValpasMockOppijat.valmistunutAmiksenOsittainen, ValpasOpiskeluoikeusExampleData.ammattikouluValmistunutOsittainenOpiskeluoikeus),
    (ValpasMockOppijat.valmistunutAmiksenOsittainenUusiOo, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenSaksalainenKoulu),
    (ValpasMockOppijat.valmistunutAmiksenOsittainenUusiOo, ValpasOpiskeluoikeusExampleData.ammattikouluValmistunutOsittainenOpiskeluoikeus),
    (ValpasMockOppijat.valmistunutAmiksenOsittainenUusiOo, ValpasOpiskeluoikeusExampleData.ammattikouluAlkaaOmnia(ValpasOpiskeluoikeusExampleData.ammattikouluValmistunutOsittainenOpiskeluoikeus.päättymispäivä.get)),
    (ValpasMockOppijat.valmistunutYsiluokkalainenJollaIlmoitusJaUusiOpiskeluoikeus, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.valmistunutYsiluokkalainenJollaIlmoitusJaUusiOpiskeluoikeus, ValpasOpiskeluoikeusExampleData.valmaRessussa),
    (ValpasMockOppijat.oppijaJollaYOOpiskeluoikeus, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenToinenKoulu),
    (ValpasMockOppijat.amisValmistunutEronnutValmasta, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.amisValmistunutEronnutValmasta, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusAlkaaJaLoppuu2021Syksyllä()),
    (ValpasMockOppijat.amisValmistunutEronnutValmasta, ValpasOpiskeluoikeusExampleData.ammattikouluValmaOpiskeluoikeusEronnut),
    (ValpasMockOppijat.amisValmistunutEronnutValmasta, ValpasOpiskeluoikeusExampleData.ammattikouluValmistunutOpiskeluoikeus(LocalDate.of(2022, 1, 10), LocalDate.of(2023, 4, 28))),
    (ValpasMockOppijat.eshNurseryssä, ValpasOpiskeluoikeusExampleData.eshNurseryssä),
    (ValpasMockOppijat.eshEbTutkinnonAloittanut, ValpasOpiskeluoikeusExampleData.eshS7Valmistunut),
    (ValpasMockOppijat.eshEbTutkinnostaValmistunut, ValpasOpiskeluoikeusExampleData.eshS7Valmistunut),
    (ValpasMockOppijat.eshEbTutkinnostaEronnut, ValpasOpiskeluoikeusExampleData.eshS7Valmistunut),
    (ValpasMockOppijat.eshKeskenEbTutkinnonAloittanut, ValpasOpiskeluoikeusExampleData.eshS7Kesken),
    (ValpasMockOppijat.lukionAineOpinnotJaAmmatillisia, ValpasOpiskeluoikeusExampleData.lukionAineopintojenOpiskeluoikeusAlkaa2021Syksyllä(None)),
    (ValpasMockOppijat.lukionAineOpinnotJaAmmatillisia, ValpasOpiskeluoikeusExampleData.ammattikouluOpiskeluoikeus),
    (ValpasMockOppijat.oppijaJollaAmisJaValmistunutYO, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenToinenKoulu),
    (ValpasMockOppijat.oppijaJollaAmisJaValmistunutYO, ValpasOpiskeluoikeusExampleData.ammattikouluEronnutOpiskeluoikeus),
    (ValpasMockOppijat.taiteenPerusopetusPäättynyt, ValpasOpiskeluoikeusExampleData.taiteenPerusopetusPäättynyt)
  )

  protected def secondBatchOpiskeluOikeudet: List[(OppijaHenkilö, KoskeenTallennettavaOpiskeluoikeus)] = List(
    (ValpasMockOppijat.eshEbTutkinnonAloittanut, ValpasOpiskeluoikeusExampleData.ebEBTutkinnonAloittanut),
    (ValpasMockOppijat.eshEbTutkinnostaValmistunut, ValpasOpiskeluoikeusExampleData.ebEBTutkinnostaValmistunut),
    (ValpasMockOppijat.eshEbTutkinnostaEronnut, ValpasOpiskeluoikeusExampleData.ebEBTutkinnostaEronnut),
    (ValpasMockOppijat.eshKeskenEbTutkinnonAloittanut, ValpasOpiskeluoikeusExampleData.ebEBTutkinnonAloittanut),
  )

  protected def thirdBatchPäivitettävätOpiskeluOikeudet: List[(OppijaHenkilö, KoskeenTallennettavaOpiskeluoikeus)] = List.empty
}
