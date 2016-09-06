package fi.oph.koski.omattiedot

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.koskiuser.RequiresAuthentication
import fi.oph.koski.servlet.ApiServlet

class OmatTiedotServlet(val application: KoskiApplication) extends ApiServlet with RequiresAuthentication {

  get() {
    application.facade.findUserOppija(koskiUser)
  }
}
