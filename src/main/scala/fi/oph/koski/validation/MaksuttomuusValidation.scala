package fi.oph.koski.validation


import fi.oph.koski.http.{HttpStatus, KoskiErrorCategory}
import fi.oph.koski.koskiuser.KoskiSpecificSession
import fi.oph.koski.opiskeluoikeus.CompositeOpiskeluoikeusRepository
import fi.oph.koski.schema._
import fi.oph.koski.util.DateOrdering

import java.time.LocalDate

object MaksuttomuusValidation {

  def checkOpiskeluoikeudenMaksuttomuus(opiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus,
                                        oppijanSyntymäpäivä: Option[LocalDate],
                                        oppijanOid: String,
                                        oppijanHetu: Option[String],
                                        opiskeluoikeusRepository: CompositeOpiskeluoikeusRepository)
                                       (implicit user: KoskiSpecificSession): HttpStatus = {
    val aikaraja = LocalDate.of(2021, 1, 1)
    val perusopetuksenAikavälit = opiskeluoikeusRepository.getPerusopetuksenAikavälit(oppijanOid)
    val maksuttomuustietoSiirretty = opiskeluoikeus.lisätiedot.collect { case l: MaksuttomuusTieto => l.maksuttomuus.toList.flatten.length > 0 }.getOrElse(false)
    val maksuttomuudenPidennysSiirretty = opiskeluoikeus.lisätiedot.collect { case l : MaksuttomuusTieto => l.oikeuttaMaksuttomuuteenPidennetty.toList.flatten.length > 0 }.getOrElse(false)

    // A2. Koskessa on jokin merkintä peruskoulun (tai vastaavan) suorituksesta, joka on joko päättynyt 1.1.2021 tai myöhemmin, tai on ollut aktiivisena 1.1.2021 tai myöhemmin, jos ei ole vielä päättynyt.
    val peruskoulussaVuoden2021Alussa = perusopetuksenAikavälit.exists(p => {
      p.loppu.isEmpty ||                            // Aktiivinen tällä hetkellä, tai
        p.loppu.exists(!_.isBefore(aikaraja))       // suoritettu lain voimassaoloaikana
    })

    // A3. Peruskoulun jälkeisen koulutuksen suoritus on alkanut 1.1.2021 tai myöhemmin
    val peruskoulunJälkeinenKoulutusAlkanutAikaisintaan2021 = opiskeluoikeus.alkamispäivä.exists(p => !p.isBefore(aikaraja))

    // A4. Peruskoulun jälkienen koulutus on uuden lain mukaiseksi peruskoulun jälkeiseksi oppivelvollisuuskoulutukseksi kelpaavaa
    val koulutusOppivelvollisuuskoulutukseksiKelpaavaa = oppivelvollisuudenSuorittamiseenKelpaavaMuuKuinPeruskoulunOpiskeluoikeus(opiskeluoikeus)

    // Oppijalla on Koskessa valmistumismerkintä peruskoulusta (tai vastaavasta) 31.12.2020 tai aiemmin
    val valmistunutPeruskoulustaEnnen2021 = perusopetuksenAikavälit.exists(p => p.loppu.exists(_.isBefore(aikaraja)))

    // Peruskoulun jälkeisen koulutuksen suoritus on alkanut 31.12.2020 tai aiemmin
    val peruskoulunJälkeinenKoulutusAlkannutEnnen2021 = !peruskoulunJälkeinenKoulutusAlkanutAikaisintaan2021

    val oppijanIkäOikeuttaaMaksuttomuuden = oppijanSyntymäpäivä.exists(bd => !LocalDate.of(2004, 1, 1).isAfter(bd))

    val maksuttomuustiedotVaaditaan =
      oppijanHetu.isDefined &&
        peruskoulussaVuoden2021Alussa &&
        peruskoulunJälkeinenKoulutusAlkanutAikaisintaan2021 &&
        koulutusOppivelvollisuuskoulutukseksiKelpaavaa

    // Tilanteet, joissa maksuttomuustietoja ei saa siirtää. Jos tuplen ensimmäinen arvo on true, ehto aktivoituu ja toinen arvon kertoo syyn.
    val maksuttomuustietoEiSallittuSyyt = List(
      (oppijanHetu.isEmpty, "oppijalla ei ole henkilötunnusta"),
      (valmistunutPeruskoulustaEnnen2021, "oppija on suorittanut perusopetuksen, aikuisten perusopetuksen oppimäärän tai International Schoolin 9. vuosiluokan ennen 1.1.2021"),
      (peruskoulunJälkeinenKoulutusAlkannutEnnen2021, "koulutuksen suoritus on alkanut ennen 1.1.2021"),
      (oppijanSyntymäpäivä.isEmpty, "oppijan syntymäpäivä ei ole tiedossa"),
      (oppijanSyntymäpäivä.isDefined && !oppijanIkäOikeuttaaMaksuttomuuden, "oppija on syntynyt ennen vuotta 2004"),
      (!koulutusOppivelvollisuuskoulutukseksiKelpaavaa, "koulutus ei ole peruskoulun jälkeiseksi oppivelvollisuuskoulutukseksi kelpaavaa (esim. väärä diaarinumero tai opiskeluoikeus- tai suoritustyyppi)")
    ).filter(_._1).map(_._2)

    if (maksuttomuustiedotVaaditaan) {
      HttpStatus.validate(maksuttomuustietoSiirretty) { KoskiErrorCategory.badRequest.validation("Tieto koulutuksen maksuttomuudesta puuttuu.") }
    } else if (maksuttomuustietoEiSallittuSyyt.nonEmpty) {
      val maksuttomuustietojaSiirretty = maksuttomuustietoSiirretty || maksuttomuudenPidennysSiirretty
      HttpStatus.validate(!maksuttomuustietojaSiirretty) {
        val syyt = maksuttomuustietoEiSallittuSyyt.mkString(" ja ")
        KoskiErrorCategory.badRequest.validation(s"Tieto koulutuksen maksuttomuudesta ei ole relevantti tässä opiskeluoikeudessa, sillä $syyt.")
      }
    } else {
      HttpStatus.ok
    }
  }

