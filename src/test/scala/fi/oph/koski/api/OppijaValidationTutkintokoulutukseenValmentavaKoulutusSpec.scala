package fi.oph.koski.api

import fi.oph.koski.KoskiHttpSpec
import fi.oph.koski.documentation.ExamplesTutkintokoulutukseenValmentavaKoulutus._
import fi.oph.koski.http.KoskiErrorCategory
import fi.oph.koski.koskiuser.MockUsers.stadinAmmattiopistoTallentaja
import fi.oph.koski.schema._
import org.scalatest.freespec.AnyFreeSpec

import java.time.LocalDate.{of => date}

class OppijaValidationTutkintokoulutukseenValmentavaKoulutusSpec extends AnyFreeSpec with PutOpiskeluoikeusTestMethods[TutkintokoulutukseenValmentavanOpiskeluoikeus] with KoskiHttpSpec {
  def tag = implicitly[reflect.runtime.universe.TypeTag[TutkintokoulutukseenValmentavanOpiskeluoikeus]]

  "Tutkintokoulutukseen valmentava koulutus" - {
    resetFixtures()

    "Suoritukset" - {
      "valmistuneen päätason suorituksen kesto ja osasuoritukset vaatimusten mukaiset" in {
        putOpiskeluoikeus(tuvaOpiskeluOikeusValmistunut, henkilö = tuvaHenkilöValmis, headers = authHeaders(stadinAmmattiopistoTallentaja) ++ jsonContent) {
          verifyResponseStatusOk()
        }
      }

      "keskeneräisen päätason suorituksen kesto ja osasuoritukset vaatimusten mukaiset" in {
        putOpiskeluoikeus(tuvaOpiskeluOikeusEiValmistunut, henkilö = tuvaHenkilöEiValmis, headers = authHeaders(stadinAmmattiopistoTallentaja) ++ jsonContent) {
          verifyResponseStatusOk()
        }
      }

      "valmistuneen päätason suorituksen laajuus liian pieni (ja osasuorituksia puuttuu)" in {
        val oo = tuvaOpiskeluOikeusValmistunut.copy(
          suoritukset = List(tuvaPäätasonSuoritus(laajuus = Some(3)).copy(
            osasuoritukset = Some(
              List(
                tuvaKoulutuksenMuunOsanSuoritus(
                  koulutusmoduuli = tuvaOpiskeluJaUrasuunnittelutaidot(laajuus = Some(2)),
                  koodistoviite = "tutkintokoulutukseenvalmentava",
                  arviointiPäivä = Some(date(2021, 9, 1))
                ),
                tuvaKoulutuksenMuunOsanSuoritus(
                  koulutusmoduuli = tuvaTyöelämätaidotJaTyöpaikallaTapahtuvaOppiminen(laajuus = Some(1)),
                  koodistoviite = "tutkintokoulutukseenvalmentava",
                  arviointiPäivä = Some(date(2021, 9, 1))
                )
              )
            )
          ))
        )

        putOpiskeluoikeus(oo, henkilö = tuvaHenkilöValmis, headers = authHeaders(stadinAmmattiopistoTallentaja) ++ jsonContent) {
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.laajuudet.tuvaPäätasonSuoritusVääräLaajuus())
        }
      }

      "valmistuneen päätason suorituksen laajuus liian suuri" in {
        val oo = tuvaOpiskeluOikeusValmistunut.copy(
          suoritukset = List(tuvaPäätasonSuoritus(laajuus = Some(39)).copy(
            osasuoritukset = Some(
              List(
                tuvaKoulutuksenMuunOsanSuoritus(
                  koulutusmoduuli = tuvaOpiskeluJaUrasuunnittelutaidot(laajuus = Some(2)),
                  koodistoviite = "tutkintokoulutukseenvalmentava",
                  arviointiPäivä = Some(date(2021, 9, 1))
                ),
                tuvaKoulutuksenMuunOsanSuoritus(
                  koulutusmoduuli = tuvaTyöelämätaidotJaTyöpaikallaTapahtuvaOppiminen(laajuus = Some(20)),
                  koodistoviite = "tutkintokoulutukseenvalmentava",
                  arviointiPäivä = Some(date(2021, 9, 1))
                ),
                tuvaKoulutuksenMuunOsanSuoritus(
                  koulutusmoduuli = tuvaArjenJaYhteiskunnallisenOsallisuudenTaidot(laajuus = Some(17)),
                  koodistoviite = "tutkintokoulutukseenvalmentava",
                  arviointiPäivä = Some(date(2021, 9, 1))
                )
              )
            )
          ))
        )

        putOpiskeluoikeus(oo, henkilö = tuvaHenkilöValmis, headers = authHeaders(stadinAmmattiopistoTallentaja) ++ jsonContent) {
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.laajuudet.tuvaPäätasonSuoritusVääräLaajuus())
        }
      }

