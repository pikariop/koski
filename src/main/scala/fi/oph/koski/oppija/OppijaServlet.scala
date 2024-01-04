package fi.oph.koski.oppija

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.henkilo.HenkilöOid
import fi.oph.koski.http.{HttpStatus, KoskiErrorCategory}
import fi.oph.koski.json.JsonSerializer.extract
import fi.oph.koski.json.SensitiveAndRedundantDataFilter
import fi.oph.koski.koskiuser._
import fi.oph.koski.log._
import fi.oph.koski.opiskeluoikeus.OpiskeluoikeusQueries
import fi.oph.koski.schema.KoskiSchema.lenientDeserializationWithoutValidation
import fi.oph.koski.schema._
import fi.oph.koski.servlet.RequestDescriber.logSafeDescription
import fi.oph.koski.servlet.{KoskiSpecificApiServlet, NoCache}
import fi.oph.koski.tiedonsiirto.TiedonsiirtoError
import fi.oph.koski.util.{Pagination, Timing, XML}
import fi.oph.koski.virta.{VirtaHakuehtoHetu, VirtaHakuehtoKansallinenOppijanumero}
import fi.oph.koski.ytr.YtrSsnWithPreviousSsns
import fi.oph.koski.ytr.download.{YtrLaajaOppija, YtrSsnData, YtrSsnDataWithPreviousSsns}

import javax.servlet.http.HttpServletRequest
import org.json4s.JsonAST.{JBool, JObject, JString}
import org.json4s.{DefaultFormats, JArray, JValue}
import org.scalatra.ContentEncodingSupport

