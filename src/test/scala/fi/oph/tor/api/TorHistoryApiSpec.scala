package fi.oph.tor.api

import java.time.LocalDate

import fi.oph.tor.db.OpiskeluOikeusHistoryRow
import fi.oph.tor.http.TorErrorCategory
import fi.oph.tor.jettylauncher.SharedJetty
import fi.oph.tor.json.Json
import fi.oph.tor.opiskeluoikeus.OpiskeluOikeusTestData
import fi.oph.tor.oppija.MockOppijat
import fi.oph.tor.organisaatio.MockOrganisaatiot
import fi.oph.tor.schema.{FullHenkilö, OpiskeluOikeus}
import fi.oph.tor.toruser.MockUsers
import org.scalatest.FunSpec

class TorHistoryApiSpec extends FunSpec with OpiskeluOikeusTestMethods {
  SharedJetty.start
  val uusiOpiskeluOikeus = OpiskeluOikeusTestData.opiskeluOikeus(MockOrganisaatiot.stadinAmmattiopisto, koulutusKoodi = 351161)
  val oppija: FullHenkilö = MockOppijat.tyhjä

  describe("Muutoshistoria") {
    describe("Luotaessa uusi opiskeluoikeus") {
      it("Luodaan historiarivi") {
        val opiskeluOikeus = createOpiskeluOikeus(oppija, uusiOpiskeluOikeus)
        verifyHistory(oppija, opiskeluOikeus, List(1))
      }
    }
    describe("Päivitettäessä") {
      it("Luodaan uusi versiorivi") {
        val opiskeluOikeus = createOpiskeluOikeus(oppija, uusiOpiskeluOikeus)
        val modified: OpiskeluOikeus = createOrUpdate(oppija, opiskeluOikeus.copy(päättymispäivä = Some(LocalDate.now)))
        verifyHistory(oppija, modified, List(1, 2))
      }

      describe("Jos mikään ei ole muuttunut") {
        it("Ei luoda uutta versioriviä") {
          val opiskeluOikeus = createOpiskeluOikeus(oppija, uusiOpiskeluOikeus)
          val modified: OpiskeluOikeus = createOrUpdate(oppija, opiskeluOikeus)
          verifyHistory(oppija, modified, List(1))
        }
      }

      describe("Kun syötteessä annetaan versionumero") {
        describe("Versionumero sama kuin viimeisin") {
          it("Päivitys hyväksytään") {
            val opiskeluOikeus = createOpiskeluOikeus(oppija, uusiOpiskeluOikeus)
            val modified: OpiskeluOikeus = createOrUpdate(oppija, opiskeluOikeus.copy(päättymispäivä = Some(LocalDate.now), versionumero = Some(1)))
            verifyHistory(oppija, modified, List(1, 2))
          }
        }

        describe("Versionumero ei täsmää") {
          it("Päivitys hylätään") {
            val opiskeluOikeus = createOpiskeluOikeus(oppija, uusiOpiskeluOikeus)
            val modified: OpiskeluOikeus = createOrUpdate(oppija, opiskeluOikeus.copy(päättymispäivä = Some(LocalDate.now), versionumero = Some(3)), {
              verifyResponseStatus(409)
            })
            verifyHistory(oppija, modified, List(1))
          }
        }
      }
    }

    describe("Käyttöoikeudet") {
      describe("Kun haetaan historiaa opiskeluoikeudelle, johon käyttäjällä ei oikeuksia") {
        it("Palautetaan 404") {
          val opiskeluOikeus = createOpiskeluOikeus(oppija, uusiOpiskeluOikeus)
          authGet("api/opiskeluoikeus/historia/" + opiskeluOikeus.id.get, MockUsers.hiiri) {
            verifyResponseStatus(404)
          }
        }
      }
    }

    describe("Virheellinen id") {
      it("Palautetaan HTTP 400") {
        authGet("api/opiskeluoikeus/historia/asdf") {
          verifyResponseStatus(400, TorErrorCategory.badRequest.format.number("Invalid id : asdf"))
        }
      }
    }
  }

  def verifyHistory(oppija: FullHenkilö, opiskeluOikeus: OpiskeluOikeus, versions: List[Int]): Unit = {
    authGet("api/opiskeluoikeus/historia/" + opiskeluOikeus.id.get) {
      verifyResponseStatus(200)
      val historia = Json.read[List[OpiskeluOikeusHistoryRow]](body)
      historia.map(_.versionumero) should equal(versions)

      markup("Validoidaan versiohistoria eheys")

      authGet("api/oppija/validate/" + oppija.oid) {
        verifyResponseStatus(200)
      }
    }
  }
}
