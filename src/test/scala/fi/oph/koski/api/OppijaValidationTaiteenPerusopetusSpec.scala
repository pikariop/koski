package fi.oph.koski.api

import com.typesafe.config.Config
import fi.oph.koski.db.OpiskeluoikeusRow
import fi.oph.koski.documentation.{ExamplesTaiteenPerusopetus => TPO}
import fi.oph.koski.henkilo.KoskiSpecificMockOppijat
import fi.oph.koski.http.{HttpStatus, KoskiErrorCategory}
import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.koskiuser.{KoskiSpecificSession, MockUsers}
import fi.oph.koski.log.{AuditLogTester, KoskiAuditLogMessageField, KoskiOperation}
import fi.oph.koski.opiskeluoikeus.OppijaOidJaLähdejärjestelmänId
import fi.oph.koski.schema._
import fi.oph.koski.suoritusjako.Suoritusjako
import fi.oph.koski.tutkinto.Perusteet
import fi.oph.koski.validation.KoskiValidator
import fi.oph.koski.{KoskiApplicationForTests, KoskiHttpSpec}
import org.json4s.{JBool, JObject, JString}
import org.json4s.jackson.JsonMethods
import org.scalatest.freespec.AnyFreeSpec

import java.time.LocalDate

class OppijaValidationTaiteenPerusopetusSpec
  extends AnyFreeSpec
    with KoskiHttpSpec
    with TutkinnonPerusteetTest[TaiteenPerusopetuksenOpiskeluoikeus]
    with OpiskeluoikeudenMitätöintiJaPoistoTestMethods
    with SearchTestMethods
    with SuoritusjakoTestMethods {
  override def tag = implicitly[reflect.runtime.universe.TypeTag[TaiteenPerusopetuksenOpiskeluoikeus]]

  override def defaultOpiskeluoikeus = TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä

  override def opiskeluoikeusWithPerusteenDiaarinumero(diaari: Option[String]): TaiteenPerusopetuksenOpiskeluoikeus = {
    defaultOpiskeluoikeus.copy(
      suoritukset = List(
        TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia.copy(
          koulutusmoduuli = TPO.PäätasonSuoritus.Koulutusmoduuli.musiikkiYleinenOppimääräEiLaajuutta.copy(
              perusteenDiaarinumero = diaari
            )
        )
      )
    )
  }

  override def eperusteistaLöytymätönValidiDiaarinumero: String = "6/011/2015"

  val oppija = KoskiSpecificMockOppijat.taiteenPerusopetusAloitettu

  val taiteenPerusopetusAloitettuOpiskeluoikeus = getOpiskeluoikeudet(
    KoskiSpecificMockOppijat.taiteenPerusopetusAloitettu.oid
  ).head match {
    case oo: TaiteenPerusopetuksenOpiskeluoikeus => oo
  }
  val taiteenPerusopetusAloitettuOpiskeluoikeusOid = taiteenPerusopetusAloitettuOpiskeluoikeus.oid.get


  "Opiskeluoikeuden lisääminen" - {
    "aloittanut example-opiskeluoikeus voidaan kirjoittaa tietokantaan" in {
      putOpiskeluoikeus(defaultOpiskeluoikeus, henkilö = oppija) {
        verifyResponseStatusOk()
      }
    }

    "valmistunut example-opiskeluoikeus voidaan kirjoittaa tietokantaan" in {
      putOpiskeluoikeus(TPO.Opiskeluoikeus.hyväksytystiSuoritettuLaajaOppimäärä, henkilö = oppija) {
        verifyResponseStatusOk()
      }
    }

    "vastaavan rinnakkaisen opiskeluoikeuden lisääminen on sallittu" in {
      resetFixtures()

      val oo1 = postAndGetOpiskeluoikeusV2(TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä, henkilö = KoskiSpecificMockOppijat.tyhjä)
      val oo2 = postAndGetOpiskeluoikeusV2(TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä, henkilö = KoskiSpecificMockOppijat.tyhjä)

      oo2.oid.get should not be(oo1.oid.get)

      val oot = oppijaByHetu(KoskiSpecificMockOppijat.tyhjä.hetu).tallennettavatOpiskeluoikeudet
      oot.size should be(2)
      oot.flatMap(_.oid) should contain(oo2.oid.get)
      oot.flatMap(_.oid) should contain(oo1.oid.get)
    }

    "päätason valmis suoritus yhdistetään samalle opiskeluoikeudelle uuden päätason suorituksen kanssa vaikka suoritukset siirretään erikseen" in {
      resetFixtures()

      val ooVanhaSuoritus = TPO.Opiskeluoikeus.hyväksytystiSuoritettuLaajaOppimäärä.copy(
        suoritukset = List(
          TPO.PäätasonSuoritus.laajojenPerusopintojenSuoritusArvioituJaVahvistettuJaOsasuorituksia
        )
      )

      val oid = postAndGetOpiskeluoikeusV2(ooVanhaSuoritus, henkilö = KoskiSpecificMockOppijat.tyhjä).oid

      val ooUusiSuoritus = TPO.Opiskeluoikeus.hyväksytystiSuoritettuLaajaOppimäärä.copy(
        oid = oid,
        suoritukset = List(
          TPO.PäätasonSuoritus.laajojenSyventävienOpintojenSuoritusArvioituJaVahvistettuJaOsasuorituksia
        )
      )

      putOpiskeluoikeus(ooUusiSuoritus, henkilö = KoskiSpecificMockOppijat.tyhjä){
        verifyResponseStatusOk()
      }

      val oot = getOpiskeluoikeus(oid.get).asInstanceOf[TaiteenPerusopetuksenOpiskeluoikeus]
      oot.suoritukset.size shouldBe 2
    }
  }

  "Suorituksen vahvistaminen" - {
    "suoritusta ei voi vahvistaa jos sillä on arvioimattomia osasuorituksia" in {
      val oo = TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
        suoritukset = List(
          TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia.copy(
            koulutusmoduuli = TPO.PäätasonSuoritus.Koulutusmoduuli.musiikkiYleinenOppimääräYhteisetOpinnot,
            arviointi = Some(List(TPO.arviointiHyväksytty)),
            vahvistus = Some(TPO.vahvistus),
            osasuoritukset = Some(List(
              TPO.Osasuoritus.osasuoritusMusiikki("Musa 1", 11.11).copy(
                arviointi = None
              )
            ))
          )
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.tila.keskeneräinenOsasuoritus("Valmiiksi merkityllä suorituksella koulutus/999907 on keskeneräinen osasuoritus Musa 1 (Musiikin kurssi)"))
      }
    }

    "suorituksen voi vahvistaa jos sillä ei ole yhtään osasuoritusta" in {
      val oo = TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
        suoritukset = List(
          TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia.copy(
            koulutusmoduuli = TPO.PäätasonSuoritus.Koulutusmoduuli.musiikkiYleinenOppimääräYhteisetOpinnot,
            arviointi = Some(List(TPO.arviointiHyväksytty)),
            vahvistus = Some(TPO.vahvistus)
          )
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatusOk()
      }
    }
  }

  "Opiskeluoikeuden päättäminen" - {
    "opiskeluoikeudelle ei voi lisätä päättävää tilaa hyväksytysti suoritettu jos on suorituksia ilman vahvistusta" in {
      val oo = TPO.Opiskeluoikeus.hyväksytystiSuoritettuLaajaOppimäärä.copy(
        suoritukset = List(
          TPO.PäätasonSuoritus.laajojenPerusopintojenSuoritusArvioituJaVahvistettuJaOsasuorituksia.copy(
            vahvistus = None
          )
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.tila.vahvistusPuuttuu ("Suoritukselta puuttuu vahvistus, vaikka opiskeluoikeus on tilassa Hyväksytysti suoritettu"))
      }
    }

    "keskeneräiselle opiskeluoikeudelle voi aina lisätä päättävän tilan päättynyt suorituksista riippumatta" in {
      val oo = TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
        tila = TaiteenPerusopetuksenOpiskeluoikeudenTila(
          opiskeluoikeusjaksot = List(
            TPO.Opiskeluoikeus.jaksoLäsnä(),
            TPO.Opiskeluoikeus.jaksoPäättynyt(TPO.alkupäivä.plusDays(1))
          )
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatusOk()
      }
    }
  }

  "Suoritusten laajuudet" - {
    "suoritusta ei voi vahvistaa ilman laajuutta" in {
      val oo = TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
        suoritukset = List(
          TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia.copy(
            koulutusmoduuli = TPO.PäätasonSuoritus.Koulutusmoduuli.musiikkiYleinenOppimääräYhteisetOpinnot.copy(
              laajuus = None
            ),
            arviointi = Some(List(TPO.arviointiHyväksytty)),
            vahvistus = Some(TPO.vahvistus)
          )
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.laajuudet.taiteenPerusopetuksenLaajuus ("Yleisen oppimäärän yhteisten opintojen laajuus on oltava vähintään 11.11 opintopistettä."))
      }
    }

    "vahvistetun yleisen oppimäärän yhteisten opintojen suorituksen laajuus oltava vähintään 11,11op" in {
      val oo = TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
        tila = TPO.Opiskeluoikeus.tilaHyväksytystiSuoritettu(),
        suoritukset = List(
          TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia.copy(
            koulutusmoduuli = TPO.PäätasonSuoritus.Koulutusmoduuli.musiikkiYleinenOppimääräYhteisetOpinnot.copy(
              laajuus = Some(LaajuusOpintopisteissä(11.10))
            ),
            arviointi = Some(List(TPO.arviointiHyväksytty)),
            vahvistus = Some(TPO.vahvistus),
            osasuoritukset = None
          )
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.laajuudet.taiteenPerusopetuksenLaajuus ("Yleisen oppimäärän yhteisten opintojen laajuus on oltava vähintään 11.11 opintopistettä."))
      }
    }

    "vahvistamattoman suorituksen laajuutta ei ole rajoitettu validaatiolla" in {
      val oo = TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
        suoritukset = List(
          TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia.copy(
            koulutusmoduuli = TPO.PäätasonSuoritus.Koulutusmoduuli.musiikkiYleinenOppimääräYhteisetOpinnot.copy(
              laajuus = Some(LaajuusOpintopisteissä(1))
            ),
            arviointi = None,
            vahvistus = None,
            osasuoritukset = None
          )
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatusOk()
      }
    }

    "vahvistetun yleisen oppimäärän teemaopintojen suorituksen laajuus oltava vähintään 7,41op" in {
      val oo = TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
        tila = TPO.Opiskeluoikeus.tilaHyväksytystiSuoritettu(),
        suoritukset = List(
          TPO.PäätasonSuoritus.yleistenTeemaopintojenSuoritusEiArvioituEiOsasuorituksia.copy(
            koulutusmoduuli = TPO.PäätasonSuoritus.Koulutusmoduuli.musiikkiYleinenOppimääräTeemaopinnot.copy(
              laajuus = Some(LaajuusOpintopisteissä(7.4))
            ),
            arviointi = Some(List(TPO.arviointiHyväksytty)),
            vahvistus = Some(TPO.vahvistus),
            osasuoritukset = None
          )
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.laajuudet.taiteenPerusopetuksenLaajuus ("Yleisen oppimäärän teemaopintojen laajuus on oltava vähintään 7.41 opintopistettä."))
      }
    }

    "vahvistetun laajan oppimäärän perusopintojen suorituksen laajuus oltava vähintään 29,63op" in {
      val oo = TPO.Opiskeluoikeus.hyväksytystiSuoritettuLaajaOppimäärä.copy(
        suoritukset = List(
          TPO.PäätasonSuoritus.laajojenPerusopintojenSuoritusArvioituJaVahvistettuJaOsasuorituksia.copy(
            koulutusmoduuli = TPO.PäätasonSuoritus.Koulutusmoduuli.musiikkiLaajaOppimääräPerusopinnot.copy(
              laajuus = Some(LaajuusOpintopisteissä(29.62))
            ),
            osasuoritukset = None
          ),
          TPO.PäätasonSuoritus.laajojenSyventävienOpintojenSuoritusArvioituJaVahvistettuJaOsasuorituksia
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.laajuudet.taiteenPerusopetuksenLaajuus ("Laajan oppimäärän perusopintojen laajuus on oltava vähintään 29.63 opintopistettä."))
      }
    }

    "vahvistetun laajan oppimäärän syventävien opintojen suorituksen laajuus oltava vähintään 18,52op" in {
      val oo = TPO.Opiskeluoikeus.hyväksytystiSuoritettuLaajaOppimäärä.copy(
        suoritukset = List(
          TPO.PäätasonSuoritus.laajojenPerusopintojenSuoritusArvioituJaVahvistettuJaOsasuorituksia,
          TPO.PäätasonSuoritus.laajojenSyventävienOpintojenSuoritusArvioituJaVahvistettuJaOsasuorituksia.copy(
            koulutusmoduuli = TPO.PäätasonSuoritus.Koulutusmoduuli.musiikkiLaajaOppimääräSyventävätOpinnot.copy(
              laajuus = Some(LaajuusOpintopisteissä(18.51))
            ),
            osasuoritukset = None
          )
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.laajuudet.taiteenPerusopetuksenLaajuus("Laajan oppimäärän syventävien opintojen laajuus on oltava vähintään 18.52 opintopistettä."))
      }
    }

    "vahvistetun suorituksen laajuus on oltava sama kuin paikallisten osasuoritusten yhteenlasketut laajuudet" in {
      val oo = TPO.Opiskeluoikeus.hyväksytystiSuoritettuLaajaOppimäärä.copy(
        suoritukset = List(
          TPO.PäätasonSuoritus.laajojenPerusopintojenSuoritusArvioituJaVahvistettuJaOsasuorituksia,
          TPO.PäätasonSuoritus.laajojenSyventävienOpintojenSuoritusArvioituJaVahvistettuJaOsasuorituksia.copy(
            koulutusmoduuli = TPO.PäätasonSuoritus.Koulutusmoduuli.musiikkiLaajaOppimääräSyventävätOpinnot.copy(
              laajuus = Some(LaajuusOpintopisteissä(20))
            ),
            osasuoritukset = Some(List(
              TPO.Osasuoritus.osasuoritusMusiikki("musa1", 19.0)
            ))
          )
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.laajuudet.osasuoritustenLaajuuksienSumma ("Suorituksen koulutus/999907 osasuoritusten laajuuksien summa 19.0 ei vastaa suorituksen laajuutta 20.0"))
      }
    }
  }

  "Suoritusten tyypit ja taiteenalat" - {
    "yleisen oppimäärän opiskeluoikeudelle ei voi siirtää laajan oppimäärän opintotasoja" in {
      val oo = TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
        suoritukset = List(
          TPO.PäätasonSuoritus.laajojenPerusopintojenSuoritusArvioituJaVahvistettuJaOsasuorituksia
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.rakenne.tpoVääräOpintotaso ("Suorituksen opintotaso ei sisälly opiskeluoikeuden oppimäärään."))
      }
    }

    "laajan oppimäärän opiskeluoikeudelle voi siirtää vain laajan oppimäärän opintotasoja" in {
      val oo = TPO.Opiskeluoikeus.hyväksytystiSuoritettuLaajaOppimäärä.copy(
        suoritukset = List(
          TPO.PäätasonSuoritus.yleistenTeemaopintojenSuoritusEiArvioituEiOsasuorituksia.copy(
            koulutusmoduuli = TPO.PäätasonSuoritus.Koulutusmoduuli.musiikkiYleinenOppimääräTeemaopinnot.copy(
              laajuus = Some(LaajuusOpintopisteissä(7.41))
            ),
            arviointi = Some(List(TPO.arviointiHyväksytty)),
            vahvistus = Some(TPO.vahvistus),
            osasuoritukset = None
          )
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.rakenne.tpoVääräOpintotaso ("Suorituksen opintotaso ei sisälly opiskeluoikeuden oppimäärään."))
      }
    }

    "opiskeluoikeudelle ei voi siirtää suorituksia eri taiteenaloilta" in {
      val oo = TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
        suoritukset = List(
          TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia,
          TPO.PäätasonSuoritus.yleistenTeemaopintojenSuoritusEiArvioituEiOsasuorituksia.copy(
            koulutusmoduuli = TPO.PäätasonSuoritus.Koulutusmoduuli.musiikkiYleinenOppimääräTeemaopinnot.copy(
              taiteenala = TPO.kuvataiteenTaiteenala
            )
          )
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.rakenne.tpoEriTaiteenalat ("Taiteen perusopetuksen opiskeluoikeudella ei voi olla suorituksia eri taiteenaloilta."))
      }
    }
  }

  "Perusteet" - {
    "yleisen oppimäärän opiskeluoikeudelle ei voi lisätä väärää diaarinumeroa" in {
      val oo = TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
        suoritukset = List(
          TPO.PäätasonSuoritus.yleistenTeemaopintojenSuoritusEiArvioituEiOsasuorituksia.copy(
            koulutusmoduuli = TPO.PäätasonSuoritus.Koulutusmoduuli.musiikkiYleinenOppimääräTeemaopinnot.copy(
              perusteenDiaarinumero = Some(Perusteet.TaiteenPerusopetuksenLaajanOppimääränPerusteet2017.diaari)
            )
          )
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.rakenne.vääräDiaari(s"""Väärä diaarinumero "OPH-2068-2017" suorituksella taiteenperusopetuksenyleisenoppimaaranteemaopinnot, sallitut arvot: OPH-2069-2017"""))
      }
    }

    "laajan oppimäärän opiskeluoikeudelle ei voi lisätä väärää diaarinumeroa" in {
      val oo = TPO.Opiskeluoikeus.hyväksytystiSuoritettuLaajaOppimäärä.copy(
        suoritukset = List(
          TPO.PäätasonSuoritus.laajojenPerusopintojenSuoritusArvioituJaVahvistettuJaOsasuorituksia.copy(
            koulutusmoduuli = TPO.PäätasonSuoritus.Koulutusmoduuli.musiikkiLaajaOppimääräPerusopinnot.copy(
              perusteenDiaarinumero = Some(Perusteet.TaiteenPerusopetuksenYleisenOppimääränPerusteet2017.diaari)
            )
          )
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.rakenne.vääräDiaari(s"""Väärä diaarinumero "OPH-2069-2017" suorituksella taiteenperusopetuksenlaajanoppimaaranperusopinnot, sallitut arvot: OPH-2068-2017"""))
      }
    }

    "opiskeluoikeutta ei voi siirtää jos se ei ole voimassa perusteen voimassaolon aikana" in {
      val oo = TPO.Opiskeluoikeus.hyväksytystiSuoritettuLaajaOppimäärä.copy(
        tila = TaiteenPerusopetuksenOpiskeluoikeudenTila(
          List(
            TPO.Opiskeluoikeus.jaksoLäsnä(LocalDate.of(2018, 1,1)),
            TPO.Opiskeluoikeus.jaksoHyväksytystiSuoritettu(LocalDate.of(2018, 7, 31))
          )
        )
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatus(
          400,
          KoskiErrorCategory.badRequest.validation.rakenne.perusteEiVoimassa("Perusteen tulee olla voimassa opiskeluoikeuden voimassaoloaikana."),
          KoskiErrorCategory.badRequest.validation.rakenne.perusteEiVoimassa("Perusteen tulee olla voimassa opiskeluoikeuden voimassaoloaikana.")
        )
      }
    }
  }

  "Mitätöinti" - {
    "Opiskeluoikeuden voi mitätöidä PUT-rajapinnalla ja mitätöinti on poisto" in {
      resetFixtures()
      val oo = taiteenPerusopetusAloitettuOpiskeluoikeus.copy(
        lähdejärjestelmänId = Some(
          LähdejärjestelmäId(
            id = Some("tpo1"),
            lähdejärjestelmä = Koodistokoodiviite("primus", "lahdejarjestelma")
          )
        ),
        tila = TPO.Opiskeluoikeus.tilaMitätöity()
      )

      putOpiskeluoikeus(oo, henkilö = oppija) {
        verifyResponseStatusOk()
      }

      val poistettuRivi: Either[HttpStatus, OpiskeluoikeusRow] =
        KoskiApplicationForTests.opiskeluoikeusRepository.findByOid(
          taiteenPerusopetusAloitettuOpiskeluoikeusOid
        )(
          KoskiSpecificSession.systemUserMitätöidytJaPoistetut
        )

      poistettuRivi.isRight should be(true)

      poistettuRivi.map(ooRow => {
        ooRow.oid should be(taiteenPerusopetusAloitettuOpiskeluoikeusOid)
        ooRow.versionumero should be(taiteenPerusopetusAloitettuOpiskeluoikeus.versionumero.get + 1)
        ooRow.aikaleima.toString should include(LocalDate.now.toString)
        ooRow.oppijaOid should be(KoskiSpecificMockOppijat.taiteenPerusopetusAloitettu.oid)
        ooRow.oppilaitosOid should be("")
        ooRow.koulutustoimijaOid should be(None)
        ooRow.sisältäväOpiskeluoikeusOid should be(None)
        ooRow.sisältäväOpiskeluoikeusOppilaitosOid should be(None)
        ooRow.data should be(JObject(List()))
        ooRow.luokka should be(None)
        ooRow.mitätöity should be(true)
        ooRow.koulutusmuoto should be("")
        ooRow.alkamispäivä.toString should be(LocalDate.now.toString)
        ooRow.päättymispäivä should be(None)
        ooRow.suoritusjakoTehty should be(false)
        ooRow.suoritustyypit should be(Nil)
        ooRow.poistettu should be(true)
      })
    }

    "Opiskeluoikeuden voi mitätöidä DELETE-rajapinnalla ja mitätöinti on poisto" in {
      resetFixtures()
      mitätöiOpiskeluoikeus(taiteenPerusopetusAloitettuOpiskeluoikeusOid)

      val poistettuRivi: Either[HttpStatus, OpiskeluoikeusRow] =
        KoskiApplicationForTests.opiskeluoikeusRepository.findByOid(
          taiteenPerusopetusAloitettuOpiskeluoikeusOid
        )(
          KoskiSpecificSession.systemUserMitätöidytJaPoistetut
        )

      poistettuRivi.isRight should be(true)

      poistettuRivi.map(ooRow => {
        ooRow.oid should be(taiteenPerusopetusAloitettuOpiskeluoikeusOid)
        ooRow.versionumero should be(taiteenPerusopetusAloitettuOpiskeluoikeus.versionumero.get + 1)
        ooRow.aikaleima.toString should include(LocalDate.now.toString)
        ooRow.oppijaOid should be(KoskiSpecificMockOppijat.taiteenPerusopetusAloitettu.oid)
        ooRow.oppilaitosOid should be("")
        ooRow.koulutustoimijaOid should be(None)
        ooRow.sisältäväOpiskeluoikeusOid should be(None)
        ooRow.sisältäväOpiskeluoikeusOppilaitosOid should be(None)
        ooRow.data should be(JObject(List()))
        ooRow.luokka should be(None)
        ooRow.mitätöity should be(true)
        ooRow.koulutusmuoto should be("")
        ooRow.alkamispäivä.toString should be(LocalDate.now.toString)
        ooRow.päättymispäivä should be(None)
        ooRow.suoritusjakoTehty should be(false)
        ooRow.suoritustyypit should be(Nil)
        ooRow.poistettu should be(true)
      })
    }
  }

  "Suostumuksen peruutus päätason suoritukselta" - {
    "suostumuksen peruutus opiskeluoikeuden ainoalta suoritukselta - opiskeluoikeus poistuu" in {
      resetFixtures()
      AuditLogTester.clearMessages

      // Syötä opiskeluoikeus
      val oo = postAndGetOpiskeluoikeusV2(
        TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
          suoritukset = List(TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia)
        ), henkilö = KoskiSpecificMockOppijat.tyhjä
      )

      oo.oid should not be empty

      // Oppija-listauksen pituus ennen suostumuksen peruuttamista
      KoskiApplicationForTests.perustiedotIndexer.sync(true)
      val opiskeluoikeuksiaEnnenPerumistaOpenSearchissa = searchForPerustiedot(
        Map("toimipiste" -> defaultOpiskeluoikeus.oppilaitos.get.oid), MockUsers.paakayttaja
      ).length


      // Peru suostumus käyttäjän omilla oikeuksilla
      val loginHeadersKansalainen = kansalainenLoginHeaders(KoskiSpecificMockOppijat.tyhjä.hetu)
      poistaSuostumusSuoritukselta(
        TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia,
        oo,
        loginHeadersKansalainen
      )

      // Opiskeluoikeus on poistettu
      authGet("api/opiskeluoikeus/" + oo.oid.get) {
        verifyResponseStatus(404)
      }

      // Opiskeluoikeuden historia on tyhjennetty
      KoskiApplicationForTests
        .historyRepository
        .findByOpiskeluoikeusOid(oo.oid.get)(KoskiSpecificSession.systemUser) should be(None)

      // Suostumuksen peruutuksesta on jäänyt rivi peruttujen suostumuksen listaukseen
      get(s"/api/opiskeluoikeus/suostumuksenperuutus", headers = authHeaders(MockUsers.paakayttaja)) {
        verifyResponseStatusOk()

        val json = JsonMethods.parse(body)
        val obj = json(0)

        (obj \\ "Opiskeluoikeuden oid") shouldBe JString(oo.oid.get)
        (obj \\ "Oppijan oid") shouldBe a[JString]
        (obj \\ "Opiskeluoikeuden päättymispäivä") shouldBe JString("")
        (obj \\ "Mitätöity") should not be a[JString]
        (obj \\ "Suostumus peruttu") shouldBe a[JString]
        (obj \\ "Oppilaitoksen oid") shouldBe JString(TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.oppilaitos.get.oid)
        (obj \\ "Oppilaitoksen nimi") shouldBe JString(TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.oppilaitos.get.nimi.get.get("fi"))
      }

      // Suostumuksen peruutuksesta on jäänyt tiedot audit logille
      val logMessages = AuditLogTester.getLogMessages
      logMessages.length should equal(4)

      AuditLogTester.verifyAuditLogMessage(
        logMessages(3), Map(
          "operation" -> KoskiOperation.KANSALAINEN_SUOSTUMUS_PERUMINEN.toString,
          "target" -> Map(
            KoskiAuditLogMessageField.opiskeluoikeusOid.toString -> oo.oid.get,
          )
        )
      )

      // Opiskeluoikeus on poistunut oppija-listauksesta
      KoskiApplicationForTests.perustiedotIndexer.sync(true)
      val opiskeluoikeuksia = searchForPerustiedot(Map("toimipiste" -> oo.oppilaitos.get.oid))
      opiskeluoikeuksia.length should equal(opiskeluoikeuksiaEnnenPerumistaOpenSearchissa - 1)
    }

    "suostumuksen peruutus suoritukselta kun opiskeluoikeulla enemmän kuin yksi suoritus - opiskeluoikeus säilyy mutta toinen suoritus poistuu" in {
      resetFixtures()
      AuditLogTester.clearMessages
      val poistettavaSuoritus = TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia

      // Syötä opiskeluoikeus
      val oo = postAndGetOpiskeluoikeusV2(
        TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä,
        henkilö = KoskiSpecificMockOppijat.tyhjä
      )
      oo.oid should not be empty

      // Oppija-listauksen pituus ennen suostumuksen peruuttamista
      KoskiApplicationForTests.perustiedotIndexer.sync(true)
      val opiskeluoikeuksiaEnnenPerumistaOpenSearchissa = searchForPerustiedot(
        Map("toimipiste" -> defaultOpiskeluoikeus.oppilaitos.get.oid), MockUsers.paakayttaja
      ).length

      // Peru suostumus käyttäjän omilla oikeuksilla
      val loginHeadersKansalainen = kansalainenLoginHeaders(KoskiSpecificMockOppijat.tyhjä.hetu)
      poistaSuostumusSuoritukselta(poistettavaSuoritus, oo, loginHeadersKansalainen)

      // Opiskeluoikeus löytyy ilman suoritusta jolta peruttiin suostumus
      val suostumusPeruttuOo = getOpiskeluoikeus(oo.oid.get)
      suostumusPeruttuOo.suoritukset.size shouldBe 1
      suostumusPeruttuOo.suoritukset.find(_.tyyppi.koodiarvo == poistettavaSuoritus.tyyppi.koodiarvo) should be(None)

      // Opiskeluoikeuden historia löytyy mutta on nollattu eikä sisällä poistettua suoritusta
      val historiaSuostumusPeruttu = KoskiApplicationForTests.historyRepository
        .findByOpiskeluoikeusOid(oo.oid.get)(KoskiSpecificSession.systemUser).getOrElse(List.empty)
      historiaSuostumusPeruttu.size shouldBe 2

      KoskiApplicationForTests.historyRepository.findVersion(oo.oid.get, 1)(KoskiSpecificSession.systemUser).toOption.get
        .suoritukset
        .find(_.tyyppi.koodiarvo == poistettavaSuoritus.tyyppi.koodiarvo) should be(None)
      KoskiApplicationForTests.historyRepository.findVersion(oo.oid.get, 2)(KoskiSpecificSession.systemUser).toOption.get
        .suoritukset
        .find(_.tyyppi.koodiarvo == poistettavaSuoritus.tyyppi.koodiarvo) should be(None)

      // Suostumuksen peruutuksesta on jäänyt rivi peruttujen suostumuksen listaukseen
      get(s"/api/opiskeluoikeus/suostumuksenperuutus", headers = authHeaders(MockUsers.paakayttaja)) {
        verifyResponseStatusOk()

        val json = JsonMethods.parse(body)
        val obj = json(0)

        (obj \\ "Opiskeluoikeuden oid") shouldBe JString(oo.oid.get)
        (obj \\ "Oppijan oid") shouldBe a[JString]
        (obj \\ "Opiskeluoikeuden päättymispäivä") shouldBe JString("")
        (obj \\ "Mitätöity") should not be a[JString]
        (obj \\ "Suostumus peruttu") shouldBe a[JString]
        (obj \\ "Oppilaitoksen oid") shouldBe JString(TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.oppilaitos.get.oid)
        (obj \\ "Oppilaitoksen nimi") shouldBe JString(TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.oppilaitos.get.nimi.get.get("fi"))
      }

      // Suostumuksen peruutuksesta on jäänyt tiedot audit logille
      val logMessages = AuditLogTester.getLogMessages
      logMessages.length should equal(5)

      AuditLogTester.verifyAuditLogMessage(
        logMessages(3), Map(
          "operation" -> KoskiOperation.KANSALAINEN_SUOSTUMUS_PERUMINEN.toString,
          "target" -> Map(
            KoskiAuditLogMessageField.opiskeluoikeusOid.toString -> oo.oid.get,
            KoskiAuditLogMessageField.suorituksenTyyppi.toString -> TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia.tyyppi.koodiarvo
          )
        )
      )

      // Opiskeluoikeus ei ole poistunut oppija-listauksesta
      KoskiApplicationForTests.perustiedotIndexer.sync(true)
      val opiskeluoikeuksia = searchForPerustiedot(Map("toimipiste" -> oo.oppilaitos.get.oid))
      opiskeluoikeuksia.length should equal(opiskeluoikeuksiaEnnenPerumistaOpenSearchissa)
    }

    "kansalainen ei voi peruuttaa kenenkään muun suostumusta suoritukselta" in {
      resetFixtures()
      val poistettavaSuoritus = TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia
      val oo = getOpiskeluoikeudet(KoskiSpecificMockOppijat.taiteenPerusopetusAloitettu.oid).head

      val loginHeadersKansalainen = kansalainenLoginHeaders(KoskiSpecificMockOppijat.taiteenPerusopetusValmis.hetu.get)
      post(
        uri = s"/api/opiskeluoikeus/suostumuksenperuutus/${oo.oid.get}",
        headers = loginHeadersKansalainen.toMap,
        params = Seq(("suorituksentyyppi", poistettavaSuoritus.tyyppi.koodiarvo))
      ) {
        verifyResponseStatus(
          403, KoskiErrorCategory.forbidden.opiskeluoikeusEiSopivaSuostumuksenPerumiselle(
            s"Opiskeluoikeuden ${oo.oid.get} annettu suostumus ei ole peruttavissa. Joko opiskeluoikeudesta on tehty suoritusjako, viranomainen on käyttänyt opiskeluoikeuden tietoja päätöksenteossa, opiskeluoikeus on tyyppiä, jonka kohdalla annettua suostumusta ei voida perua tai opiskeluoikeudelta ei löytynyt annetun syötteen tyyppistä päätason suoritusta."
          )
        )
      }
    }

    "säilyneen suorituksen päivttäminen onnistuu vaikka toiselta suoritukselta on peruttu suostumus" in {
      resetFixtures()
      AuditLogTester.clearMessages
      val poistettavaSuoritus = TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia
      val säilytettäväSuoritus = TPO.PäätasonSuoritus.yleistenTeemaopintojenSuoritusEiArvioituEiOsasuorituksia

      // Syötä opiskeluoikeus
      val oo = postAndGetOpiskeluoikeusV2(
        TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä,
        henkilö = KoskiSpecificMockOppijat.tyhjä
      )
      oo.oid should not be empty

      // Peru suostumus käyttäjän omilla oikeuksilla
      val loginHeadersKansalainen = kansalainenLoginHeaders(KoskiSpecificMockOppijat.tyhjä.hetu)
      poistaSuostumusSuoritukselta(poistettavaSuoritus, oo, loginHeadersKansalainen)

      // Opiskeluoikeus löytyy ilman suoritusta jolta peruttiin suostumus
      val suostumusPeruttuOo = getOpiskeluoikeus(oo.oid.get)
      suostumusPeruttuOo.suoritukset.size shouldBe 1
      suostumusPeruttuOo.suoritukset.find(_.tyyppi.koodiarvo == poistettavaSuoritus.tyyppi.koodiarvo) should be(None)

      // Säilyneen opiskeluoikeuden jäljellä olevan suorituksen päivittäminen
      putOpiskeluoikeus(
        TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
          suoritukset = List(
            säilytettäväSuoritus.copy(
              arviointi = Some(List(TPO.arviointiHyväksytty))
            )
          )
        ), henkilö = KoskiSpecificMockOppijat.tyhjä
      ) {
        verifyResponseStatusOk()
      }

      // Opiskeluoikeuden historia löytyy ja se on nollattu suostumuksen peruutusta edeltäviltä versioilta
      val historiaSuostumusPeruttu = KoskiApplicationForTests.historyRepository
        .findByOpiskeluoikeusOid(oo.oid.get)(KoskiSpecificSession.systemUser).getOrElse(List.empty)
      historiaSuostumusPeruttu.size shouldBe 3
      KoskiApplicationForTests.historyRepository.findVersion(oo.oid.get, 1)(KoskiSpecificSession.systemUser).toOption.get
        .suoritukset
        .find(_.tyyppi.koodiarvo == poistettavaSuoritus.tyyppi.koodiarvo) should be(None)
      KoskiApplicationForTests.historyRepository.findVersion(oo.oid.get, 2)(KoskiSpecificSession.systemUser).toOption.get
        .suoritukset
        .find(_.tyyppi.koodiarvo == poistettavaSuoritus.tyyppi.koodiarvo) should be(None)

      val viimeisinVersio = KoskiApplicationForTests.historyRepository.findVersion(oo.oid.get, 3)(KoskiSpecificSession.systemUser).toOption.get
      viimeisinVersio.suoritukset.find(_.tyyppi.koodiarvo == poistettavaSuoritus.tyyppi.koodiarvo) should be(None)
      viimeisinVersio
        .suoritukset
        .find(_.tyyppi.koodiarvo == säilytettäväSuoritus.tyyppi.koodiarvo)
        .flatMap(_.arviointi) should be(Some(List(TPO.arviointiHyväksytty)))
    }

    "opiskeluoikeus poistuu kun sen ainoalta suoritukselta perutaan suostumus - uutta erityyppistä suoritusta ei voi lisätä poistuneelle opiskeluoikeudelle" in {
      resetFixtures()
      val poistettavaSuoritus = TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia
      val lisättäväSuoritus = TPO.PäätasonSuoritus.yleistenTeemaopintojenSuoritusEiArvioituEiOsasuorituksia

      // Syötä opiskeluoikeus
      val oo = postAndGetOpiskeluoikeusV2(
        TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
          suoritukset = List(
            poistettavaSuoritus
          )
        ), henkilö = KoskiSpecificMockOppijat.tyhjä
      )
      oo.oid should not be empty

      // Peru suostumus käyttäjän omilla oikeuksilla
      val loginHeadersKansalainen = kansalainenLoginHeaders(KoskiSpecificMockOppijat.tyhjä.hetu)
      poistaSuostumusSuoritukselta(poistettavaSuoritus, oo, loginHeadersKansalainen)

      // Säilyneen opiskeluoikeuden jäljellä olevan suorituksen päivittäminen
      putOpiskeluoikeus(
        TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
          oid = oo.oid,
          suoritukset = List(
            lisättäväSuoritus.copy(
              arviointi = Some(List(TPO.arviointiHyväksytty))
            )
          )
        ), henkilö = KoskiSpecificMockOppijat.tyhjä
      ) {
        verifyResponseStatus(404)
      }
    }

    "suoritusta jolta on peruttu suostumus, ei voi lisätä uudelleen - opiskeluoikeudella yksi suoritus" in {
      resetFixtures()
      val suoritus = TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia

      // Syötä opiskeluoikeus jolla yksi suoritus
      val oo = postAndGetOpiskeluoikeusV2(
        TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
          suoritukset = List(
            suoritus
          )
        ), henkilö = KoskiSpecificMockOppijat.tyhjä
      )
      oo.oid should not be empty

      // Peru suostumus käyttäjän omilla oikeuksilla
      val loginHeadersKansalainen = kansalainenLoginHeaders(KoskiSpecificMockOppijat.tyhjä.hetu)
      poistaSuostumusSuoritukselta(suoritus, oo, loginHeadersKansalainen)

      // Perutun suostumuksen suorituksen lisääminen takaisin samalle opiskeluoikeudelle
      putOpiskeluoikeus(
        TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
          oid = oo.oid,
          suoritukset = List(
            suoritus.copy(
              arviointi = Some(List(TPO.arviointiHyväksytty))
            )
          )
        ), henkilö = KoskiSpecificMockOppijat.tyhjä
      ) {
        verifyResponseStatus(404)
      }
    }

    "suoritusta jolta on peruttu suostumus, ei voi lisätä uudelleen - opiskeluoikeudella kaksi suoritusta" in {
      resetFixtures()
      val poistettavaJaLisättäväSuoritus = TPO
        .PäätasonSuoritus
        .yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia
      val säilyväSuoritus = TPO.PäätasonSuoritus.yleistenTeemaopintojenSuoritusEiArvioituEiOsasuorituksia

      // Syötä opiskeluoikeus jolla kaksi suoritusta
      val oo = postAndGetOpiskeluoikeusV2(
        TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
          suoritukset = List(
            poistettavaJaLisättäväSuoritus,
            säilyväSuoritus
          )
        ), henkilö = KoskiSpecificMockOppijat.tyhjä
      )
      oo.oid should not be empty

      // Peru suostumus suoritukselta käyttäjän omilla oikeuksilla
      val loginHeadersKansalainen = kansalainenLoginHeaders(KoskiSpecificMockOppijat.tyhjä.hetu)
      poistaSuostumusSuoritukselta(poistettavaJaLisättäväSuoritus, oo, loginHeadersKansalainen)

      // Perutun suostumuksen suorituksen lisääminen takaisin samalle opiskeluoikeudelle
      putOpiskeluoikeus(
        TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
          oid = oo.oid,
          suoritukset = List(
            poistettavaJaLisättäväSuoritus.copy(
              arviointi = Some(List(TPO.arviointiHyväksytty))
            ),
            säilyväSuoritus
          )
        ), henkilö = KoskiSpecificMockOppijat.tyhjä
      ) {
        verifyResponseStatus(404)
      }
    }
  }

  "Suostumuksen peruutus ja suoritusjako päätason suorituksen tasolla" - {
    "suoritusjaon tekeminen estää suostumuksen peruuttamisen suoritukselta - yksi päätason suoritus" in {
      resetFixtures()
      val suoritus = TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia

      // Syötä opiskeluoikeus jolla yksi suoritus
      val oo = postAndGetOpiskeluoikeusV2(TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä.copy(
        suoritukset = List(
          suoritus
        )
      ), henkilö = KoskiSpecificMockOppijat.tyhjä)
      oo.oid should not be empty

      // Tee suoritusjako
      val json =
        s"""[{
        "oppilaitosOid": "1.2.246.562.10.31915273374",
        "suorituksenTyyppi": "${suoritus.tyyppi.koodiarvo}",
        "koulutusmoduulinTunniste": "999907"
      }]"""

      createSuoritusjako(json, hetu = KoskiSpecificMockOppijat.tyhjä.hetu){
        verifyResponseStatusOk()
      }

      // Peru suostumus suoritukselta käyttäjän omilla oikeuksilla
      val loginHeadersKansalainen = kansalainenLoginHeaders(KoskiSpecificMockOppijat.tyhjä.hetu)
      post(
        uri = s"/api/opiskeluoikeus/suostumuksenperuutus/${oo.oid.get}",
        headers = loginHeadersKansalainen.toMap,
        params = Seq(("suorituksentyyppi", suoritus.tyyppi.koodiarvo))
      ) {
        verifyResponseStatus(403, KoskiErrorCategory.forbidden.opiskeluoikeusEiSopivaSuostumuksenPerumiselle(
          s"Opiskeluoikeuden ${oo.oid.get} annettu suostumus ei ole peruttavissa. Joko opiskeluoikeudesta on tehty suoritusjako, viranomainen on käyttänyt opiskeluoikeuden tietoja päätöksenteossa, opiskeluoikeus on tyyppiä, jonka kohdalla annettua suostumusta ei voida perua tai opiskeluoikeudelta ei löytynyt annetun syötteen tyyppistä päätason suoritusta."
        ))
      }
    }

    "suoritusjaon tekeminen estää suostumuksen peruuttamisen suoritukselta - kaksi päätason suoritusta" in {
      resetFixtures()
      val suoritus = TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia

      // Syötä opiskeluoikeus jolla yksi suoritus
      val oo = postAndGetOpiskeluoikeusV2(TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä, henkilö = KoskiSpecificMockOppijat.tyhjä)
      oo.oid should not be empty

      // Tee suoritusjako
      val json =
        s"""[{
        "oppilaitosOid": "1.2.246.562.10.31915273374",
        "suorituksenTyyppi": "${suoritus.tyyppi.koodiarvo}",
        "koulutusmoduulinTunniste": "999907"
      }]"""

      createSuoritusjako(json, hetu = KoskiSpecificMockOppijat.tyhjä.hetu){
        verifyResponseStatusOk()
      }

      // Peru suostumus suoritukselta käyttäjän omilla oikeuksilla
      val loginHeadersKansalainen = kansalainenLoginHeaders(KoskiSpecificMockOppijat.tyhjä.hetu)
      post(
        uri = s"/api/opiskeluoikeus/suostumuksenperuutus/${oo.oid.get}",
        headers = loginHeadersKansalainen.toMap,
        params = Seq(("suorituksentyyppi", suoritus.tyyppi.koodiarvo))
      ) {
        verifyResponseStatus(403, KoskiErrorCategory.forbidden.opiskeluoikeusEiSopivaSuostumuksenPerumiselle(
          s"Opiskeluoikeuden ${oo.oid.get} annettu suostumus ei ole peruttavissa. Joko opiskeluoikeudesta on tehty suoritusjako, viranomainen on käyttänyt opiskeluoikeuden tietoja päätöksenteossa, opiskeluoikeus on tyyppiä, jonka kohdalla annettua suostumusta ei voida perua tai opiskeluoikeudelta ei löytynyt annetun syötteen tyyppistä päätason suoritusta."
        ))
      }
    }

    "suoritusjaon tekeminen onnistuu toiselle suoritukselle vaikka toiselta suoritukselta on peruttu suostumus" in {
      resetFixtures()
      val poistettavaSuoritus = TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia
      val jaettavaSuoritus = TPO.PäätasonSuoritus.yleistenTeemaopintojenSuoritusEiArvioituEiOsasuorituksia

      // Syötä opiskeluoikeus jolla kaksi suoritusta
      val oo = postAndGetOpiskeluoikeusV2(TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä, henkilö = KoskiSpecificMockOppijat.tyhjä)
      oo.oid should not be empty

      // Tee suoritusjako
      val json =
        s"""[{
        "oppilaitosOid": "1.2.246.562.10.31915273374",
        "suorituksenTyyppi": "${jaettavaSuoritus.tyyppi.koodiarvo}",
        "koulutusmoduulinTunniste": "999907"
      }]"""

      createSuoritusjako(json, hetu = KoskiSpecificMockOppijat.tyhjä.hetu){
        verifyResponseStatusOk()
      }

      // Peru suostumus suoritukselta käyttäjän omilla oikeuksilla
      val loginHeadersKansalainen = kansalainenLoginHeaders(KoskiSpecificMockOppijat.tyhjä.hetu)
      poistaSuostumusSuoritukselta(poistettavaSuoritus, oo, loginHeadersKansalainen)
    }

    "suoritusjakoon ei sisälly rinnakkaisen opiskeluoikeuden vastaava suoritus ja rinnakkaisen opiskeluoikeuden suoritukselta voi perua suostumuksen" in {
      resetFixtures()
      val poistettavaSuoritus = TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia

      // Syötä opiskeluoikeudet
      val oo = postAndGetOpiskeluoikeusV2(TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä, henkilö = KoskiSpecificMockOppijat.tyhjä)
      oo.oid should not be empty

      val oo2 = postAndGetOpiskeluoikeusV2(TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä, henkilö = KoskiSpecificMockOppijat.tyhjä)
      oo2.oid should not be empty
      oo.oid should not be oo2.oid

      // Tee suoritusjako
      val json =
        s"""[{
        "opiskeluoikeusOid": "${oo.oid.get}",
        "oppilaitosOid": "1.2.246.562.10.31915273374",
        "suorituksenTyyppi": "${poistettavaSuoritus.tyyppi.koodiarvo}",
        "koulutusmoduulinTunniste": "999907"
      }]"""

      var secret = ""
      createSuoritusjako(json, hetu = KoskiSpecificMockOppijat.tyhjä.hetu){
        verifyResponseStatusOk()
        secret = JsonSerializer.parse[Suoritusjako](response.body).secret
      }

      val suoritusjakoOppija = getSuoritusjakoOppija(secret)
      suoritusjakoOppija.opiskeluoikeudet.size shouldBe 1
      suoritusjakoOppija.opiskeluoikeudet.head.oid.get shouldBe oo.oid.get

      // Peru suostumus jakamattomalta suoritukselta käyttäjän omilla oikeuksilla
      val loginHeadersKansalainen = kansalainenLoginHeaders(KoskiSpecificMockOppijat.tyhjä.hetu)
      poistaSuostumusSuoritukselta(poistettavaSuoritus, oo2, loginHeadersKansalainen)
    }

    "suoritusjaon olemassaolon tarkistus toimii päätason suorituksen tasolla" in {
      resetFixtures()
      val jaettavaSuoritus = TPO.PäätasonSuoritus.yleistenYhteistenOpintojenSuoritusEiArvioituEiOsasuorituksia
      val eiJaettuSuoritus = TPO.PäätasonSuoritus.yleistenTeemaopintojenSuoritusEiArvioituEiOsasuorituksia

      // Syötä opiskeluoikeudet
      val oo = postAndGetOpiskeluoikeusV2(TPO.Opiskeluoikeus.aloitettuYleinenOppimäärä, henkilö = KoskiSpecificMockOppijat.tyhjä)
      oo.oid should not be empty

      // Tee suoritusjako
      val json =
        s"""[{
        "opiskeluoikeusOid": "${oo.oid.get}",
        "oppilaitosOid": "1.2.246.562.10.31915273374",
        "suorituksenTyyppi": "${jaettavaSuoritus.tyyppi.koodiarvo}",
        "koulutusmoduulinTunniste": "999907"
      }]"""

      createSuoritusjako(json, hetu = KoskiSpecificMockOppijat.tyhjä.hetu){
        verifyResponseStatusOk()
      }

      // Suoritusjako on tehty jaettavaan suoritukseen
      val loginHeadersKansalainen = kansalainenLoginHeaders(KoskiSpecificMockOppijat.tyhjä.hetu)
      post(
        uri = s"/api/opiskeluoikeus/suostumuksenperuutus/suoritusjakoTehty/${oo.oid.get}",
        headers = loginHeadersKansalainen.toMap,
        params = Seq(("suorituksentyyppi", jaettavaSuoritus.tyyppi.koodiarvo))
      ) {
        response.body shouldBe """{"tehty":true}"""
      }

      // Suoritusjako ei ole tehty toiseen suoritukseen
      post(
        uri = s"/api/opiskeluoikeus/suostumuksenperuutus/suoritusjakoTehty/${oo.oid.get}",
        headers = loginHeadersKansalainen.toMap,
        params = Seq(("suorituksentyyppi", eiJaettuSuoritus.tyyppi.koodiarvo))
      ) {
        response.body shouldBe """{"tehty":false}"""
      }
    }
  }

  private def poistaSuostumusSuoritukselta(
    poistettavaSuoritus: TaiteenPerusopetuksenYleisenOppimääränYhteistenOpintojenSuoritus,
    oo: TaiteenPerusopetuksenOpiskeluoikeus,
    loginHeadersKansalainen: List[(String, String)]
  ): Unit = {
    post(
      uri = s"/api/opiskeluoikeus/suostumuksenperuutus/${oo.oid.get}",
      headers = loginHeadersKansalainen.toMap,
      params = Seq(("suorituksentyyppi", poistettavaSuoritus.tyyppi.koodiarvo))
    ) {
      verifyResponseStatusOk()
    }
  }

  def mockKoskiValidator(config: Config) = {
    new KoskiValidator(
      KoskiApplicationForTests.organisaatioRepository,
      KoskiApplicationForTests.possu,
      KoskiApplicationForTests.henkilöRepository,
      KoskiApplicationForTests.ePerusteetValidator,
      KoskiApplicationForTests.ePerusteetFiller,
      KoskiApplicationForTests.validatingAndResolvingExtractor,
      KoskiApplicationForTests.suostumuksenPeruutusService,
      KoskiApplicationForTests.koodistoViitePalvelu,
      config
    )
  }

  private def postAndGetOpiskeluoikeusV2(
    oo: TaiteenPerusopetuksenOpiskeluoikeus,
    henkilö: Henkilö = defaultHenkilö,
    headers: Headers = authHeaders() ++ jsonContent
  ): TaiteenPerusopetuksenOpiskeluoikeus = {
    val jsonString = JsonMethods.pretty(makeOppija(henkilö, List(oo)))
    post("api/v2/oppija", body = jsonString, headers = headers) {
      verifyResponseStatusOk()
      val response = readPutOppijaResponse
      response.opiskeluoikeudet.size shouldBe 1
      getOpiskeluoikeus(readPutOppijaResponse.opiskeluoikeudet.head.oid).asInstanceOf[TaiteenPerusopetuksenOpiskeluoikeus]
    }
  }
}