class OppijaServlet(implicit val application: KoskiApplication)
  extends KoskiSpecificApiServlet
    with Logging
    with OpiskeluoikeusQueries
    with RequiresVirkailijaOrPalvelukäyttäjä
    with ContentEncodingSupport
    with NoCache
    with Timing
    with Pagination {

  post("/") { putSingle(false) }

  put("/") { putSingle(true) }

  private def putSingle(allowUpdate: Boolean) = {
    withTracking { withJsonBody { (oppijaJson: JValue) =>
      val cleanedJson = cleanForTesting(oppijaJson)
      val validationResult: Either[HttpStatus, Oppija] = if (application.validationContext.validoiOpiskeluoikeudet) {
        application.validator.extractUpdateFieldsAndValidateOppija(cleanedJson)(session, AccessType.write)
      } else {
        application.validator.extractOppija(cleanedJson, lenientDeserializationWithoutValidation)
      }
      val result: Either[HttpStatus, HenkilönOpiskeluoikeusVersiot] = UpdateContext(session, application, request).putSingle(validationResult, cleanedJson, allowUpdate)
      renderEither[HenkilönOpiskeluoikeusVersiot](result)
    }(parseErrorHandler = handleUnparseableJson)}
  }

  put("/batch") {
    withTracking { withJsonBody { parsedJson =>
      val putter = UpdateContext(session, application, request)

      val validationResults: List[(Either[HttpStatus, Oppija], JValue)] = application.validator.extractUpdateFieldsAndValidateBatch(parsedJson.asInstanceOf[JArray])(session, AccessType.write)

      val batchResults: List[Either[HttpStatus, HenkilönOpiskeluoikeusVersiot]] = validationResults.par.map { results =>
        putter.putSingle(results._1, results._2, true)
      }.toList

      response.setStatus(batchResults.map {
        case Left(status) => status.statusCode
        case _ => 200
      }.max)

      batchResults
    }(parseErrorHandler = handleUnparseableJson)}
  }

  get("/") {
    val serializer = SensitiveAndRedundantDataFilter(session).rowSerializer

    val oppijat = performOpiskeluoikeudetQueryLaajoillaHenkilötiedoilla.map(observable => observable
      .map(x => (application.henkilöRepository.oppijaHenkilöToTäydellisetHenkilötiedot(x._1), x._2))
      .map(serializer)
    )

    streamResponse(oppijat, session)
  }

  // TODO: tarkista lokeista voiko tämän poistaa
  get("/:oid") {
    renderEither[Oppija](HenkilöOid.validateHenkilöOid(params("oid")).right.flatMap { oid =>
      application.oppijaFacade.findOppija(oid, findMasterIfSlaveOid = false)(session)
    }.flatMap(_.warningsToLeft))
  }

  get("/:oid/uiv2") {
    renderEither[Oppija](HenkilöOid.validateHenkilöOid(params("oid")).right.flatMap { oid =>
      application.oppijaFacade.findOppija(oid, useVirta = false, useYtr = false)(session)
    }.flatMap(_.warningsToLeft))
  }

  get("/:oid/ytr-json") {
    if (!onOikeusNähdäYtrJson) {
      haltWithStatus(KoskiErrorCategory.forbidden.kiellettyKäyttöoikeus())
    } else {
      renderEither[Oppija](HenkilöOid.validateHenkilöOid(params("oid")).right.flatMap { oid => {
        application.oppijaFacade.findYtrDownloadedOppija(
          oid,
          findMasterIfSlaveOid = true
        )(KoskiSpecificSession.systemUserTallennetutYlioppilastutkinnonOpiskeluoikeudet)
      }
      }
        .flatMap(_.warningsToLeft)
        .map(auditLogYtrKatsominen)
      )
    }
  }

  private def onOikeusNähdäYtrJson: Boolean = {
    session.hasGlobalReadAccess && session.sensitiveDataAllowed(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT))
  }

  get("/:oid/ytr-json/:versionumero") {
    if (!onOikeusNähdäYtrJson) {
      haltWithStatus(KoskiErrorCategory.forbidden.kiellettyKäyttöoikeus())
    } else {
      renderEither[Oppija](HenkilöOid.validateHenkilöOid(params("oid")).right.flatMap { oid => {
        application.oppijaFacade.findYtrDownloadedOppijaVersionumerolla(
          oid,
          params("versionumero").toInt,
          findMasterIfSlaveOid = true
        )(KoskiSpecificSession.systemUserTallennetutYlioppilastutkinnonOpiskeluoikeudet)
      }
      }
        .flatMap(_.warningsToLeft)
        .map(auditLogYtrKatsominen)
      )
    }
  }

  private def auditLogYtrKatsominen(oppija: Oppija): Oppija = {
    oppija.opiskeluoikeudet.foreach(oo =>
      AuditLog.log(
        KoskiAuditLogMessage(
          KoskiOperation.YTR_OPISKELUOIKEUS_KATSOMINEN,
          session,
          Map(
            KoskiAuditLogMessageField.oppijaHenkiloOid -> (oppija.henkilö match {
              case henkilö: HenkilöWithOid => henkilö.oid
              case _ => ""
            }),
            KoskiAuditLogMessageField.opiskeluoikeusOid -> oo.oid.getOrElse(""),
            KoskiAuditLogMessageField.opiskeluoikeusVersio -> oo.versionumero.map(_.toString).getOrElse("")
          )
        )
      )
    )
    oppija
  }

  get("/:oid/ytr-saved-original-json") {
    if (!onOikeusNähdäYtrJson) {
      haltWithStatus(KoskiErrorCategory.forbidden.kiellettyKäyttöoikeus())
    } else {
      val oppijaOid = HenkilöOid.validateHenkilöOid(params("oid"))
      renderEither[JValue](oppijaOid.right.map { oid =>
          application.ytrPossu.findAlkuperäinenYTRJsonByOppijaOid(oid)
        }.flatMap {
        case Some(json) => Right(json)
        case _ => Left(KoskiErrorCategory.notFound("Alkuperäistä dataa ei löytynyt"))
      }.map(
        auditLogYtrKatsominen(oppijaOid.getOrElse(""))
      ))
    }
  }

  get("/:oid/ytr-current-original-json") {
    if (!onOikeusNähdäYtrJson) {
      haltWithStatus(KoskiErrorCategory.forbidden.kiellettyKäyttöoikeus())
    } else {
      val oppijaOid = HenkilöOid.validateHenkilöOid(params("oid"))
      renderEither[List[YtrLaajaOppija]](oppijaOid.right
        .flatMap(oid =>
          application.opintopolkuHenkilöFacade.findMasterOppija(oid).toRight(KoskiErrorCategory.notFound.oppijaaEiLöydy())
        )
        .flatMap {
          case henkilö if henkilö.hetu.isDefined =>
            Right(YtrSsnDataWithPreviousSsns(Some(List(YtrSsnWithPreviousSsns(henkilö.hetu.get, henkilö.vanhatHetut)))))
          case _ =>
            Left(KoskiErrorCategory.notFound())
        }
        .map(application.ytrClient.oppijatByHetut)
        .map(
          auditLogYtrKatsominen(oppijaOid.getOrElse(""))
        ))
    }
  }

  private def auditLogYtrKatsominen[T](oppijaOid: String)(originalJson: T): T = {
    AuditLog.log(
      KoskiAuditLogMessage(
        KoskiOperation.YTR_OPISKELUOIKEUS_KATSOMINEN,
        session,
        Map(
          KoskiAuditLogMessageField.oppijaHenkiloOid -> oppijaOid
        )
      )
    )
    originalJson
  }

  get("/:oid/opintotiedot-json") {
    renderEither[Oppija](HenkilöOid.validateHenkilöOid(params("oid")).right.flatMap { oid =>
      application.oppijaFacade.findOppija(oid, findMasterIfSlaveOid = true)(session)
    }.flatMap(_.warningsToLeft))
  }

  get("/:oid/virta-opintotiedot-xml") {
    if (!session.hasGlobalReadAccess) {
      haltWithStatus(KoskiErrorCategory.forbidden())
    }
    virtaOpinnot(params("oid")) match {
      case Right(elements) =>
        contentType = "text/plain"
        response.writer.print(XML.prettyPrintNodes(elements))
      case Left(status) => haltWithStatus(status)
    }
  }

  get("/oids") {
    streamResponse[String](application.opiskeluoikeusQueryRepository.oppijaOidsQuery(paginationSettings)(session), session)
  }

  private def virtaOpinnot(oid: String) =
    application.opintopolkuHenkilöFacade.findMasterOppija(oid).toRight(KoskiErrorCategory.notFound.oppijaaEiLöydy()).map { oppijaHenkilö =>
      val byHetu = (oppijaHenkilö.hetu.toList ++ oppijaHenkilö.vanhatHetut).sorted.map(VirtaHakuehtoHetu)
      val byOid = (oppijaHenkilö.oid :: oppijaHenkilö.linkitetytOidit).sorted.map(VirtaHakuehtoKansallinenOppijanumero)
      application.virtaClient.opintotiedotMassahaku(byHetu) ++ application.virtaClient.opintotiedotMassahaku(byOid)
    }.map(_.toList.distinct)

  private def handleUnparseableJson(status: HttpStatus) = {
    application.tiedonsiirtoService.storeTiedonsiirtoResult(session, None, None, None, Some(TiedonsiirtoError(JObject("unparseableJson" -> JString(request.body)), status.errors)))
    haltWithStatus(status)
  }

  private def withTracking[T](f: => T) = {
    if (session.isPalvelukäyttäjä) {
      application.ipService.trackIPAddress(session)
    }
    try {
      f
    } catch {
      case e: Exception =>
        application.tiedonsiirtoService.storeTiedonsiirtoResult(session, None, None, None, Some(TiedonsiirtoError(JObject("unparseableJson" -> JString(request.body)), KoskiErrorCategory.internalError().errors)))
        logger.error(e)("virhe aiheutti unparseableJson merkinnän tiedonsiirtoihin")
        throw e
    }
  }

  private def cleanForTesting(oppijaJson: JValue) = {
    val cleanForTesting = extract[Boolean]((oppijaJson \ "cleanForTesting").map {
      case pass: JBool => pass
      case _ => JBool(false)
    })

    val shouldClean = loginEnvIsMock && cleanForTesting

    if (shouldClean) {
      implicit val formats = DefaultFormats
      oppijaJson.replace(List("henkilö"), (oppijaJson \ "henkilö").removeField {
        case ("oid", _) => true
        case ("kansalaisuus", _) => true
        case ("äidinkieli", _) => true
        case ("syntymäaika", _) => true
        case ("turvakielto", _) => true
        case _ => false
      })
    } else {
      oppijaJson
    }
  }

  private def loginEnvIsMock = {
    val sec = application.config.getString("login.security")
    sec == "mock"
  }
}

