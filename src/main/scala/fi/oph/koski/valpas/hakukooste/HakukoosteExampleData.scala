package fi.oph.koski.valpas.hakukooste

import fi.oph.koski.henkilo.OppijaHenkilö
import fi.oph.koski.organisaatio.{MockOrganisaatioRepository, MockOrganisaatiot}
import fi.oph.koski.schema._
import fi.oph.koski.valpas.henkilo.ValpasMockOppijat
import fi.vm.sade.oidgenerator.OIDGenerator

import java.time.{LocalDate, LocalDateTime}

object HakukoosteExampleData {
  private implicit class Conversions(maybe: Option[LocalizedString]) {
    def toBlankable: BlankableLocalizedString = maybe.getOrElse(BlankLocalizedString())
  }

  lazy val data = List(
    haku(
      ValpasMockOppijat.oppivelvollinenYsiluokkaKeskenKeväällä2021,
      List(
        hakutoive(
          hakukohdeOid = MockOrganisaatiot.ressunLukio,
          koulutusNimi = "Lukiokoulutus"
        ),
        hakutoive(
          hakukohdeOid = MockOrganisaatiot.helsinginMedialukio,
          koulutusNimi = "Lukiokoulutus"
        ),
        hakutoive(
          hakukohdeOid = MockOrganisaatiot.omnia,
          koulutusNimi = "Leipomoalan ammattitutkinto"
        ),
        hakutoive(
          hakukohdeOid = MockOrganisaatiot.omnia,
          koulutusNimi = "Puhtaus- ja kiinteistöpalvelualan ammattitutkinto laitoshuoltajille ja toimitilahuoltajille"
        ),
        hakutoive(
          hakukohdeOid = MockOrganisaatiot.varsinaisSuomenKansanopisto,
          koulutusNimi = "Vapaan sivistystyön koulutus oppivelvollisille"
        ),
      )),
    haku(
      ValpasMockOppijat.luokalleJäänytYsiluokkalainen,
      List(
        hakutoive(
          hakukohdeOid = "",
          koulutusNimi = "Lukiokoulutus"
        ),
      )),
  )

  def haku(
    henkilö: OppijaHenkilö,
    hakutoiveet: Seq[Hakutoive],
    alkamisaika: LocalDateTime = LocalDateTime.of(2020, 3, 9, 12, 0, 0),
  ): Hakukooste =
    Hakukooste(
      oppijaOid = henkilö.oid,
      hakuOid = generateHakuOid(),
      hakemusOid = generateHakemusOid(),
      hakutapa = yhteishakukoodi,
      hakutyyppi = varsinaisenHaunKoodi,
      haunAlkamispaivamaara = alkamisaika,
      hakuNimi = Finnish("Yhteishaku 2021"),
      email = generateEmail(henkilö),
      lahiosoite = "Esimerkkikatu 123, 00000 KAUPUNKI",
      matkapuhelin = "0401234567",
      huoltajanNimi = Some("Huoltaja Sukunimi"),
      huoltajanPuhelinnumero = Some("0407654321"),
      huoltajanSähkoposti = Some("huoltaja.sukunimi@gmail.com"),
      hakutoiveet = hakutoiveet.map(hakutoive => hakutoive.copy(
        hakukohdeNimi = MockOrganisaatioRepository
          .getOrganisaationNimiHetkellä(hakutoive.hakukohdeOid, alkamisaika.toLocalDate)
          .toBlankable,
        hakutoivenumero = if (hakutoive.hakutoivenumero >= 0) {
          hakutoive.hakutoivenumero
        } else {
          hakutoiveet.indexOf(hakutoive)
        }
      ))
    )

  def hakutoive(
    hakukohdeOid: String,
    koulutusNimi: String,
    hakutoivenumero: Int = -1
  ): Hakutoive =
    Hakutoive(
      hakukohdeOid = hakukohdeOid,
      hakukohdeNimi = MockOrganisaatioRepository.getOrganisaationNimiHetkellä(
        oid = hakukohdeOid,
        localDate = LocalDate.now()
      ).toBlankable,
      hakutoivenumero = hakutoivenumero,
      koulutusNimi = Finnish(koulutusNimi),
      hakukohdeOrganisaatio = hakukohdeOid,
      pisteet = Some(0),
      alinValintaPistemaara = Some(0),
      valintatila = Some("KESKEN"),
      vastaanottotieto = Some("KESKEN"),
      ilmoittautumistila = Some("EI_ILMOITTAUTUNUT"),
      koulutusOid = Some("TODO"),
      harkinnanvaraisuus = Some("TODO"),
      hakukohdeKoulutuskoodi = "TODO"
    )

  def yhteishakukoodi = Koodistokoodiviite("01", "hakutapa")
  def varsinaisenHaunKoodi = Koodistokoodiviite("01", "hakutyyppi")
  def generateHakuOid() = OIDGenerator.generateOID(100)
  def generateHakemusOid() = OIDGenerator.generateOID(101)
  def generateEmail(henkilö: OppijaHenkilö) =
    s"${henkilö.etunimet}.${henkilö.sukunimi}".replace(" ", "-") + "@gmail.com"
}