package fi.oph.koski.raportit

import java.time.LocalDate

import fi.oph.koski.KoskiApplicationForTests
import fi.oph.koski.fixture.PaallekkaisetOpiskeluoikeudetFixtures.{keskimmaisenAlkamispaiva, ensimmaisenAlkamispaiva}
import fi.oph.koski.henkilo.MockOppijat
import fi.oph.koski.koskiuser.MockUsers
import fi.oph.koski.log.AuditLogTester
import fi.oph.koski.organisaatio.MockOrganisaatiot
import fi.oph.koski.raportointikanta.RaportointikantaTestMethods
import org.scalatest.{BeforeAndAfterAll, FreeSpec}

class PaallekkaisetOpiskeluoikeudetSpec extends FreeSpec with RaportointikantaTestMethods with BeforeAndAfterAll {

  override def defaultUser = MockUsers.helsinginKaupunkiPalvelukäyttäjä
  override def beforeAll() = loadRaportointikantaFixtures

  lazy val helsinginRaportti = loadRaportti(MockOrganisaatiot.helsinginKaupunki)
  lazy val stadinRaportti = loadRaportti(MockOrganisaatiot.stadinAmmattiopisto)
  lazy val keskuksenRaportti = loadRaportti(MockOrganisaatiot.stadinOppisopimuskeskus)

