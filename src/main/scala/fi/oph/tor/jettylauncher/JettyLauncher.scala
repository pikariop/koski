package fi.oph.tor.jettylauncher

import fi.vm.sade.utils.tcp.PortChecker
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.{ContextHandler, HandlerList, ResourceHandler}
import org.eclipse.jetty.webapp.WebAppContext

object JettyLauncher extends App {
  lazy val globalPort = System.getProperty("tor.port","7021").toInt
  new JettyLauncher(globalPort).start.join
}

class JettyLauncher(val port: Int, overrides: Map[String, String] = Map.empty) {
  lazy val server = new Server(port)

  val context = new WebAppContext()
  context.setContextPath("/tor")
  context.setResourceBase("src/main/webapp")
  context.setDescriptor("src/main/webapp/WEB-INF/web.xml")
  context.setAttribute("tor.overrides", overrides)

  val all = new HandlerList
  all.setHandlers(List(
    staticResources("./web/static", "/tor"),
    staticResources("./web/dist", "/tor"),
    staticResources("./web/test", "/tor/test"),
    staticResources("./web/node_modules/codemirror", "/tor/codemirror"),
    context).toArray)

  server.setHandler(all)

  def staticResources(path: String, contextPath: String) = {
    val staticResources = new ResourceHandler()
    staticResources.setResourceBase(path)
    val contextHandler = new ContextHandler(contextPath)
    contextHandler.setHandler(staticResources)
    contextHandler
  }

  def start = {
    server.start
    server
  }

  def withJetty[T](block: => T) = {
    val server = start
    try {
      block
    } finally {
      server.stop
    }
  }

  def baseUrl = "http://localhost:" + port + "/tor"
}

object SharedJetty extends JettyLauncher(PortChecker.findFreeLocalPort, Map("db.name" -> "tortest", "fixtures.use" -> "true")) {
}