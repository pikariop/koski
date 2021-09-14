package fi.oph.koski.valpas.valpasrepository

import fi.oph.koski.schema.annotation.{KoodistoKoodiarvo, KoodistoUri}
import fi.oph.koski.schema.{Koodistokoodiviite, OrganisaatioWithOid}
import fi.oph.koski.valpas.yhteystiedot.ValpasYhteystietojenAlkuperä

import java.time.LocalDateTime
import fi.oph.koski.valpas.ValpasKuntailmoitusLaajatTiedotLisätiedoilla
import fi.oph.koski.valpas.opiskeluoikeusrepository.{ValpasOpiskeluoikeus, ValpasOpiskeluoikeusLaajatTiedot, ValpasOppijaLaajatTiedot, ValpasRajapäivätService}

case class ValpasKuntailmoitusLaajatTiedotJaOppijaOid(
  oppijaOid: String,
  kuntailmoitus: ValpasKuntailmoitusLaajatTiedot
)

trait ValpasKuntailmoitus {
  def id: Option[String] // TODO: Scala-schemasta puuttuu UUID-tuki :(
  def tekijä: ValpasKuntailmoituksenTekijä
  def kunta: OrganisaatioWithOid // Koska suuri osa kunnista on koulutustoimijoita, on niille vaikea luoda omaa tyyppiä.
                                 // Validointi, että tähän voi tallentaa vain kunta-tyyppisen organisaation, tehdään erikseen.
  def aikaleima: Option[LocalDateTime]
}

object ValpasKuntailmoitus {
  def automaattisestiArkistoitava(
    opiskeluoikeudet: Seq[ValpasOpiskeluoikeusLaajatTiedot],
    kaikkiIlmoitukset: Seq[ValpasKuntailmoitus],
  )(
    ilmoitus: ValpasKuntailmoitus
  ): Boolean = {
    val saanutOpiskelupaikan = opiskeluoikeudet.exists(oo =>
      oo.oppivelvollisuudenSuorittamiseenKelpaava &&
        (oo.tarkastelupäivänTila.koodiarvo == "voimassa" || oo.tarkastelupäivänTila.koodiarvo == "voimassatulevaisuudessa")
    )

    val uudemmatIlmoitukset = ilmoitus.aikaleima match {
      case None => Seq.empty
      case Some(käsiteltäväAikaleima) => {
        kaikkiIlmoitukset.filter(_.aikaleima match {
          case None => false
          case Some(aikaleima) => aikaleima.isAfter(käsiteltäväAikaleima)
        })
      }
    }

    val asuinkuntaVaihtunut = uudemmatIlmoitukset.exists(_.kunta.oid != ilmoitus.kunta.oid)

    saanutOpiskelupaikan || asuinkuntaVaihtunut
  }
}

trait ValpasKuntailmoituksenTekijä {
  def organisaatio: OrganisaatioWithOid // Tekijä voi olla joko kunta tai oppilaitos. Validointi, että on jompikumpi, tehdään erikseen.
}

case class ValpasKuntailmoitusSuppeatTiedot(
  id: Option[String], // Oikeasti UUID - scala-schemasta puuttuu tuki UUID-tyypille
  tekijä: ValpasKuntailmoituksenTekijäSuppeatTiedot,
  kunta: OrganisaatioWithOid,
  aikaleima: Option[LocalDateTime],
  // Option, koska riippuen käyttöoikeuksista käyttäjä voi saada nähdä vain osan tietyn ilmoituksen tiedoista,
  // tai tätä ei ole enää tallessa, koska on oppivelvollisuusrekisterin ulkopuolista dataa.
  hakenutMuualle: Option[Boolean],
  // Option, koska relevantti kenttä vain haettaessa ilmoituksia tietylle kunnalle
  onUudempiaIlmoituksiaMuihinKuntiin: Option[Boolean]
) extends ValpasKuntailmoitus

object ValpasKuntailmoitusSuppeatTiedot {
  def apply(laajatTiedot: ValpasKuntailmoitusLaajatTiedotLisätiedoilla): ValpasKuntailmoitusSuppeatTiedot = {
    ValpasKuntailmoitusSuppeatTiedot(laajatTiedot.kuntailmoitus)
  }

  def apply(laajatTiedot: ValpasKuntailmoitusLaajatTiedot): ValpasKuntailmoitusSuppeatTiedot = {
    ValpasKuntailmoitusSuppeatTiedot(
      id = laajatTiedot.id,
      tekijä = ValpasKuntailmoituksenTekijäSuppeatTiedot(laajatTiedot.tekijä),
      kunta = laajatTiedot.kunta,
      aikaleima = laajatTiedot.aikaleima,
      hakenutMuualle = laajatTiedot.hakenutMuualle,
      onUudempiaIlmoituksiaMuihinKuntiin = laajatTiedot.onUudempiaIlmoituksiaMuihinKuntiin,
    )
  }
}

