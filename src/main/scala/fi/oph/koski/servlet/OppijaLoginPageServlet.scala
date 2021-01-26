package fi.oph.koski.servlet

import fi.oph.koski.config.{Environment, KoskiApplication, ShibbolethSecret}
import fi.oph.koski.henkilo.MockOppijat
import fi.oph.koski.http.KoskiErrorCategory
import fi.oph.koski.sso.KoskiSSOSupport
import org.scalatra.{EnvironmentKey, ScalatraServlet}

import scala.xml.Unparsed

class OppijaLoginPageServlet(implicit val application: KoskiApplication) extends ScalatraServlet with OppijaHtmlServlet with KoskiSSOSupport {
  get("/") {
    redirectToOppijaLogin
  }

  get("/local") {
    htmlIndex(
      scriptBundleName = "koski-korhopankki.js",
      scripts = <script id="auth">{Unparsed(s"window.mockUsers=$oppijat")}</script>,
      responsive = true
    )
  }

  private def oppijat = MockOppijat.defaultOppijat.sortBy(_.henkilö.etunimet).flatMap { o =>
    o.henkilö.hetu.filter(_.nonEmpty).map(h => s"""{'hetu': '$h', 'nimi': '${o.henkilö.etunimet} ${o.henkilö.sukunimi}'}""")
  }.distinct.mkString("[", ",", "]")
}
