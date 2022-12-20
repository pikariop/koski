package fi.oph.koski.validation

import com.typesafe.config.Config
import fi.oph.koski.http.{HttpStatus, KoskiErrorCategory}
import fi.oph.koski.schema._

object TaiteenPerusopetusValidation {

  def validateOpiskeluoikeus(config: Config)(oo: KoskeenTallennettavaOpiskeluoikeus): HttpStatus = {
    oo match {
      case oo: TaiteenPerusopetuksenOpiskeluoikeus =>
        HttpStatus.fold(
          validateOpintotasot(oo),
          validateTaiteenalat(oo),
          validateHyväksytystiSuoritettuPäätasonSuoritus(oo),
          validateSuoritustenLaajuus(oo),
        )
      case _ => HttpStatus.ok
    }
  }

  def validateOpintotasot(oo: TaiteenPerusopetuksenOpiskeluoikeus): HttpStatus = {
    val yleisenOppimääränOpintotasojenSuoritustyypit = List(
      "taiteenperusopetuksenyleisenoppimaaranyhteisetopinnot",
      "taiteenperusopetuksenyleisenoppimaaranteemaopinnot"
    )
    val laajanOppimääränOpintotasojenSuoritustyypit = List(
      "taiteenperusopetuksenlaajanoppimaaranperusopinnot",
      "taiteenperusopetuksenlaajanoppimaaransyventavatopinnot"
    )

    oo.oppimäärä.koodiarvo match {
      case "yleinenoppimaara" => HttpStatus.validate(
        oo.suoritukset.map(_.tyyppi.koodiarvo).forall(t => yleisenOppimääränOpintotasojenSuoritustyypit.contains(t))
      )(
        KoskiErrorCategory.badRequest.validation.rakenne.tpoVääräOpintotaso()
      )
      case "laajaoppimaara" => HttpStatus.validate(
        oo.suoritukset.map(_.tyyppi.koodiarvo).forall(t => laajanOppimääränOpintotasojenSuoritustyypit.contains(t))
      )(
        KoskiErrorCategory.badRequest.validation.rakenne.tpoVääräOpintotaso()
      )
      case _ => KoskiErrorCategory.badRequest.validation.rakenne.tpoVääräOpintotaso("Opiskeluoikeuden oppimäärä on tuntematon.")
    }
  }

  def validateTaiteenalat(oo: TaiteenPerusopetuksenOpiskeluoikeus): HttpStatus = {
    HttpStatus.validate(
      oo.suoritukset.map(_.koulutusmoduuli.taiteenala.koodiarvo).distinct.size == 1
    )(
      KoskiErrorCategory.badRequest.validation.rakenne.tpoEriTaiteenalat()
    )
  }

  def validateHyväksytystiSuoritettuPäätasonSuoritus(oo: TaiteenPerusopetuksenOpiskeluoikeus): HttpStatus = {
    def valmiitOppimäärätLöytyvät: Boolean = oo.suoritukset.forall(s => s.valmis)

    if (oo.tila.opiskeluoikeusjaksot.last.hyväksytystiSuoritettu && !valmiitOppimäärätLöytyvät) {
      KoskiErrorCategory.badRequest.validation.tila.vahvistusPuuttuu("Suoritukselta puuttuu vahvistus, vaikka opiskeluoikeus on tilassa Hyväksytysti suoritettu")
    } else {
      HttpStatus.ok
    }
  }

  def validateSuoritustenLaajuus(oo: TaiteenPerusopetuksenOpiskeluoikeus): HttpStatus = {
    def laajuusVähintään(min: Double, k: Koulutusmoduuli, virheIlmoitus: HttpStatus): HttpStatus =
      HttpStatus.validate(min - 0.0001 < k.laajuusArvo(default = 0.0)) {
        virheIlmoitus
      }

    HttpStatus.fold(
      oo.suoritukset.filter(_.vahvistettu).map {
        case s: TaiteenPerusopetuksenYleisenOppimääränYhteistenOpintojenSuoritus => laajuusVähintään(
          min = 11.11,
          k = s.koulutusmoduuli,
          KoskiErrorCategory.badRequest.validation.laajuudet.taiteenPerusopetuksenLaajuus("Yleisen oppimäärän yhteisten opintojen laajuus on oltava vähintään 11.11 opintopistettä.")
        )
        case s: TaiteenPerusopetuksenYleisenOppimääränTeemaopintojenSuoritus => laajuusVähintään(
          min = 7.41,
          k = s.koulutusmoduuli,
          KoskiErrorCategory.badRequest.validation.laajuudet.taiteenPerusopetuksenLaajuus("Yleisen oppimäärän teemaopintojen laajuus on oltava vähintään 7.41 opintopistettä.")
        )
        case s: TaiteenPerusopetuksenLaajanOppimääränPerusopintojenSuoritus => laajuusVähintään(
          min = 29.63,
          k = s.koulutusmoduuli,
          KoskiErrorCategory.badRequest.validation.laajuudet.taiteenPerusopetuksenLaajuus("Laajan oppimäärän perusopintojen laajuus on oltava vähintään 29.63 opintopistettä.")
        )
        case s: TaiteenPerusopetuksenLaajanOppimääränSyventävienOpintojenSuoritus => laajuusVähintään(
          min = 18.52,
          k = s.koulutusmoduuli,
          KoskiErrorCategory.badRequest.validation.laajuudet.taiteenPerusopetuksenLaajuus("Laajan oppimäärän syventävien opintojen laajuus on oltava vähintään 18.52 opintopistettä.")
        )
      }
    )
  }
}