  "Päällekkäisten opiskeluoikeuksien raportti" - {
    "Lataus onnistuu ja tuottaa auditlogin" in {
      AuditLogTester.clearMessages
      authGet(s"api/raportit/paallekkaisetopiskeluoikeudet?oppilaitosOid=${MockOrganisaatiot.helsinginKaupunki}&alku=2018-01-01&loppu=2020-01-01&password=salasana") {
        verifyResponseStatusOk()
        response.headers("Content-Disposition").head should equal(
          s"""attachment; filename="paallekkaiset_opiskeluoikeudet_${MockOrganisaatiot.helsinginKaupunki}_2018-01-01_2020-01-01.xlsx""""
        )
        response.bodyBytes.take(ENCRYPTED_XLSX_PREFIX.length) should equal(ENCRYPTED_XLSX_PREFIX)
        AuditLogTester.verifyAuditLogMessage(Map(
          "operation" -> "OPISKELUOIKEUS_RAPORTTI",
          "target" -> Map("hakuEhto" -> s"raportti=paallekkaisetopiskeluoikeudet&oppilaitosOid=${MockOrganisaatiot.helsinginKaupunki}&alku=2018-01-01&loppu=2020-01-01")
        ))
      }
    }

    "Pekalla on 3 opiskeluoikeutta, keskimmäinen opiskeluoikeus on päällekkäinen ensimmäisen ja viimeisin opiskeluoikeuden kanssa" - {
      "Raportti koko koulutustoimijalta, kahdella opiskeluoikeudella on sama koulutustoimija Helsingin kaupunki" - {
        "Keskimmäinen opiskeluoikeus on kahdella rivillä, koska se on päällekkäinen kahden muun opiskeluoikeuden kanssa" in {
          pekanRivit(helsinginRaportti)
            .filter(_.alkamispaiva == keskimmaisenAlkamispaiva)
            .map(_.paallekkainenOppilaitosNimi) should contain theSameElementsAs(Seq("Omnia", "Stadin ammatti- ja aikuisopisto"))
        }
        "Ensimmäinen opiskeluoikeus on vain kerran, koska se on päällekkäinen vain keskimmäisen kanssa" in {
          pekanRivit(helsinginRaportti)
            .filter(_.alkamispaiva == ensimmaisenAlkamispaiva)
            .map(_.paallekkainenOppilaitosNimi) shouldBe(Seq("Stadin oppisopimuskeskus"))
        }
        "Pekka on yhteensä kolmella rivillä" in {
          pekanRivit(helsinginRaportti).length shouldBe(3)
        }
      }
      "Yhdestä oppilaitoksesta kerrallaan" in {
        pekanRivit(stadinRaportti).map(_.paallekkainenOppilaitosNimi) shouldBe(Seq("Stadin oppisopimuskeskus"))
        pekanRivit(keskuksenRaportti).map(_.paallekkainenOppilaitosNimi) should contain theSameElementsAs (Seq("Omnia", "Stadin ammatti- ja aikuisopisto"))
      }
    }

    "Päällekkäisen opiskeluoikeuden suorituksesta käytetään selkokielistä nimeä" in {
      pekanRivit(stadinRaportti).map(_.paallekkainenSuoritusTyyppi) shouldBe(Seq("Ammatillisen tutkinnon suoritus"))
      pekanRivit(keskuksenRaportti).map(withOppilaitos(_.paallekkainenSuoritusTyyppi)) should contain theSameElementsAs(Seq(
        ("Stadin ammatti- ja aikuisopisto","Näyttötutkintoon valmistavan koulutuksen suoritus"),
        ("Omnia","Ammatillisen tutkinnon suoritus")
      ))
    }
    "Päällekkäisen opiskeluoikeuden valtionosuusrahoitteiset jaksot" in {
      pekanRivit(stadinRaportti).map(_.paallekkaisellaVOSJaksoja) shouldBe(Seq(true))
      pekanRivit(stadinRaportti).map(_.paallekkaisellaVOSJaksojaParametrienSisalla) shouldBe(Seq(false))

      pekanRivit(keskuksenRaportti).map(withOppilaitos(_.paallekkaisellaVOSJaksoja)) should contain theSameElementsAs(Seq(
        ("Stadin ammatti- ja aikuisopisto", true),
        ("Omnia", true)
      ))
      pekanRivit(keskuksenRaportti).map(withOppilaitos(_.paallekkaisellaVOSJaksojaParametrienSisalla)) should contain theSameElementsAs(Seq(
        ("Stadin ammatti- ja aikuisopisto", true),
        ("Omnia", true)
      ))
    }
    "Päällekkäinen opiskeluoikeus alkanut aikasemmin" in {
      pekanRivit(stadinRaportti).map(_.paallekkainenAlkanutEka) shouldBe(Seq(false))
      pekanRivit(keskuksenRaportti).map(withOppilaitos(_.paallekkainenAlkanutEka)) should contain theSameElementsAs(Seq(
        ("Stadin ammatti- ja aikuisopisto", true),
        ("Omnia", false)
      ))
    }

    "Päällekkäisen opiskeluoikeuden sisältämistä suorituksista käytetävän nimi" - {
      "International school" - {
        "Yksikin 10-luokan MYP-suoritus tulkitaan lukion suoritukseksi, vaikka opiskeluoikeudella on useita alemman vuosiluokan suorituksia" in {
          val jsonb = """
          [
            ["internationalschoolmypvuosiluokka", "9"],
            ["internationalschoolmypvuosiluokka", "10"],
            ["internationalschoolmypvuosiluokka", "8"]
          ]
        """
          PaallekkaisetOpiskeluoikeudet.suorituksistaKaytettavaNimi(jsonb) shouldBe("International school lukio")
        }
        "Alemman vuosiluokan suoritukset tulkitaan perusopetukseksi" in {
          val jsonb = """
          [
            ["internationalschoolmypvuosiluokka", "9"],
            ["internationalschoolmypvuosiluokka", "8"]
          ]
        """
          PaallekkaisetOpiskeluoikeudet.suorituksistaKaytettavaNimi(jsonb) shouldBe("International school perusopetus")
        }
      }
      "Ammatillinen" - {
        "Jos opiskeluoikeudella on näyttötutkintoon valmistavan koulutuksen suoritus ja ammatillisen tutkinnon suoritus tai osa/osia-suoritus" in {
          val jsonb = """
          [
            ["ammatillinentutkinto", "381113"],
            ["nayttotutkintoonvalmistavakoulutus", "8718"]
          ]
        """
          PaallekkaisetOpiskeluoikeudet.suorituksistaKaytettavaNimi(jsonb) shouldBe("Ammatillisen tutkinnon suoritus")
        }
        "Jos pelkkä näyttötutkintoon valmistava" in {
          val jsonb = """
          [
            ["nayttotutkintoonvalmistavakoulutus", "8718"]
          ]
        """
          PaallekkaisetOpiskeluoikeudet.suorituksistaKaytettavaNimi(jsonb) shouldBe("Näyttötutkintoon valmistavan koulutuksen suoritus")
        }
      }
      "Muiden opiskeluoikeuksien suorituksista käytetään vain samaa nimeä riippumatta tyypistä" in {
        val jsonb = """
          [
            ["perusopetuksenoppimaara", "201101"],
            ["perusopetuksenvuosiluokka", "8"],
            ["perusopetuksenvuosiluokka", "7"],
            ["perusopetuksenvuosiluokka", "6"]
          ]
        """
        PaallekkaisetOpiskeluoikeudet.suorituksistaKaytettavaNimi(jsonb) shouldBe("Perusopetuksen oppimäärä")
      }
    }
  }

  private def withOppilaitos[T](f: PaallekkaisetOpiskeluoikeudetRow => T)(row: PaallekkaisetOpiskeluoikeudetRow): (String, T) = (row.paallekkainenOppilaitosNimi , f(row))

  private def pekanRivit(raportti:Seq[PaallekkaisetOpiskeluoikeudetRow]) =
    raportti.filter(_.oppijaOid == MockOppijat.paallekkaisiOpiskeluoikeuksia.oid)

  private def loadRaportti(oppilaitos: String) = {
    val request = AikajaksoRaporttiRequest(
      oppilaitosOid = oppilaitos,
      downloadToken = None,
      password = "password",
      alku = LocalDate.of(2020, 6, 30),
      loppu = LocalDate.of(2020, 11, 30)
    )

    new RaportitService(KoskiApplicationForTests)
      .paallekkaisetOpiskeluoikeudet(request)
      .sheets.head.asInstanceOf[DataSheet]
      .rows.asInstanceOf[Seq[PaallekkaisetOpiskeluoikeudetRow]]
  }
}