      "valmistuneen päätason suorituksen osasuorituksen laajuus liian pieni" in {
        val oo = tuvaOpiskeluOikeusValmistunut.copy(
          suoritukset = List(tuvaPäätasonSuoritus(laajuus = Some(4)).copy(
            osasuoritukset = Some(
              List(
                tuvaKoulutuksenMuunOsanSuoritus(
                  koulutusmoduuli = tuvaOpiskeluJaUrasuunnittelutaidot(laajuus = Some(1)),
                  koodistoviite = "tutkintokoulutukseenvalmentava",
                  arviointiPäivä = Some(date(2021, 9, 1))
                ),
                tuvaKoulutuksenMuunOsanSuoritus(
                  koulutusmoduuli = tuvaTyöelämätaidotJaTyöpaikallaTapahtuvaOppiminen(laajuus = Some(2)),
                  koodistoviite = "tutkintokoulutukseenvalmentava",
                  arviointiPäivä = Some(date(2021, 9, 1))
                ),
                tuvaKoulutuksenMuunOsanSuoritus(
                  koulutusmoduuli = tuvaArjenJaYhteiskunnallisenOsallisuudenTaidot(laajuus = Some(1)),
                  koodistoviite = "tutkintokoulutukseenvalmentava",
                  arviointiPäivä = Some(date(2021, 9, 1))
                )
              )
            )
          ))
        )

        putOpiskeluoikeus(oo, henkilö = tuvaHenkilöValmis, headers = authHeaders(stadinAmmattiopistoTallentaja) ++ jsonContent) {
          verifyResponseStatus(
            expectedStatus = 400,
            KoskiErrorCategory.badRequest.validation.laajuudet.tuvaOsaSuoritusVääräLaajuus(
              "Tutkintokoulutukseen valmentavan koulutuksen opiskelu- ja urasuunnittelutaitojen osasuorituksen laajuus on oltava vähintään 2 ja enintään 10 viikkoa."
            )
          )
        }
      }

      "valmistuneen päätason suorituksesta puuttuu opiskelu ja urasuunnittelutaitojen osasuoritus" in {
        val oo = tuvaOpiskeluOikeusValmistunut.copy(
          suoritukset = List(tuvaPäätasonSuoritus(laajuus = Some(4)).copy(
            osasuoritukset = Some(
              List(
                tuvaKoulutuksenMuunOsanSuoritus(
                  koulutusmoduuli = tuvaTyöelämätaidotJaTyöpaikallaTapahtuvaOppiminen(laajuus = Some(2)),
                  koodistoviite = "tutkintokoulutukseenvalmentava",
                  arviointiPäivä = Some(date(2021, 9, 1))
                ),
                tuvaKoulutuksenMuunOsanSuoritus(
                  koulutusmoduuli = tuvaArjenJaYhteiskunnallisenOsallisuudenTaidot(laajuus = Some(2)),
                  koodistoviite = "tutkintokoulutukseenvalmentava",
                  arviointiPäivä = Some(date(2021, 9, 1))
                )
              )
            )
          ))
        )

        putOpiskeluoikeus(oo, henkilö = tuvaHenkilöValmis, headers = authHeaders(stadinAmmattiopistoTallentaja) ++ jsonContent) {
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.rakenne.tuvaOpiskeluJaUrasuunnittelutaitojenOsasuoritusPuuttuu())
        }
      }

      "valmistuneen päätason suorituksesta puuttuu riittävä määrä eri osasuorituksia" in {
        val oo = tuvaOpiskeluOikeusValmistunut.copy(
          suoritukset = List(tuvaPäätasonSuoritus(laajuus = Some(4)).copy(
            osasuoritukset = Some(
              List(
                tuvaKoulutuksenMuunOsanSuoritus(
                  koulutusmoduuli = tuvaOpiskeluJaUrasuunnittelutaidot(laajuus = Some(2)),
                  koodistoviite = "tutkintokoulutukseenvalmentava",
                  arviointiPäivä = Some(date(2021, 9, 1))
                ),
                tuvaKoulutuksenMuunOsanSuoritus(
                  koulutusmoduuli = tuvaArjenJaYhteiskunnallisenOsallisuudenTaidot(laajuus = Some(2)),
                  koodistoviite = "tutkintokoulutukseenvalmentava",
                  arviointiPäivä = Some(date(2021, 9, 1))
                )
              )
            )
          ))
        )

        putOpiskeluoikeus(oo, henkilö = tuvaHenkilöValmis, headers = authHeaders(stadinAmmattiopistoTallentaja) ++ jsonContent) {
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.rakenne.tuvaOsasuorituksiaLiianVähän())
        }
      }
    }
  }

  override def defaultOpiskeluoikeus: TutkintokoulutukseenValmentavanOpiskeluoikeus = tuvaOpiskeluOikeusValmistunut
}