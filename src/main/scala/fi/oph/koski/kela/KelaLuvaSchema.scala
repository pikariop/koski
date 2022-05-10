package fi.oph.koski.kela

import fi.oph.koski.schema
import fi.oph.koski.schema.{LocalizedString, OpiskeluoikeudenTyyppi}
import fi.oph.koski.schema.annotation.KoodistoKoodiarvo
import fi.oph.scalaschema.annotation.{Description, Title}

import java.time.{LocalDate, LocalDateTime}

@Title("Lukioon valmistavan koulutuksen opiskeluoikeus")
@Description("Lukioon valmistava koulutus (LUVA)")
case class KelaLuvaOpiskeluoikeus(
  oid: Option[String],
  versionumero: Option[Int],
  aikaleima: Option[LocalDateTime],
  oppilaitos: Option[Oppilaitos],
  koulutustoimija: Option[Koulutustoimija],
  sisältyyOpiskeluoikeuteen: Option[Sisältäväopiskeluoikeus],
  arvioituPäättymispäivä: Option[LocalDate],
  tila: KelaOpiskeluoikeudenTila,
  suoritukset: List[KelaLuvaPäätasonSuoritus],
  lisätiedot: Option[KelaLuvaOpiskeluoikeudenLisätiedot],
  @KoodistoKoodiarvo(OpiskeluoikeudenTyyppi.luva.koodiarvo)
  tyyppi: schema.Koodistokoodiviite,
  organisaatioHistoria: Option[List[OrganisaatioHistoria]]
) extends KelaOpiskeluoikeus {
  override def alkamispäivä: Option[LocalDate] = super.alkamispäivä
  override def päättymispäivä: Option[LocalDate] = super.päättymispäivä
  def withEmptyArvosana: KelaLuvaOpiskeluoikeus = copy(
    suoritukset = suoritukset.map(_.withEmptyArvosana)
  )
}

case class KelaLuvaOpiskeluoikeudenLisätiedot(
  sisäoppilaitosmainenMajoitus: Option[List[schema.Aikajakso]],
  ulkomaanjaksot: Option[List[Ulkomaanjakso]],
  ulkomainenVaihtoopiskelija: Option[Boolean],
  maksuttomuus: Option[List[schema.Maksuttomuus]],
  oikeuttaMaksuttomuuteenPidennetty: Option[List[schema.OikeuttaMaksuttomuuteenPidennetty]]
) extends OpiskeluoikeudenLisätiedot

@Title("Lukioon valmistavan koulutuksen suoritus")
case class KelaLuvaPäätasonSuoritus(
  koulutusmoduuli: KelaLuvaSuorituksenKoulutusmoduuli,
  toimipiste: Option[Toimipiste],
  oppimäärä: Option[schema.Koodistokoodiviite],
  vahvistus: Option[Vahvistus],
  osasuoritukset: Option[List[KelaLuvaOsasuoritus]],
  tyyppi: schema.Koodistokoodiviite,
  tila: Option[schema.Koodistokoodiviite]
) extends Suoritus {
  def withEmptyArvosana = copy(
    osasuoritukset = osasuoritukset.map(_.map(_.withEmptyArvosana))
  )
}

@Title("Lukioon valmistavan koulutuksen osasuoritus")
case class KelaLuvaOsasuoritus(
  koulutusmoduuli: KelaLuvaOsasuorituksenKoulutusmoduuli,
  arviointi: Option[List[KelaLuvaOsasuorituksenArvionti]],
  osasuoritukset: Option[List[KelaLuvaOsasuoritus]],
  tyyppi: schema.Koodistokoodiviite,
  tila: Option[schema.Koodistokoodiviite],
  tunnustettu: Option[KelaLuvaOsaamisenTunnustaminen],
  suoritettuLukiodiplomina: Option[Boolean],
  suoritettuSuullisenaKielikokeena: Option[Boolean]
) extends Osasuoritus {
  def withEmptyArvosana: KelaLuvaOsasuoritus = copy(
    arviointi = arviointi.map(_.map(_.withEmptyArvosana)),
    osasuoritukset = osasuoritukset.map(_.map(_.withEmptyArvosana))
  )
}

case class KelaLuvaOsasuorituksenArvionti(
  arvosana: Option[schema.Koodistokoodiviite],
  hyväksytty: Option[Boolean],
  päivä: Option[LocalDate]
) extends OsasuorituksenArvionti {
  def withEmptyArvosana: KelaLuvaOsasuorituksenArvionti = copy(
    arvosana = None,
    hyväksytty = arvosana.map(schema.YleissivistävänKoulutuksenArviointi.hyväksytty)
  )
}

case class KelaLuvaOsaamisenTunnustaminen(
  osaaminen: Option[KelaLuvaOsasuoritus],
  selite: LocalizedString,
  rahoituksenPiirissä: Boolean
) extends OsaamisenTunnustaminen

case class KelaLuvaSuorituksenKoulutusmoduuli(
  tunniste: KelaKoodistokoodiviite,
  laajuus: Option[schema.Laajuus],
  perusteenDiaarinumero: Option[String],
  koulutustyyppi: Option[schema.Koodistokoodiviite],
) extends SuorituksenKoulutusmoduuli

case class KelaLuvaOsasuorituksenKoulutusmoduuli(
  tunniste: KelaKoodistokoodiviite,
  laajuus: Option[schema.Laajuus],
  pakollinen: Option[Boolean],
  kieli: Option[schema.Koodistokoodiviite],
  kurssinTyyppi: Option[schema.Koodistokoodiviite],
  oppimäärä: Option[schema.Koodistokoodiviite]
) extends OsasuorituksenKoulutusmoduuli