  // Huom! Valpas käyttää myös tätä funktiota!
  def oppivelvollisuudenSuorittamiseenKelpaavaMuuKuinPeruskoulunOpiskeluoikeus(opiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus): Boolean =
    opiskeluoikeus.suoritukset.collectFirst {
      case myp: MYPVuosiluokanSuoritus if myp.koulutusmoduuli.tunniste.koodiarvo == "10" => myp
      case s: SuoritusVaatiiMahdollisestiMaksuttomuusTiedonOpiskeluoikeudelta => s
    }.isDefined

  def validateAndFillJaksot(opiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus): Either[HttpStatus, KoskeenTallennettavaOpiskeluoikeus] = {
   opiskeluoikeus.lisätiedot.collect {
     case lisätiedot: MaksuttomuusTieto => {
       val oikeuttaMaksuttomuuteenPidennetty = sortJaksonAlkupäivänMukaan(lisätiedot.oikeuttaMaksuttomuuteenPidennetty.toList.flatten)
       val maksuttomuus = sortJaksonAlkupäivänMukaan(lisätiedot.maksuttomuus.toList.flatten)

       for {
         validMaksuttomuus <- validateAndFillMaksuttomuusJaksot(maksuttomuus, opiskeluoikeus)
         validMaksuttomuuttaPidennetty <- validateMaksuttomuuttaPidennetty(oikeuttaMaksuttomuuteenPidennetty, validMaksuttomuus, opiskeluoikeus)
       } yield (
         opiskeluoikeus
           .withLisätiedot(Some(lisätiedot
             .withMaksuttomus(toOptional(validMaksuttomuus))
             .withOikeuttaMaksuttomuuteenPidennetty(toOptional(validMaksuttomuuttaPidennetty))
           ))
         )
     }
   }.getOrElse(Right(opiskeluoikeus))
  }

  private def sortJaksonAlkupäivänMukaan[A <: Alkupäivällinen](jaksot: List[A]): List[A] = jaksot.sortBy(_.alku)(DateOrdering.localDateOrdering)

  private def validateAndFillMaksuttomuusJaksot(jaksot: List[Maksuttomuus], opiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus) = {
    val voimassaolonUlkopuolella = jaksot.map(_.alku).filterNot(d => between(opiskeluoikeus.alkamispäivä, opiskeluoikeus.päättymispäivä)(d))
    val samojaAlkamispäiviä = jaksot.map(_.alku).groupBy(x => x).filter(_._2.length > 1).values.flatten.toSeq

    val validationResult = HttpStatus.fold(
      validate(voimassaolonUlkopuolella)(x => KoskiErrorCategory.badRequest.validation(s"Opiskeluoikeudella on koulutuksen maksuttomuusjaksoja, jonka alkupäivä ${x.map(_.toString).mkString(", ")} ei ole opiskeluoikeuden voimassaolon (${voimassaolo(opiskeluoikeus)}) sisällä")),
      validate(samojaAlkamispäiviä)(x => KoskiErrorCategory.badRequest.validation(s"Opiskeluoikeudella on koulutuksen maksuttomuusjaksoja, joilla on sama alkupäivä ${x.map(_.toString).mkString(", ")}"))
    )

    if (validationResult.isOk) Right(fillPäättymispäivät(jaksot)) else Left(validationResult)
  }

