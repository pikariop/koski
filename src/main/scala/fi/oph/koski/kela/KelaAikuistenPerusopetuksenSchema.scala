package fi.oph.koski.kela

import fi.oph.koski.koskiuser.Rooli
import fi.oph.koski.schema
import fi.oph.koski.schema.OpiskeluoikeudenTyyppi
import fi.oph.koski.schema.annotation.{KoodistoKoodiarvo, SensitiveData}
import fi.oph.scalaschema.annotation.{Description, Title}

import java.time.{LocalDate, LocalDateTime}

@Title("Aikuisten perusopetuksen opiskeluoikeus")
@Description("Aikuisten perusopetuksen opiskeluoikeus")
case class KelaAikuistenPerusopetuksenOpiskeluoikeus(
  oid: Option[String],
  versionumero: Option[Int],
  aikaleima: Option[LocalDateTime],
  oppilaitos: Option[Oppilaitos],
  koulutustoimija: Option[Koulutustoimija],
  sisältyyOpiskeluoikeuteen: Option[SisältäväOpiskeluoikeus],
  tila: KelaOpiskeluoikeudenTila,
  suoritukset: List[KelaAikuistenPerusopetuksenSuoritus],
  lisätiedot: Option[KelaAikuistenPerusopetuksenOpiskeluoikeudenLisätiedot],
  @KoodistoKoodiarvo(OpiskeluoikeudenTyyppi.aikuistenperusopetus.koodiarvo)
  tyyppi: schema.Koodistokoodiviite,
  organisaatioHistoria: Option[List[OrganisaatioHistoria]],
  organisaatiohistoria: Option[List[OrganisaatioHistoria]]
) extends KelaOpiskeluoikeus {
  override def alkamispäivä: Option[LocalDate] = super.alkamispäivä
  override def päättymispäivä: Option[LocalDate] = super.päättymispäivä
  override def arvioituPäättymispäivä = None
  def withEmptyArvosana: KelaAikuistenPerusopetuksenOpiskeluoikeus = copy(
    suoritukset = suoritukset.map(_.withEmptyArvosana)
  )
  override def withOrganisaatiohistoria: KelaOpiskeluoikeus = copy(
    organisaatioHistoria = organisaatiohistoria,
    organisaatiohistoria = None
  )
}

case class KelaAikuistenPerusopetuksenOpiskeluoikeudenLisätiedot(
  sisäoppilaitosmainenMajoitus: Option[List[KelaAikajakso]],
  ulkomaanjaksot: Option[List[Ulkomaanjakso]],
  majoitusetu: Option[KelaAikajakso],
  ulkomailla: Option[KelaAikajakso],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  tehostetunTuenPäätös: Option[KelaTehostetunTuenPäätös],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  tehostetunTuenPäätökset: Option[List[KelaTehostetunTuenPäätös]],
  maksuttomuus: Option[List[KelaMaksuttomuus]],
  oikeuttaMaksuttomuuteenPidennetty: Option[List[KelaOikeuttaMaksuttomuuteenPidennetty]]
) extends OpiskeluoikeudenLisätiedot

@Title("Aikuisten perusopetuksen suoritus")
case class KelaAikuistenPerusopetuksenSuoritus(
  koulutusmoduuli: KelaAikuistenPerusopetuksenSuorituksenKoulutusmoduuli,
  toimipiste: Option[Toimipiste],
  vahvistus: Option[Vahvistus],
  osasuoritukset: Option[List[KelaAikuistenPerusopetuksenOsasuoritus]],
  tyyppi: schema.Koodistokoodiviite,
  tila: Option[KelaKoodistokoodiviite],
) extends Suoritus {
  def withEmptyArvosana: KelaAikuistenPerusopetuksenSuoritus = copy(
    osasuoritukset = osasuoritukset.map(_.map(_.withEmptyArvosana))
  )
}

@Title("Aikuisten perusopetuksen osasuoritus")
case class KelaAikuistenPerusopetuksenOsasuoritus(
  koulutusmoduuli: KelaAikuistenPerusopetuksenOsasuorituksenKoulutusmoduuli,
  arviointi: Option[List[KelaPerusopetuksenOsasuorituksenArviointi]],
  osasuoritukset: Option[List[KelaAikuistenPerusopetuksenOsasuoritus]],
  tyyppi: schema.Koodistokoodiviite,
  tila: Option[KelaKoodistokoodiviite],
  tunnustettu: Option[OsaamisenTunnustaminen],
) extends Osasuoritus {
  def withEmptyArvosana: KelaAikuistenPerusopetuksenOsasuoritus = copy(
    arviointi = arviointi.map(_.map(_.withEmptyArvosana)),
    osasuoritukset = osasuoritukset.map(_.map(_.withEmptyArvosana))
  )
}

case class KelaAikuistenPerusopetuksenSuorituksenKoulutusmoduuli(
  tunniste: KelaKoodistokoodiviite,
  laajuus: Option[KelaLaajuus],
  perusteenDiaarinumero: Option[String],
  koulutustyyppi: Option[KelaKoodistokoodiviite],
  pakollinen: Option[Boolean],
  kieli: Option[KelaKoodistokoodiviite],
) extends SuorituksenKoulutusmoduuli

case class KelaAikuistenPerusopetuksenOsasuorituksenKoulutusmoduuli(
  tunniste: KelaKoodistokoodiviite,
  laajuus: Option[KelaLaajuus],
  pakollinen: Option[Boolean],
  kieli: Option[KelaKoodistokoodiviite],
) extends OsasuorituksenKoulutusmoduuli