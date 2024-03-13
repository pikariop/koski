package fi.oph.koski.queuedqueries

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.log.Logging
import fi.oph.koski.schedule.{IntervalSchedule, Scheduler}
import fi.oph.koski.schema.KoskiSchema.strictDeserialization
import org.json4s.JValue

class QueryScheduler(application: KoskiApplication) extends Logging {
  val concurrency: Int = application.config.getInt("kyselyt.concurrency")
  val kyselyt = application.kyselyService

  sys.addShutdownHook {
    kyselyt.cancelAllTasks("Interrupted: worker shutdown")
  }

  def scheduler: Option[Scheduler] = {
    val context = createContext
    if (isQueryWorker(context)) {
      val workerId = application.kyselyService.workerId
      logger.info(s"Starting as Query Worker. id: $workerId")

      Some(new Scheduler(
        application.masterDatabase.db,
        "kysely",
        new IntervalSchedule(application.config.getDuration("kyselyt.checkInterval")),
        Some(context.asJson),
        runNextQuery,
        intervalMillis = 1000
      ))
    } else {
      None
    }
  }

  private def runNextQuery(context: Option[JValue]): Option[JValue] = {
    if (context.flatMap(parseContext).exists(isQueryWorker)) {
      if (kyselyt.numberOfRunningQueries < concurrency) {
        kyselyt.runNext()
      }
    }
    None // QueryScheduler päivitä kontekstia vain käynnistyessään
  }

  private def createContext: QuerySchedulerContext = QuerySchedulerContext(
    workerId = kyselyt.workerId,
  )

  private def parseContext(context: JValue): Option[QuerySchedulerContext] =
    application
      .validatingAndResolvingExtractor.extract[QuerySchedulerContext](context, strictDeserialization)
      .toOption

  private def isQueryWorker(context: QuerySchedulerContext) =
    QueryUtils.isQueryWorker(application) && context.workerId == kyselyt.workerId
}

case class QuerySchedulerContext(
  workerId: String,
) {
  def asJson: JValue = JsonSerializer.serializeWithRoot(this)
}