case class ValpasKuntailmoitusLaajatTiedot(
  id: Option[String], // Oikeasti UUID - scala-schemasta puuttuu tuki UUID-tyypille
  kunta: OrganisaatioWithOid,
  aikaleima: Option[LocalDateTime], // Option, koska create-operaatiossa bäkkäri täyttää ilmoitusajan
  tekijä: ValpasKuntailmoituksenTekijäLaajatTiedot,
  @KoodistoUri("kieli")
  @KoodistoKoodiarvo("FI")
  @KoodistoKoodiarvo("SV")

  // Option, koska riippuen käyttöoikeuksista käyttäjä voi saada nähdä vain osan tietyn ilmoituksen tiedoista,
  // tai tätä ei ole enää tallessa, koska on oppivelvollisuusrekisterin ulkopuolista dataa.
  yhteydenottokieli: Option[Koodistokoodiviite],

  // Option, koska riippuen käyttöoikeuksista käyttäjä voi saada nähdä vain osan tietyn
  // ilmoituksen tiedoista, tai tätä ei ole enää tallessa, koska on oppivelvollisuusrekisterin ulkopuolista dataa
  oppijanYhteystiedot: Option[ValpasKuntailmoituksenOppijanYhteystiedot],

  // Option, koska riippuen käyttöoikeuksista käyttäjä voi saada nähdä vain osan tietyn ilmoituksen tiedoista,
  // tai tätä ei ole enää tallessa, koska on oppivelvollisuusrekisterin ulkopuolista dataa.
  hakenutMuualle: Option[Boolean],

  // Option, koska relevantti kenttä vain haettaessa ilmoituksia tietylle kunnalle
  onUudempiaIlmoituksiaMuihinKuntiin: Option[Boolean]
) extends ValpasKuntailmoitus

case class ValpasKuntailmoituksenTekijäSuppeatTiedot(
  organisaatio: OrganisaatioWithOid
) extends ValpasKuntailmoituksenTekijä

object ValpasKuntailmoituksenTekijäSuppeatTiedot {
  def apply(laajatTiedot: ValpasKuntailmoituksenTekijäLaajatTiedot): ValpasKuntailmoituksenTekijäSuppeatTiedot = {
    ValpasKuntailmoituksenTekijäSuppeatTiedot(laajatTiedot.organisaatio)
  }
}

case class ValpasKuntailmoituksenTekijäLaajatTiedot(
  organisaatio: OrganisaatioWithOid,
  henkilö: Option[ValpasKuntailmoituksenTekijäHenkilö], // Option, koska tämä on oppivelvollisuurekisterin ulkopuolista lisädataa eikä välttämättä tallessa tietokannassa vaikka muuta ilmoituksen tiedot olisivatkin
) extends ValpasKuntailmoituksenTekijä

case class ValpasKuntailmoituksenTekijäHenkilö(
  oid: Option[ValpasKuntailmoituksenTekijäHenkilö.Oid], // Option, koska create-operaatiossa bäkkäri lukee tekijän oidin sessiosta
  etunimet: Option[String], // Option, koska kuntailmoitusta tehdessä tieto täytetään käyttäjän tiedoista
  sukunimi: Option[String], // Option, koska kuntailmoitusta tehdessä tieto täytetään käyttäjän tiedoista
  kutsumanimi: Option[String],
  email: Option[String],
  puhelinnumero: Option[String]
)

object ValpasKuntailmoituksenTekijäHenkilö {
  type Oid = String
}

case class ValpasKuntailmoituksenOppijanYhteystiedot(
  puhelinnumero: Option[String] = None,
  email: Option[String] = None,
  lähiosoite: Option[String] = None,
  postinumero: Option[String] = None,
  postitoimipaikka: Option[String] = None,
  @KoodistoUri("maatjavaltiot2")
  maa: Option[Koodistokoodiviite] = None
)

case class ValpasKuntailmoitusPohjatiedotInput(
  /**
   * Option, koska detaljinäkymästä ilmoitusta tehtäessä tekijäroganisaatiota ei välttämättä tiedetä. Paluuarvossa
   * palautetaan sen vuoksi mahdollisetTekijäOrganisaatiot, jotka voi tarjota käyttäjälle valitsimessa.
   */
  tekijäOrganisaatio: Option[OrganisaatioWithOid],
  oppijaOidit: List[String]
)

case class ValpasKuntailmoitusPohjatiedot(
  tekijäHenkilö: ValpasKuntailmoituksenTekijäHenkilö,
  mahdollisetTekijäOrganisaatiot: Seq[OrganisaatioWithOid],
  oppijat: Seq[ValpasOppijanPohjatiedot],
  kunnat: Seq[OrganisaatioWithOid],
  @KoodistoUri("maatjavaltiot2")
  maat: Seq[Koodistokoodiviite],
  @KoodistoUri("kieli")
  yhteydenottokielet: Seq[Koodistokoodiviite],
)

case class ValpasOppijanPohjatiedot(
  oppijaOid: String,
  @KoodistoUri("kieli")
  @KoodistoKoodiarvo("FI")
  @KoodistoKoodiarvo("SV")
  yhteydenottokieli: Option[Koodistokoodiviite],
  turvakielto: Boolean,
  yhteystiedot: Seq[ValpasPohjatietoYhteystieto],
  hetu: Option[String],
)

case class ValpasPohjatietoYhteystieto(
  yhteystietojenAlkuperä: ValpasYhteystietojenAlkuperä,
  yhteystiedot: ValpasKuntailmoituksenOppijanYhteystiedot,
  kunta: Option[OrganisaatioWithOid]
)

case class ValpasKuntailmoitusOpiskeluoikeusKonteksti(
  ilmoitusId: String, // Oikeasti UUID - scala-schemasta puuttuu tuki UUID-tyypille
  opiskeluoikeusOid: ValpasOpiskeluoikeus.Oid
)
