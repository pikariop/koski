package fi.oph.tor.virta

import com.typesafe.config.Config
import fi.oph.tor.cache.{CachingProxy, CachingStrategy}
import fi.oph.tor.http.Http
import fi.oph.tor.http.Http._
import fi.oph.tor.log.TimedProxy
import fi.oph.tor.util.Files

import scala.xml.{Elem, Node}

object VirtaClient {
  def apply(config: Config) = config.hasPath("virta.serviceUrl") match {
    case false => MockVirtaClient
    case true => CachingProxy(CachingStrategy.noCache, TimedProxy[VirtaClient](RemoteVirtaClient(VirtaConfig.fromConfig(config))))
  }
}

trait VirtaClient {
  def opintotiedot(hakuehto: VirtaHakuehto): Option[Elem]
  def henkilötiedot(hakuehto: VirtaHakuehto, oppilaitosNumero: String): Option[Elem]
}

object MockVirtaClient extends VirtaClient {
  override def opintotiedot(hakuehto: VirtaHakuehto) = {
    hakuehto match {
      case VirtaHakuehtoHetu(hetu) =>
        Files.asString("src/main/resources/mockdata/virta/" + hetu + ".xml").map(scala.xml.XML.loadString)
      case _ =>
        throw new UnsupportedOperationException()
    }
  }
  def henkilötiedot(hakuehto: VirtaHakuehto, oppilaitosNumero: String) = None
}

case class RemoteVirtaClient(config: VirtaConfig) extends VirtaClient {
  def opintotiedot(hakuehto: VirtaHakuehto) = {
    val body = soapEnvelope(
      <OpiskelijanKaikkiTiedotRequest xmlns="http://tietovaranto.csc.fi/luku">
        {kutsuja}
        <Hakuehdot>{ hakuehdot(hakuehto) }</Hakuehdot>
      </OpiskelijanKaikkiTiedotRequest>)
    Some(Http(config.serviceUrl).post(uri"", body)(Http.Encoders.xml, Http.parseXml))
  }

  def henkilötiedot(hakuehto: VirtaHakuehto, oppilaitosNumero: String) = {
    val body = soapEnvelope(
      <OpiskelijanTiedotRequest xmlns="http://tietovaranto.csc.fi/luku">
        {kutsuja}
        <Hakuehdot>{ hakuehdot(hakuehto) }<organisaatio>{oppilaitosNumero}</organisaatio></Hakuehdot>
      </OpiskelijanTiedotRequest>)
    Some(Http(config.serviceUrl).post(uri"", body)(Http.Encoders.xml, Http.parseXml))
  }

  private def hakuehdot(hakuehto: VirtaHakuehto) = hakuehto match {
    case VirtaHakuehtoHetu(hetu) => <henkilotunnus>{hetu}</henkilotunnus>
    case VirtaHakuehtoKansallinenOppijanumero(oppijanumero) => <kansallinenOppijanumero>{oppijanumero}</kansallinenOppijanumero>
  }

  private def kutsuja = <Kutsuja>
    <jarjestelma>{config.jarjestelma}</jarjestelma>
    <tunnus>{config.tunnus}</tunnus>
    <avain>{config.avain}</avain>
  </Kutsuja>


  private def soapEnvelope(node: Node) = <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
    <SOAP-ENV:Body>{node}</SOAP-ENV:Body>
  </SOAP-ENV:Envelope>
}

sealed trait VirtaHakuehto
case class VirtaHakuehtoHetu(hetu: String) extends VirtaHakuehto
case class VirtaHakuehtoKansallinenOppijanumero(numero: String) extends VirtaHakuehto

case class VirtaConfig(serviceUrl: String, jarjestelma: String, tunnus: String, avain: String)

object VirtaConfig {
  // Virta test environment config, see http://virtawstesti.csc.fi/
  val virtaTestEnvironment = VirtaConfig("http://virtawstesti.csc.fi/luku106/OpiskelijanTiedot", "", "", "salaisuus")
  def fromConfig(config: Config) = VirtaConfig(config.getString("virta.serviceUrl"), config.getString("virta.jarjestelma"), config.getString("virta.tunnus"), config.getString("virta.avain"))
}
