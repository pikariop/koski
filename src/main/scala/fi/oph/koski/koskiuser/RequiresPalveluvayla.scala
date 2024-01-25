package fi.oph.koski.koskiuser

import fi.oph.koski.http.KoskiErrorCategory

trait RequiresPalveluvayla extends KoskiSpecificAuthenticationSupport {
  implicit def koskiSession: KoskiSpecificSession = koskiSessionOption.get

  before() {
    getUser match {
      case Left(status) if status.statusCode == 401 =>
        haltWithStatus(status)
      case _ =>
        if (!koskiSessionOption.exists(_.hasPalveluvaylaAccess)) {
          haltWithStatus(KoskiErrorCategory.forbidden.vainPalveluvayla())
        }
    }
  }
}