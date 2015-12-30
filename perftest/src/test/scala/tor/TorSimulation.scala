package tor

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.{Http, HttpRequestBuilder}
import tor.Scenarios._
import scala.concurrent.duration._


class TorSimulation extends Simulation {

  val baseUrl = sys.env.getOrElse("TOR_BASE_URL", "http://tordev.tor.oph.reaktor.fi/tor")

  val httpConf = http.baseURL(baseUrl).acceptEncodingHeader("gzip, deflate")

  val headers = Map("Content-Type" -> "application/json")

  setUp(
    findOppija.inject(constantUsersPerSec(1) during(1 minute) randomized),
    updateOppija.inject(constantUsersPerSec(1) during(1 minute) randomized)
  ).protocols(httpConf)
}

object Scenarios {

  val username = sys.env("TOR_USER")
  val password = sys.env("TOR_PASS")

  val findOppija = scenario("Find oppija").exec(
    http("find by oid")
      .get("/api/oppija/1.2.246.562.24.00000000001")
      .basicAuth(username, password)
  )

  val updateOppija = scenario("Update oppija").exec(
    http("update")
      .put("/api/oppija")
      .body(RawFileBody("oppija.json")).asJSON
      .basicAuth(username, password)
  )

}
