package fi.oph.tor.oppija

import fi.oph.tor.http.{Http, HttpStatus, VirkailijaHttpClient}
import fi.oph.tor.json.Json._
import fi.oph.tor.json.Json4sHttp4s._
import fi.vm.sade.utils.memoize.TTLOptionalMemoize
import org.http4s._

import scalaz.concurrent.Task

class RemoteOppijaRepository(henkilöPalveluClient: VirkailijaHttpClient) extends OppijaRepository with EntityDecoderInstances {
  override def findOppijat(query: String): List[Oppija] = {
    henkilöPalveluClient.httpClient(henkilöPalveluClient.virkailijaUriFromString("/authentication-service/resources/henkilo?no=true&count=0&q=" + query))(Http.parseJson[AuthenticationServiceUserQueryResult])
      .results.map(toOppija)
  }

  override def findByOid(id: String): Option[Oppija] = henkilöPalveluClient.httpClient(henkilöPalveluClient.virkailijaUriFromString("/authentication-service/resources/henkilo/" + id))(Http.parseJsonOptional[AuthenticationServiceUser]).map(toOppija)

  override def create(hetu: String, etunimet: String, kutsumanimi: String, sukunimi: String) = {
      val task: Task[Request] = Request(
        uri = henkilöPalveluClient.virkailijaUriFromString("/authentication-service/resources/henkilo"),
        method = Method.POST
      ).withBody(new AuthenticationServiceCreateUser(hetu, "OPPIJA", sukunimi, etunimet, kutsumanimi))(json4sEncoderOf[AuthenticationServiceCreateUser])

      henkilöPalveluClient.httpClient(task) {
        case (200, oid) => Right(oid)
        case (400, "socialsecuritynr.already.exists") => Left(HttpStatus.conflict("socialsecuritynr.already.exists"))
        case (status, text) => throw new RuntimeException(status + ": " + text)
      }
  }

  private def toOppija(user: AuthenticationServiceUser) = Oppija(user.oidHenkilo, user.hetu, user.etunimet, user.kutsumanimi, user.sukunimi)
}

case class AuthenticationServiceUserQueryResult(totalCount: Integer, results: List[AuthenticationServiceUser])
case class AuthenticationServiceUser(oidHenkilo: Option[String], sukunimi: Option[String], etunimet: Option[String], kutsumanimi: Option[String], hetu: Option[String])
case class AuthenticationServiceCreateUser(hetu: String, henkiloTyyppi: String, sukunimi: String, etunimet: String, kutsumanimi: String)