package fi.oph.koski.kela

import fi.oph.koski.koskiuser.Rooli
import fi.oph.koski.schema
import fi.oph.koski.schema.{OmanÄidinkielenOpinnotLaajuusVuosiviikkotunteina, OpiskeluoikeudenTyyppi}
import fi.oph.koski.schema.annotation.{KoodistoKoodiarvo, SensitiveData}
import fi.oph.scalaschema.annotation.{Description, Discriminator, OnlyWhen, Title}

import java.time.{LocalDate, LocalDateTime}

@Title("Perusopetuksen opiskeluoikeus")
@Description("Perusopetuksen opiskeluoikeus")
case class KelaPerusopetuksenOpiskeluoikeus(
  oid: Option[String],
  versionumero: Option[Int],
  aikaleima: Option[LocalDateTime],
  oppilaitos: Option[Oppilaitos],
  koulutustoimija: Option[Koulutustoimija],
  sisältyyOpiskeluoikeuteen: Option[SisältäväOpiskeluoikeus],
  tila: KelaOpiskeluoikeudenTila,
  suoritukset: List[KelaPerusopetuksenSuoritus],
  lisätiedot: Option[KelaPerusopetuksenOpiskeluoikeudenLisätiedot],
  @KoodistoKoodiarvo(OpiskeluoikeudenTyyppi.perusopetus.koodiarvo)
  tyyppi: schema.Koodistokoodiviite,
  organisaatioHistoria: Option[List[OrganisaatioHistoria]],
  organisaatiohistoria: Option[List[OrganisaatioHistoria]]
) extends KelaOpiskeluoikeus {
  override def alkamispäivä: Option[LocalDate] = super.alkamispäivä
  override def päättymispäivä: Option[LocalDate] = super.päättymispäivä
  override def arvioituPäättymispäivä = None
  def withHyväksyntämerkinnälläKorvattuArvosana: KelaPerusopetuksenOpiskeluoikeus = copy(
    suoritukset = suoritukset.map(_.withHyväksyntämerkinnälläKorvattuArvosana)
  )
  override def withOrganisaatiohistoria: KelaOpiskeluoikeus = copy(
    organisaatioHistoria = organisaatiohistoria,
    organisaatiohistoria = None
  )
}

case class KelaPerusopetuksenOpiskeluoikeudenLisätiedot(
  sisäoppilaitosmainenMajoitus: Option[List[KelaAikajakso]],
  // TODO: TOR-1732: Päätietomallissa ei ole näin monimutkaista ulkomaanjakso-rakennetta perusopetuksessa, joten korvaa simppelimmällä Kelallekin?
  ulkomaanjaksot: Option[List[Ulkomaanjakso]],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  koulukoti: Option[List[KelaAikajakso]],
  majoitusetu: Option[KelaAikajakso],
  ulkomailla: Option[KelaAikajakso],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  tehostetunTuenPäätös: Option[KelaTehostetunTuenPäätös],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  tehostetunTuenPäätökset: Option[List[KelaTehostetunTuenPäätös]],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  joustavaPerusopetus: Option[KelaAikajakso],
) extends OpiskeluoikeudenLisätiedot

@Title("Perusopetuksen suoritus")
case class KelaPerusopetuksenSuoritus(
  koulutusmoduuli: KelaPerusopetuksenSuorituksenKoulutusmoduuli,
  toimipiste: Option[Toimipiste],
  vahvistus: Option[Vahvistus],
  osasuoritukset: Option[List[KelaPerusopetuksenOsasuoritus]],
  tyyppi: schema.Koodistokoodiviite,
  tila: Option[KelaKoodistokoodiviite],
  alkamispäivä: Option[LocalDate],
  jääLuokalle: Option[Boolean],
  // TODO: TOR-1732: Töstä varmasti pitäisi poistaa myös tarkka arvosana ja korvata hyväksyntätietoina
  omanÄidinkielenOpinnot: Option[OmanÄidinkielenOpinnotLaajuusVuosiviikkotunteina]
) extends Suoritus {
  def withHyväksyntämerkinnälläKorvattuArvosana: KelaPerusopetuksenSuoritus = copy(
    osasuoritukset = osasuoritukset.map(_.map(_.withHyväksyntämerkinnälläKorvattuArvosana))
  )
}

@Title("Perusopetuksen osasuoritus")
case class KelaPerusopetuksenOsasuoritus(
  koulutusmoduuli: KelaPerusopetuksenOsasuorituksenKoulutusmoduuli,
  arviointi: Option[List[KelaPerusopetuksenOsasuorituksenArviointi]],
  tyyppi: schema.Koodistokoodiviite,
  tila: Option[KelaKoodistokoodiviite],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  yksilöllistettyOppimäärä: Option[Boolean]
) extends Osasuoritus with YksilöllistettyOppimäärä {
  def withHyväksyntämerkinnälläKorvattuArvosana: KelaPerusopetuksenOsasuoritus = copy(
    arviointi = arviointi.map(_.map(_.withHyväksyntämerkinnälläKorvattuArvosana))
  )
}

case class KelaPerusopetuksenOsasuorituksenArviointi(
  arvosana: Option[schema.Koodistokoodiviite],
  hyväksytty: Option[Boolean],
  päivä: Option[LocalDate]
) extends OsasuorituksenArviointi {
  def withHyväksyntämerkinnälläKorvattuArvosana: KelaPerusopetuksenOsasuorituksenArviointi = copy(
    arvosana = None,
    hyväksytty = arvosana.map(schema.YleissivistävänKoulutuksenArviointi.hyväksytty)
  )
}

case class KelaPerusopetuksenSuorituksenKoulutusmoduuli(
  tunniste: KelaKoodistokoodiviite,
  laajuus: Option[KelaLaajuus],
  perusteenDiaarinumero: Option[String],
  koulutustyyppi: Option[KelaKoodistokoodiviite],
  pakollinen: Option[Boolean],
  kieli: Option[KelaKoodistokoodiviite]
) extends SuorituksenKoulutusmoduuli

case class KelaPerusopetuksenOsasuorituksenKoulutusmoduuli(
  tunniste: KelaKoodistokoodiviite,
  laajuus: Option[KelaLaajuus],
  pakollinen: Option[Boolean],
  kieli: Option[KelaKoodistokoodiviite]
) extends OsasuorituksenKoulutusmoduuli
