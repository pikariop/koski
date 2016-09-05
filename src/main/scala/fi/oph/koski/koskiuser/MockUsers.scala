package fi.oph.koski.koskiuser

import fi.oph.koski.koskiuser.Käyttöoikeusryhmät.{ophPääkäyttäjä, oppilaitosKatselija, oppilaitosPalvelukäyttäjä, oppilaitosTallentaja, viranomaisKatselija}
import fi.oph.koski.organisaatio.MockOrganisaatiot.{lehtikuusentienToimipiste, omnia, oppilaitokset}
import fi.oph.koski.organisaatio.{MockOrganisaatiot, Opetushallitus}
import fi.oph.koski.schema.Organisaatio
import fi.vm.sade.security.ldap.LdapUser

object MockUsers {
  val kalle = new MockUser(LdapUser(List(), "käyttäjä", "kalle", "12345"), (lehtikuusentienToimipiste :: oppilaitokset).toSet.map((_: String, oppilaitosTallentaja)))
  val localkoski = new MockUser(LdapUser(List(), "käyttäjä", "localkoski", "1.2.246.562.24.91698845204"), oppilaitokset.toSet.map((_: String, oppilaitosTallentaja)))
  val omniaPalvelukäyttäjä = new MockUser(LdapUser(List(), "käyttäjä", "omnia-palvelukäyttäjä", "11111"), Set((omnia, oppilaitosPalvelukäyttäjä)))
  val omniaKatselija = new MockUser(LdapUser(List(), "käyttäjä", "omnia-katselija", "11112"), Set((omnia, oppilaitosKatselija)))
  val omniaTallentaja = new MockUser(LdapUser(List(), "käyttäjä", "omnia-tallentaja", "11113"), Set((omnia, oppilaitosTallentaja)))
  val paakayttaja = new MockUser(LdapUser(List(), "käyttäjä", "pää", "00001"), Set((Opetushallitus.organisaatioOid, ophPääkäyttäjä)))
  val viranomainen = new MockUser(LdapUser(List(), "käyttäjä", "viranomais", "00002"), Set((Opetushallitus.organisaatioOid, viranomaisKatselija)))
  val stadinAmmattiopistoPalvelukäyttäjä = new MockUser(LdapUser(List(), "tiedonsiirtäjä", "tiedonsiirtäjä", "98371"), Set((MockOrganisaatiot.stadinAmmattiopisto, oppilaitosPalvelukäyttäjä)))
  val helsinkiPalvelukäyttäjä = MockUser(LdapUser(List(), "helsinki", "helsinki", "00100"), Set((MockOrganisaatiot.helsinginKaupunki, oppilaitosPalvelukäyttäjä)))
  val kahdenOrganisaatioPalvelukäyttäjä = MockUser(LdapUser(List(), "palvelu2", "palvelu2", "01130"), Set((MockOrganisaatiot.helsinginKaupunki, oppilaitosPalvelukäyttäjä), (MockOrganisaatiot.omnia, oppilaitosPalvelukäyttäjä)))

  val users = List(kalle, omniaPalvelukäyttäjä, omniaKatselija, omniaTallentaja, localkoski, paakayttaja, viranomainen, stadinAmmattiopistoPalvelukäyttäjä, helsinkiPalvelukäyttäjä, kahdenOrganisaatioPalvelukäyttäjä)
}

case class MockUser(ldapUser: LdapUser, käyttöoikeudet: Set[(Organisaatio.Oid, Käyttöoikeusryhmä)]) extends UserWithPassword {
  def toKoskiUser(käyttöoikeudet: KäyttöoikeusRepository) = new KoskiUser(ldapUser.oid, "192.168.0.10", "fi", käyttöoikeudet.käyttäjänKäyttöoikeudet(oid))
  def oid = ldapUser.oid
  def username = ldapUser.givenNames
  def password = username
}