  private def validateMaksuttomuuttaPidennetty(jaksot: List[OikeuttaMaksuttomuuteenPidennetty], maksuttomuus: List[Maksuttomuus], opiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus) = {
    def betweenOpiskeluoikeudenAlkamisPäättymis(jakso: OikeuttaMaksuttomuuteenPidennetty) = {
      val voimassaolonSisällä = between(opiskeluoikeus.alkamispäivä, opiskeluoikeus.päättymispäivä)_
      voimassaolonSisällä(jakso.alku) && voimassaolonSisällä(jakso.loppu)
    }

    val voimassaolonUlkopuolella = jaksot.filterNot(betweenOpiskeluoikeudenAlkamisPäättymis)
    val jaksonAlkuEnnenLoppua = jaksot.filterNot(jakso => !jakso.alku.isAfter(jakso.loppu))
    val päällekkäisiäJaksoja = jaksot.zip(jaksot.drop(1)).filter { case (a,b) => a.overlaps(b) }

    val maksuttomatMaksuttomuusJaksot = maksuttomuus.filter(_.maksuton)
    val pidennysMaksuttomuudenUlkopuolella = jaksot.filterNot(pidennys => maksuttomatMaksuttomuusJaksot.exists(maksuton => maksuton.containsPidennysJakso(pidennys)))

    val validationResult = HttpStatus.fold(
      validate(voimassaolonUlkopuolella)(x => KoskiErrorCategory.badRequest.validation(s"Opiskeluoikeudella on koulutuksen maksuttomuuden pidennykseen liittyvä jakso, jonka alku- ja/tai loppupäivä ei ole opiskeluoikeuden voimassaolon (${voimassaolo(opiskeluoikeus)}) sisällä ${x.map(_.toString).mkString(", ")}")),
      validate(jaksonAlkuEnnenLoppua)(x => KoskiErrorCategory.badRequest.validation(s"Opiskeluoikeudella on koulutuksen maksuttomuuden pidennykseen liittyvä jakso, jonka loppupäivä on aikaisemmin kuin alkupäivä. ${x.map(y => s"${y.alku} (alku) - ${y.loppu} (loppu)").mkString(", ")}")),
      validate(päällekkäisiäJaksoja)(x => KoskiErrorCategory.badRequest.validation(s"Opiskeluoikeudella on koulutuksen maksuttomuuden pidennykseen liittyviä jaksoja, jotka ovat keskenään päällekkäisiä ${x.map(_.toString).mkString(", ")}")),
      validate(pidennysMaksuttomuudenUlkopuolella)(x => KoskiErrorCategory.badRequest.validation(s"Maksuttomuutta voidaan pidetäntää vain aikavälillä jolloin koulutus on maksutontonta"))
    )

    if (validationResult.isOk) Right(jaksot) else Left(validationResult)
  }

  private def fillPäättymispäivät(maksuttomuus: List[Maksuttomuus]) = {
    val jaksot = maksuttomuus.map(_.copy(loppu = None))
    val last = jaksot.lastOption.toList
    val filled = jaksot.zip(jaksot.drop(1)).map { case (a, b) => a.copy(loppu = Some(b.alku.minusDays(1))) }
    filled ::: last
  }

  private def validate[A](virheelliset: Seq[A])(virheviesti: Seq[A] => HttpStatus) =
    if (virheelliset.length > 0) virheviesti(virheelliset) else HttpStatus.ok

  private def between(start: Option[LocalDate], end: Option[LocalDate])(date: LocalDate) =
    start.map(alku => !date.isBefore(alku)).getOrElse(false) && end.map(loppu => !date.isAfter(loppu)).getOrElse(true)

  private def voimassaolo(opiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus) =
    s"${opiskeluoikeus.alkamispäivä.map(_.toString).getOrElse("")} - ${opiskeluoikeus.päättymispäivä.map(_.toString).getOrElse("")}"

  private def toOptional[A](xs: List[A]): Option[List[A]] = if (xs.isEmpty) None else Some(xs)
}
