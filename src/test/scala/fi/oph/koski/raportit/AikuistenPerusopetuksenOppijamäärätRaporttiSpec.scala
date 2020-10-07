package fi.oph.koski.raportit

import java.sql.Date.{valueOf => sqlDate}

import fi.oph.koski.KoskiApplicationForTests
import fi.oph.koski.koskiuser.MockUser
import fi.oph.koski.log.AuditLogTester
import fi.oph.koski.organisaatio.MockOrganisaatiot.jyväskylänNormaalikoulu
import fi.oph.koski.raportointikanta.RaportointikantaTestMethods
import org.scalatest.{BeforeAndAfterAll, FreeSpec, Matchers}

class AikuistenPerusopetuksenOppijamäärätRaporttiSpec extends FreeSpec with Matchers with RaportointikantaTestMethods with BeforeAndAfterAll {
  private val application = KoskiApplicationForTests
  private val raporttiBuilder = AikuistenPerusopetuksenOppijamäärätRaportti(application.raportointiDatabase.db, application.organisaatioService)
  private lazy val raportti =
    raporttiBuilder.build(List(jyväskylänNormaalikoulu), sqlDate("2010-01-01"))(session(defaultUser)).rows.map(_.asInstanceOf[AikuistenPerusopetuksenOppijamäärätRaporttiRow])

  override def beforeAll(): Unit = loadRaportointikantaFixtures

  "Aikuisten perusopetuksen oppijamäärien raportti" - {
    "Raportti voidaan ladata ja lataaminen tuottaa auditlogin" in {
      authGet(s"api/raportit/aikuistenperusopetuksenoppijamaaratraportti?oppilaitosOid=$jyväskylänNormaalikoulu&paiva=2010-01-01&password=salasana") {
        verifyResponseStatusOk()
        response.headers("Content-Disposition").head should equal(s"""attachment; filename="aikuisten_perusopetuksen_vos_raportti-2010-01-01.xlsx"""")
        response.bodyBytes.take(ENCRYPTED_XLSX_PREFIX.length) should equal(ENCRYPTED_XLSX_PREFIX)
        AuditLogTester.verifyAuditLogMessage(Map("operation" -> "OPISKELUOIKEUS_RAPORTTI", "target" -> Map("hakuEhto" -> s"raportti=aikuistenperusopetuksenoppijamaaratraportti&oppilaitosOid=$jyväskylänNormaalikoulu&paiva=2010-01-01")))
      }
    }

    "Raportin kolumnit" in {
      lazy val r = findSingle(raportti)

      r.oppilaitosNimi should equal("Jyväskylän normaalikoulu")
      r.opetuskieli should equal("suomi")
      r.oppilaidenMääräVOS should equal(6)
      r.oppilaidenMääräMuuKuinVOS should equal(3)
      r.oppimääränSuorittajiaVOS should equal(3)
      r.oppimääränSuorittajiaMuuKuinVOS should equal(2)
      r.aineopiskelijoitaVOS should equal(3)
      r.aineopiskelijoitaMuuKuinVOS should equal(1)
      r.vieraskielisiäVOS should equal(1)
      r.vieraskielisiäMuuKuinVOS should equal(1)
    }
  }

  private def findSingle(rows: Seq[AikuistenPerusopetuksenOppijamäärätRaporttiRow]) = {
    val found = rows.filter(_.oppilaitosNimi.equals("Jyväskylän normaalikoulu"))
    found.length should be(1)
    found.head
  }

  private def session(user: MockUser)= user.toKoskiUser(application.käyttöoikeusRepository)
}
