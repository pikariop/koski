package fi.oph.koski.api.oppijavalidation

import fi.oph.koski.KoskiHttpSpec
import fi.oph.koski.api.misc.PutOpiskeluoikeusTestMethods
import fi.oph.koski.documentation.ExampleData._
import fi.oph.koski.documentation.ExamplesIB._
import fi.oph.koski.documentation.{ExampleData, LukioExampleData}
import fi.oph.koski.henkilo.KoskiSpecificMockOppijat.vuonna2004SyntynytPeruskouluValmis2021
import fi.oph.koski.http.{ErrorMatcher, KoskiErrorCategory}
import fi.oph.koski.schema._
import org.scalatest.freespec.AnyFreeSpec

import java.time.LocalDate.{of => date}

class OppijaValidationIBSpec extends AnyFreeSpec with KoskiHttpSpec with PutOpiskeluoikeusTestMethods[IBOpiskeluoikeus] {

  def tag = implicitly[reflect.runtime.universe.TypeTag[IBOpiskeluoikeus]]
  override def defaultOpiskeluoikeus = opiskeluoikeus

  "IB validation" - {

    "IB tutkinnon suoritus" - {

      "CAS-aine, arvosanan antaminen" - {
        def historiaOppiaine(level: String, arvosana: String) = ibAineSuoritus(ibOppiaine("HIS", level, 3), ibArviointi(arvosana), ibPredictedArviointi(arvosana))

        "Arvosana S" - {
          "Palautetaan HTTP/200" in {
            val opiskeluoikeus = opiskeluoikeusIBTutkinnollaWithCASArvosana("S")
            setupOppijaWithOpiskeluoikeus(opiskeluoikeus) {
              verifyResponseStatusOk()
          }}
        }

        "Arvosana numeerinen" - {
          "Palautetaan HTTP/400" in {
            val opiskeluoikeus = opiskeluoikeusIBTutkinnollaWithCASArvosana("4")
            setupOppijaWithOpiskeluoikeus(opiskeluoikeus) {
              verifyResponseStatus(400, ErrorMatcher.regex(KoskiErrorCategory.badRequest.validation.jsonSchema, ".*enumValueMismatch.*".r))
          }}
        }
      }

      "Kaksi samaa oppiainetta"  - {
        def historiaOppiaine(level: String, arvosana: String) = ibAineSuoritus(ibOppiaine("HIS", level, 3), ibArviointi(arvosana), ibPredictedArviointi(arvosana))
        "Joilla sama taso" - {
          val opiskeluoikeus = opiskeluoikeusIBTutkinnollaWithOppiaineet(List(
            historiaOppiaine(higherLevel, "S"),
            historiaOppiaine(higherLevel, "1")
          ))
          "Palautetaan HTTP/400" in { setupOppijaWithOpiskeluoikeus(opiskeluoikeus) {
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.rakenne.duplikaattiOsasuoritus("Osasuoritus oppiaineetib/HIS esiintyy useammin kuin kerran ryhmässä HL"))
          }}
        }

        "Eri taso, vain toisella tasolla numeroarviointi" - {
          val opiskeluoikeus = opiskeluoikeusIBTutkinnollaWithOppiaineet(List(
            historiaOppiaine(higherLevel, "2"),
            historiaOppiaine(standardLevel, "O")
          ))
          "Palautetaan HTTP/200" in { setupOppijaWithOpiskeluoikeus(opiskeluoikeus) {
            verifyResponseStatusOk()
          }}
        }

        "Eri taso, molemmilla numeroarviointi" - {
          val opiskeluoikeus = opiskeluoikeusIBTutkinnollaWithOppiaineet(List(
            historiaOppiaine(higherLevel, "3"),
            historiaOppiaine(standardLevel, "4")
          ))
          "Palautetaa HTTP/400" in { setupOppijaWithOpiskeluoikeus(opiskeluoikeus) {
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.rakenne.kaksiSamaaOppiainettaNumeroarvioinnilla("Kahdella saman oppiaineen suorituksella oppiaineetib/HIS ei molemmilla voi olla numeerista arviointia"))
          }}
        }

        "Eri taso, ei numeroarviointeja" - {
          val opiskeluoikeus = opiskeluoikeusIBTutkinnollaWithOppiaineet(List(
            historiaOppiaine(higherLevel, "S"),
            historiaOppiaine(standardLevel, "S")
          ))
          "Palautetaan HTTP/200" in { setupOppijaWithOpiskeluoikeus(opiskeluoikeus) {
            verifyResponseStatusOk()
          }}
        }
      }
    }

    "Opintojen rahoitus" - {
      "lasna -tilalta vaaditaan opintojen rahoitus" in {
        setupOppijaWithOpiskeluoikeus(defaultOpiskeluoikeus.copy(tila = LukionOpiskeluoikeudenTila(List(LukionOpiskeluoikeusjakso(longTimeAgo, opiskeluoikeusLäsnä))))) {
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.tila.tilaltaPuuttuuRahoitusmuoto("Opiskeluoikeuden tilalta lasna puuttuu rahoitusmuoto"))
        }
      }
      "valmistunut -tilalta vaaditaan opintojen rahoitus" in {
        val tila = LukionOpiskeluoikeudenTila(List(
          LukionOpiskeluoikeusjakso(longTimeAgo, opiskeluoikeusLäsnä, Some(valtionosuusRahoitteinen)),
          LukionOpiskeluoikeusjakso(date(2018, 1, 1), opiskeluoikeusValmistunut)
        ))
        setupOppijaWithOpiskeluoikeus(defaultOpiskeluoikeus.copy(tila = tila)) {
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.tila.tilaltaPuuttuuRahoitusmuoto("Opiskeluoikeuden tilalta valmistunut puuttuu rahoitusmuoto"))
        }
      }
    }

    "Maksuttomuustieto" - {
      "Kun opiskeluoikeus alkanut 1.1.2021 jälkeen" - {
        "Palautetaan HTTP/200" in {
          val opiskeluoikeus = defaultOpiskeluoikeus.copy(
            lisätiedot = Some(LukionOpiskeluoikeudenLisätiedot(maksuttomuus = Some(List(Maksuttomuus(alku = date(2021, 1, 1) , loppu = None, maksuton = true))))),
              tila = LukionOpiskeluoikeudenTila(
              List(
                LukionOpiskeluoikeusjakso(date(2021, 1, 1), LukioExampleData.opiskeluoikeusAktiivinen, Some(ExampleData.valtionosuusRahoitteinen)),
                LukionOpiskeluoikeusjakso(date(2022, 6, 4), LukioExampleData.opiskeluoikeusPäättynyt, Some(ExampleData.valtionosuusRahoitteinen))
              )
            )
          )
          setupOppijaWithOpiskeluoikeus(opiskeluoikeus, henkilö = vuonna2004SyntynytPeruskouluValmis2021) {
            verifyResponseStatusOk()
          }
        }
      }
      "Kun opiskeluoikeus alkanut ennen 1.1.2021" - {
        "Palautetaan HTTP/400" in {
          val opiskeluoikeus = defaultOpiskeluoikeus.copy(
            lisätiedot = Some(LukionOpiskeluoikeudenLisätiedot(maksuttomuus = Some(List(Maksuttomuus(alku = date(2020, 12, 31) , loppu = None, maksuton = true))))),
            tila = LukionOpiskeluoikeudenTila(
              List(
                LukionOpiskeluoikeusjakso(date(2020, 12, 31), LukioExampleData.opiskeluoikeusAktiivinen, Some(ExampleData.valtionosuusRahoitteinen)),
                LukionOpiskeluoikeusjakso(date(2022, 6, 4), LukioExampleData.opiskeluoikeusPäättynyt, Some(ExampleData.valtionosuusRahoitteinen))
              )
            )
          )
          setupOppijaWithOpiskeluoikeus(opiskeluoikeus, henkilö = vuonna2004SyntynytPeruskouluValmis2021) {
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation("Tieto koulutuksen maksuttomuudesta ei ole relevantti tässä opiskeluoikeudessa, sillä oppija on aloittanut Pre-IB opinnot aiemmin kuin 1.1.2021."))
          }
        }
      }
    }

    "Suorituksen vahvistaminen" - {
      "Ei onnistu, jos päättöarvosana puuttuu" in {
        val suoritus = ibTutkinnonSuoritus(predicted = false)
        val osasuoritukset = suoritus.osasuoritukset.map(_.map(_.copy(
          arviointi = None,
          predictedArviointi = ibPredictedArviointi("4"),
        )))
        val opiskeluoikeus = defaultOpiskeluoikeus.copy(suoritukset = List(suoritus.copy(osasuoritukset = osasuoritukset)))
        setupOppijaWithOpiskeluoikeus(opiskeluoikeus) {
          verifyResponseStatus(400,
            KoskiErrorCategory.badRequest.validation.tila.keskeneräinenOsasuoritus("Valmiiksi merkityllä suorituksella koulutus/301102 on keskeneräinen osasuoritus oppiaineetib/A"),
            KoskiErrorCategory.badRequest.validation.arviointi.arviointiPuuttuu("Vahvistetun suorituksen koulutus/301102 osasuoritukselta oppiaineetib/A puuttuu päättöarvosana"),
            KoskiErrorCategory.badRequest.validation.arviointi.arviointiPuuttuu("Vahvistetun suorituksen koulutus/301102 osasuoritukselta oppiaineetib/A2 puuttuu päättöarvosana"),
            KoskiErrorCategory.badRequest.validation.arviointi.arviointiPuuttuu("Vahvistetun suorituksen koulutus/301102 osasuoritukselta oppiaineetib/HIS puuttuu päättöarvosana"),
            KoskiErrorCategory.badRequest.validation.arviointi.arviointiPuuttuu("Vahvistetun suorituksen koulutus/301102 osasuoritukselta oppiaineetib/PSY puuttuu päättöarvosana"),
            KoskiErrorCategory.badRequest.validation.arviointi.arviointiPuuttuu("Vahvistetun suorituksen koulutus/301102 osasuoritukselta oppiaineetib/BIO puuttuu päättöarvosana"),
            KoskiErrorCategory.badRequest.validation.arviointi.arviointiPuuttuu("Vahvistetun suorituksen koulutus/301102 osasuoritukselta oppiaineetib/MATST puuttuu päättöarvosana"),
          )
        }
      }

      "Ei onnistu, jos predicted grade puuttuu" in {
        val suoritus = ibTutkinnonSuoritus(predicted = false)
        val osasuoritukset = suoritus.osasuoritukset.map(_.map(_.copy(
          arviointi = ibArviointi("5"),
          predictedArviointi = None,
        )))
        val opiskeluoikeus = defaultOpiskeluoikeus.copy(suoritukset = List(suoritus.copy(osasuoritukset = osasuoritukset)))
        setupOppijaWithOpiskeluoikeus(opiskeluoikeus) {
          verifyResponseStatus(400,
            KoskiErrorCategory.badRequest.validation.arviointi.arviointiPuuttuu("Vahvistetun suorituksen koulutus/301102 osasuoritukselta oppiaineetib/A puuttuu predicted grade"),
            KoskiErrorCategory.badRequest.validation.arviointi.arviointiPuuttuu("Vahvistetun suorituksen koulutus/301102 osasuoritukselta oppiaineetib/A2 puuttuu predicted grade"),
            KoskiErrorCategory.badRequest.validation.arviointi.arviointiPuuttuu("Vahvistetun suorituksen koulutus/301102 osasuoritukselta oppiaineetib/HIS puuttuu predicted grade"),
            KoskiErrorCategory.badRequest.validation.arviointi.arviointiPuuttuu("Vahvistetun suorituksen koulutus/301102 osasuoritukselta oppiaineetib/PSY puuttuu predicted grade"),
            KoskiErrorCategory.badRequest.validation.arviointi.arviointiPuuttuu("Vahvistetun suorituksen koulutus/301102 osasuoritukselta oppiaineetib/BIO puuttuu predicted grade"),
            KoskiErrorCategory.badRequest.validation.arviointi.arviointiPuuttuu("Vahvistetun suorituksen koulutus/301102 osasuoritukselta oppiaineetib/MATST puuttuu predicted grade"),
          )
        }
      }

      "Onnistuu, kun molemmat arvosanat on annettu" in {
        val suoritus = ibTutkinnonSuoritus(predicted = false)
        val osasuoritukset = suoritus.osasuoritukset.map(_.map(_.copy(
          arviointi = ibArviointi("5"),
          predictedArviointi = ibPredictedArviointi("4"),
        )))
        val opiskeluoikeus = defaultOpiskeluoikeus.copy(suoritukset = List(suoritus.copy(osasuoritukset = osasuoritukset)))
        setupOppijaWithOpiskeluoikeus(opiskeluoikeus) {
          verifyResponseStatusOk()
        }
      }
    }
  }

  private def opiskeluoikeusIBTutkinnollaWithOppiaineet(oppiaineet: List[IBOppiaineenSuoritus]) = {
    defaultOpiskeluoikeus.copy(
      suoritukset = List(ibTutkinnonSuoritus(predicted = false).copy(
        osasuoritukset = Some(oppiaineet)
      ))
    )
  }

  private def opiskeluoikeusIBTutkinnollaWithCASArvosana(arvosana: String) = {
    defaultOpiskeluoikeus.copy(
      suoritukset = List(ibTutkinnonSuoritus(predicted = false).copy(
        creativityActionService = Some(IBCASSuoritus(
          IBOppiaineCAS(laajuus = Some(LaajuusTunneissa(267))), ibCASArviointi(arvosana, predicted = true)
        ))
      ))
    )
  }
}
