package fi.oph.koski.log

import fi.oph.koski.json.GenericJsonFormats
import org.json4s.JsonAST.JObject
import org.json4s.jackson.JsonMethods.parse
import org.scalatest.matchers.should.Matchers

object AuditLogTester extends Matchers with LogTester {
  override def appenderName: String = "Audit"

  def verifyAuditLogMessage(params: Map[String, Any]): Unit = {
    val message = getLogMessages.lastOption.map(m => parse(m))
    message match {
      case Some(msg: JObject) => verifyAuditLogMessage(msg, params)
      case _ => throw new IllegalStateException("No audit log message found")
    }
  }

  def verifyAuditLogMessage(loggingEvent: String, params: Map[String, Any]): Unit = {
    parse(loggingEvent) match {
      case msg: JObject => verifyAuditLogMessage(msg, params)
      case _ => throw new IllegalStateException("No audit log message found")
    }
  }

  def verifyNoAuditLogMessages(): Unit = {
    if (getLogMessages.nonEmpty) {
      throw new IllegalStateException("Audit log message found, expected none")
    }
  }

  private def verifyAuditLogMessage(msg: JObject, params: Map[String, Any]): Unit = {
    implicit val formats = GenericJsonFormats.genericFormats
    params.foreach {
      case (key, expectedValue: String) =>
        msg.values.get(key) should equal(Some(expectedValue))
      case (key, newParams: Map[String, Any] @unchecked) =>
        verifyAuditLogMessage((msg \ key).extract[JObject], newParams)
      case _ => ???
    }
  }
}
