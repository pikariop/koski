package fi.oph.koski.etk

import java.sql.Timestamp
import java.time.{Instant, LocalDate}

import scala.io.Source

object ElaketurvakeskusCsvParser {

  private val separator = ";"

  private val hetu = "hetu"
  private val syntymaaika = "syntymaaika"
  private val sukunimi = "sukunimi"
  private val etunimet = "etunimet"
  private val tutkinnon_taso = "tutkinnon_taso"
  private val OpiskeluoikeudenAlkamispaivamaara = "OpiskeluoikeudenAlkamispaivamaara"
  private val suorituspaivamaara = "suorituspaivamaara"
  private val oppijanumero = "oppijanumero"

  def parse(filepath: String): Option[EtkResponse] = {
    val csv = Source.fromFile(filepath).getLines().toList
    val headLine = validateHeading(csv.head.split(separator))
    val csvLegend = headLine.zipWithIndex.toMap

    val vuosi = csv.drop(1).head.split(separator)(0).toInt
    val tutkintotiedot = csv.drop(1).map(toEtkTutkintotieto(_, csvLegend, headLine.size))

    Some(EtkResponse(
      vuosi = vuosi,
      tutkintojenLkm = tutkintotiedot.size,
      tutkinnot = tutkintotiedot,
      aikaleima = Timestamp.from(Instant.now)
    ))
  }

  private def validateHeading(headings: Array[String]) = {
    List(hetu, syntymaaika, etunimet, tutkinnon_taso, OpiskeluoikeudenAlkamispaivamaara, suorituspaivamaara, oppijanumero).filterNot(headings.contains) match {
      case Nil => headings //ok
      case notFound => throw new Error(s"Csv tiedostosta puuttuu haluttuja otsikoita. $notFound")
    }
  }

  private def toEtkTutkintotieto(row: String, csvLegend: Map[String, Int], expectedFieldCount: Int) = {
    val fields = row.split(separator, -1)

    if (fields.size != expectedFieldCount) {
      throw new Exception(s"Riviltä puuttuu kenttiä: ${row}")
    }

    def field(fieldName: String) = fields(csvLegend(fieldName))

    def fieldOpt(fieldName: String) = field(fieldName) match {
      case "" => None
      case str@_ => Some(str)
    }

    EtkTutkintotieto(
      henkilö = EtkHenkilö(
        hetu = fieldOpt(hetu),
        syntymäaika = fieldOpt(syntymaaika).map(LocalDate.parse),
        sukunimi = field(sukunimi),
        etunimet = field(etunimet)
      ),
      tutkinto = EtkTutkinto(
        tutkinnonTaso = fieldOpt(tutkinnon_taso),
        alkamispäivä = fieldOpt(OpiskeluoikeudenAlkamispaivamaara).map(LocalDate.parse),
        päättymispäivä = fieldOpt(suorituspaivamaara).map(LocalDate.parse)
      ),
      viite = fieldOpt(oppijanumero) match {
        case Some(oppijanumero) => Some(
          EtkViite(
            opiskeluoikeusOid = None,
            opiskeluoikeusVersionumero = None,
            oppijaOid = Some(oppijanumero)
          )
        )
        case _ => None
      }
    )
  }
}