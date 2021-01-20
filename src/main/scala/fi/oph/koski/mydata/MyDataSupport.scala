package fi.oph.koski.mydata

import java.net.URLEncoder

import fi.oph.koski.http.KoskiErrorCategory
import fi.oph.koski.servlet.InvalidRequestException
import com.typesafe.config.{ConfigFactory, Config => TypeSafeConfig}
import fi.oph.koski.config.{Environment, ShibbolethSecret}
import org.scalatra.ScalatraServlet

trait MyDataSupport extends ScalatraServlet with MyDataConfig {
  override def hasConfigForMember(id: String = memberCodeParam): Boolean
  override def getConfigForMember(id: String = memberCodeParam): TypeSafeConfig
  def mydataLoginServletURL: String = conf.getString("login.servlet")

  def urlEncode(str: String): String = URLEncoder.encode(str, "UTF-8")

  def getLoginURL(target: String = getCurrentURL, encode: Boolean = false): String = {
    if (encode) {
      urlEncode(s"${mydataLoginServletURL}?onSuccess=${urlEncode(target)}")
    } else {
      s"${mydataLoginServletURL}?onSuccess=${target}"
    }
  }

  def getCasLoginURL(target: String = getCurrentURL, lang: String) = {
    conf.getString(s"login.cas.$lang") +
      conf.getString("login.cas.targetparam") + getLoginURL(target, encode = true) +
      "&valtuudet=false" + getKorhopankkiRedirectURLParameter(target)
  }

  def getKorhopankkiRedirectURLParameter(target: String): String = {
    val security = if (Environment.usesAwsSecretsManager) {
      ShibbolethSecret.fromSecretsManager
    } else {
      ShibbolethSecret.fromConfig(application.config)
    }

    if(security == "mock") {
      s"&redirect=${urlEncode(target)}"
    } else {
      ""
    }
  }

  def getCurrentURL: String = {
    if (request.queryString.isEmpty) {
      request.getRequestURI
    } else {
      request.getRequestURI + s"?${request.queryString}"
    }
  }

  def memberCodeParam: String = {
    if (params("memberCode") == null) {
      throw InvalidRequestException(KoskiErrorCategory.badRequest.queryParam.missing("Vaadittu valtuutuksen kumppani-parametri puuttuu"))
    }

    params("memberCode")
  }
}