/**
  *  Operating context for data updates. Operates outside the lecixal scope of OppijaServlet to ensure that none of the
  *  Scalatra threadlocals are used. This must be done because in batch mode, we are running in several threads.
  */
case class UpdateContext(user: KoskiSpecificSession, application: KoskiApplication, request: HttpServletRequest) extends Logging {
  def putSingle(validationResult: Either[HttpStatus, Oppija], oppijaJsonFromRequest: JValue, allowUpdate: Boolean): Either[HttpStatus, HenkilönOpiskeluoikeusVersiot] = {

    validationResult.foreach(_.tallennettavatOpiskeluoikeudet.filter(_.lähdejärjestelmänId.isDefined).flatMap(_.versionumero).foreach(_ => logger.info("Lähdejärjestelmä siirsi versionumeron")))

    val result: Either[HttpStatus, HenkilönOpiskeluoikeusVersiot] = validationResult.flatMap(application.oppijaFacade.createOrUpdate(_, allowUpdate)(user))

    result.left.foreach { case HttpStatus(code, errors) =>
      logger(user).info("Opiskeluoikeuden lisäys/päivitys estetty: " + code + " " + errors + " for request " + logSafeDescription(request))
    }

    val error = result.left.toOption.map(status => TiedonsiirtoError(oppijaJsonFromRequest, status.errors))
    application.tiedonsiirtoService
      .storeTiedonsiirtoResult(user, result.toOption.map(_.henkilö), validationResult.toOption, Some(oppijaJsonFromRequest), error)

    result
  }
}

