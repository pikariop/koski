package fi.oph.koski.kela

import fi.oph.koski.koskiuser.Rooli
import fi.oph.koski.schema
import fi.oph.koski.schema.{LocalizedString, OpiskeluoikeudenTyyppi}
import fi.oph.koski.schema.annotation.{KoodistoKoodiarvo, SensitiveData}
import fi.oph.scalaschema.annotation.{Description, OnlyWhen, Title}

import java.time.{LocalDate, LocalDateTime}

@Title("Aikuisten perusopetuksen opiskeluoikeus")
@Description("Aikuisten perusopetuksen opiskeluoikeus")
case class KelaAikuistenPerusopetuksenOpiskeluoikeus(
  oid: Option[String],
  versionumero: Option[Int],
  aikaleima: Option[LocalDateTime],
  oppilaitos: Option[Oppilaitos],
  koulutustoimija: Option[Koulutustoimija],
  sisältyyOpiskeluoikeuteen: Option[Sisältäväopiskeluoikeus],
  tila: KelaOpiskeluoikeudenTila,
  suoritukset: List[KelaaikuistenPerusopetuksenSuoritus],
  lisätiedot: Option[KelaAikuistenPerusopetuksenOpiskeluoikeudenLisätiedot],
  @KoodistoKoodiarvo(OpiskeluoikeudenTyyppi.aikuistenperusopetus.koodiarvo)
  tyyppi: schema.Koodistokoodiviite,
  organisaatioHistoria: Option[List[OrganisaatioHistoria]]
) extends KelaOpiskeluoikeus {
  override def alkamispäivä: Option[LocalDate] = super.alkamispäivä
  override def päättymispäivä: Option[LocalDate] = super.päättymispäivä
  override def arvioituPäättymispäivä = None
  def withEmptyArvosana: KelaAikuistenPerusopetuksenOpiskeluoikeus = copy(
    suoritukset = suoritukset.map(_.withEmptyArvosana)
  )
}

case class KelaAikuistenPerusopetuksenOpiskeluoikeudenLisätiedot(
  sisäoppilaitosmainenMajoitus: Option[List[schema.Aikajakso]],
  ulkomaanjaksot: Option[List[Ulkomaanjakso]],
  majoitusetu: Option[schema.Aikajakso],
  ulkomailla: Option[schema.Aikajakso],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  tehostetunTuenPäätös: Option[schema.TehostetunTuenPäätös],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  tehostetunTuenPäätökset: Option[List[schema.TehostetunTuenPäätös]],
  maksuttomuus: Option[List[schema.Maksuttomuus]],
  oikeuttaMaksuttomuuteenPidennetty: Option[List[schema.OikeuttaMaksuttomuuteenPidennetty]]
) extends OpiskeluoikeudenLisätiedot

@Title("Aikuisten perusopetuksen suoritus")
case class KelaAikuistenPerusopetuksenPäätasonSuoritus(
  koulutusmoduuli: KelaAikuistenPerusopetuksenSuorituksenKoulutusmoduuli,
  suoritustapa: Option[KelaKoodistokoodiviite],
  toimipiste: Option[Toimipiste],
  vahvistus: Option[Vahvistus],
  osasuoritukset: Option[List[KelaAikuistenPerusopetuksenOsasuoritus]],
  @KoodistoKoodiarvo("aikuistenperusopetuksenoppimaaranalkuvaihe")
  @KoodistoKoodiarvo("aikuistenperusopetuksenoppimaara")
  tyyppi: schema.Koodistokoodiviite,
  tila: Option[schema.Koodistokoodiviite],
) extends KelaaikuistenPerusopetuksenSuoritus {
  def withEmptyArvosana: KelaAikuistenPerusopetuksenPäätasonSuoritus = copy(
    osasuoritukset = osasuoritukset.map(_.map(_.withEmptyArvosana))
  )
}

@Title("Aikuisten perusopetuksen oppiaineen oppimäärän suoritus")
case class KelaAikuistenPerusopetuksenOppiaineenOppimääränSuoritus(
  koulutusmoduuli: KelaAikuistenPerusopetuksenSuorituksenKoulutusmoduuli,
  toimipiste: Option[Toimipiste],
  vahvistus: Option[Vahvistus],
  osasuoritukset: Option[List[KelaAikuistenPerusopetuksenOsasuoritus]],
  @KoodistoKoodiarvo("perusopetuksenoppiaineenoppimaara")
  tyyppi: schema.Koodistokoodiviite,
  tila: Option[schema.Koodistokoodiviite],
) extends KelaaikuistenPerusopetuksenSuoritus {
  def withEmptyArvosana: KelaAikuistenPerusopetuksenOppiaineenOppimääränSuoritus = copy(
    osasuoritukset = osasuoritukset.map(_.map(_.withEmptyArvosana))
  )
}

trait KelaaikuistenPerusopetuksenSuoritus extends Suoritus {
  def withEmptyArvosana: KelaaikuistenPerusopetuksenSuoritus
}

@Title("Aikuisten perusopetuksen osasuoritus")
case class KelaAikuistenPerusopetuksenOsasuoritus(
  koulutusmoduuli: KelaAikuistenPerusopetuksenOsasuorituksenKoulutusmoduuli,
  arviointi: Option[List[KelaPerusopetuksenOsasuorituksenArvionti]],
  osasuoritukset: Option[List[KelaAikuistenPerusopetuksenOsasuoritus]],
  tyyppi: schema.Koodistokoodiviite,
  tila: Option[schema.Koodistokoodiviite],
  tunnustettu: Option[KelaAikuistenPerusopetuksenOsaamisenTunnustaminen],
) extends Osasuoritus {
  def withEmptyArvosana: KelaAikuistenPerusopetuksenOsasuoritus = copy(
    arviointi = arviointi.map(_.map(_.withEmptyArvosana)),
    osasuoritukset = osasuoritukset.map(_.map(_.withEmptyArvosana))
  )
}

case class KelaAikuistenPerusopetuksenOsaamisenTunnustaminen(
  osaaminen: Option[KelaAikuistenPerusopetuksenOsasuoritus],
  selite: LocalizedString,
  rahoituksenPiirissä: Boolean
) extends OsaamisenTunnustaminen

case class KelaAikuistenPerusopetuksenSuorituksenKoulutusmoduuli(
  tunniste: KelaKoodistokoodiviite,
  laajuus: Option[schema.Laajuus],
  perusteenDiaarinumero: Option[String],
  koulutustyyppi: Option[schema.Koodistokoodiviite],
  pakollinen: Option[Boolean],
  kieli: Option[schema.Koodistokoodiviite],
) extends SuorituksenKoulutusmoduuli

case class KelaAikuistenPerusopetuksenOsasuorituksenKoulutusmoduuli(
  tunniste: KelaKoodistokoodiviite,
  laajuus: Option[schema.Laajuus],
  pakollinen: Option[Boolean],
  kieli: Option[schema.Koodistokoodiviite],
) extends OsasuorituksenKoulutusmoduuli
