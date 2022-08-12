package fi.oph.koski.config

import com.typesafe.config.Config

import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

object Environment {
  val Local = "local"
  val UnitTest = "unittest"

  def isUnitTestEnvironment(config: Config): Boolean = currentEnvironment(config) == UnitTest

  def isLocalDevelopmentEnvironment(config: Config): Boolean = currentEnvironment(config) == Local

  def isUsingLocalDevelopmentServices(app: KoskiApplication): Boolean =
    app.masterDatabase.isLocal && isMockEnvironment(app.config)

  def isMockEnvironment(config: Config): Boolean =
    config.getString("opintopolku.virkailija.url") == "mock"

  def isProdEnvironment(config: Config): Boolean =
    config.getString("opintopolku.virkailija.url") == "https://virkailija.opintopolku.fi"

  def isServerEnvironment(config: Config): Boolean = !Set(Local, UnitTest).contains(currentEnvironment(config))

  def usesAwsAppConfig: Boolean = {
    sys.env.getOrElse("CONFIG_SOURCE", "") == "appconfig"
  }

  def usesAwsSecretsManager: Boolean = {
    sys.env.getOrElse("USE_SECRETS_MANAGER", "") == "true"
  }

  def currentEnvironment(config: Config): String = config.getString("env")

  def skipFixtures: Boolean =
    sys.env.getOrElse("SKIP_FIXTURES", "") == "true"

  def forceLocalMigration: Option[String] =
    sys.env.get("FORCE_LOCAL_MIGRATION")

  def raportointikantaLoadDueTime: Option[ZonedDateTime] =
    sys.env.get("RAPORTOINTIKANTA_DUETIME").map(time => try {
      ZonedDateTime.parse(time)
    } catch {
      case e: DateTimeParseException => throw new RuntimeException(s"Invalid timestamp format in environment variable RAPORTOINTIKANTA_DUETIME (expected ISO datetime format): ${e.getMessage}")
    })
}
