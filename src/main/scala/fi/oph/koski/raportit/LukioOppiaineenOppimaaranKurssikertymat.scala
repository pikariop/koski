package fi.oph.koski.raportit

import java.sql.ResultSet
import java.time.LocalDate

import fi.oph.koski.db.DatabaseConverters
import fi.oph.koski.db.PostgresDriverWithJsonSupport.plainAPI._
import fi.oph.koski.raportointikanta.RaportointiDatabase
import slick.jdbc.GetResult

object LukioOppiaineenOppimaaranKurssikertymat extends DatabaseConverters {
  val sheetTitle = "Aineopiskelijat"

  def datasheet(oppilaitosOids: List[String], jaksonAlku: LocalDate, jaksonLoppu: LocalDate, raportointiDatabase: RaportointiDatabase): DataSheet = {
    DataSheet(
      sheetTitle,
      rows = raportointiDatabase.runDbSync(queryAineopiskelija(oppilaitosOids, jaksonAlku, jaksonLoppu)),
      columnSettings
    )
  }

  private def queryAineopiskelija(oppilaitosOids: List[String], aikaisintaan: LocalDate, viimeistaan: LocalDate) = {
    sql"""
      with oppiaineen_oppimaara as (
        select
          r_opiskeluoikeus.oppilaitos_oid,
          r_opiskeluoikeus.oppilaitos_nimi,
          r_paatason_suoritus.paatason_suoritus_id
        from r_opiskeluoikeus
        join r_paatason_suoritus on r_opiskeluoikeus.opiskeluoikeus_oid = r_paatason_suoritus.opiskeluoikeus_oid
        where
          oppilaitos_oid = any($oppilaitosOids)
          and r_paatason_suoritus.suorituksen_tyyppi = 'lukionoppiaineenoppimaara'
          and exists(
            select 1
            from r_opiskeluoikeus_aikajakso
            where r_opiskeluoikeus.opiskeluoikeus_oid = r_opiskeluoikeus_aikajakso.opiskeluoikeus_oid
              and r_opiskeluoikeus_aikajakso.tila = 'lasna'
              and r_opiskeluoikeus_aikajakso.alku <= $viimeistaan
              and r_opiskeluoikeus_aikajakso.loppu >= $aikaisintaan
          )
      ) select
          oppilaitos_oid oppilaitos_oid,
          oppilaitos_nimi oppilaitos,
          count(*) yhteensa,
          count(*) filter (where tunnustettu = false) suoritettuja,
          count(*) filter (where tunnustettu) tunnustettuja,
          count(*) filter (where tunnustettu_rahoituksen_piirissa) tunnustettuja_rahoituksen_piirissa,
          count(*) filter (where
            koulutusmoduuli_kurssin_tyyppi = 'pakollinen'
             or
            (koulutusmoduuli_kurssin_tyyppi = 'syventava' and koulutusmoduuli_paikallinen = false)
          ) pakollisia_tai_valtakunnallisia_syventavia,
          count(*) filter (where koulutusmoduuli_kurssin_tyyppi = 'pakollinen') pakollisia,
          count(*) filter (where
            koulutusmoduuli_paikallinen = false and
            koulutusmoduuli_kurssin_tyyppi = 'syventava'
          ) valtakunnallisia_syventavia,
          count(*) filter (where
            tunnustettu = false and (
            koulutusmoduuli_kurssin_tyyppi = 'pakollinen'
              or
            (koulutusmoduuli_kurssin_tyyppi = 'syventava' and koulutusmoduuli_paikallinen = false))
          ) suoritettuja_pakollisia_ja_valtakunnallisiaSyventavia,
          count(*) filter (where
            koulutusmoduuli_kurssin_tyyppi = 'pakollinen' and
            tunnustettu = false
          ) suoritettuja_pakollisia,
          count(*) filter (where
            koulutusmoduuli_paikallinen = false and
            koulutusmoduuli_kurssin_tyyppi = 'syventava' and
            tunnustettu = false
          ) suoritettuja_valtakunnallisia_syventavia,
          count(*) filter (where
            koulutusmoduuli_kurssin_tyyppi = 'pakollinen' and
            tunnustettu
          ) tunnustettuja_pakollisia,
          count(*) filter (where
            koulutusmoduuli_paikallinen = false and
            koulutusmoduuli_kurssin_tyyppi = 'syventava' and
            tunnustettu
          ) tunnustettuja_valtakunnallisia_syventavia,
          count(*) filter (where
            tunnustettu_rahoituksen_piirissa and
            koulutusmoduuli_kurssin_tyyppi = 'pakollinen'
          ) pakollisia_tunnustettuja_rahoituksen_piirissa,
          count(*) filter (where
            koulutusmoduuli_paikallinen = false and
            koulutusmoduuli_kurssin_tyyppi = 'syventava' and
            tunnustettu_rahoituksen_piirissa
          ) valtakunnallisia_syventavia_tunnustettuja_rahoituksen_piirissa
        from oppiaineen_oppimaara
        join r_osasuoritus on oppiaineen_oppimaara.paatason_suoritus_id = r_osasuoritus.paatason_suoritus_id
        where r_osasuoritus.suorituksen_tyyppi = 'lukionkurssi'
         and  r_osasuoritus.arviointi_paiva >= $aikaisintaan
         and  r_osasuoritus.arviointi_paiva <= $viimeistaan
        group by oppiaineen_oppimaara.oppilaitos_oid, oppiaineen_oppimaara.oppilaitos_nimi;
    """.as[LukioKurssikertymaAineopiskelijaRow]
  }

  implicit private val getResult: GetResult[LukioKurssikertymaAineopiskelijaRow] = GetResult(r => {
    val rs: ResultSet = r.rs
    LukioKurssikertymaAineopiskelijaRow(
      oppilaitosOid = rs.getString("oppilaitos_oid"),
      oppilaitos = rs.getString("oppilaitos"),
      kurssejaYhteensa = rs.getInt("yhteensa"),
      suoritettujaKursseja = rs.getInt("suoritettuja"),
      tunnustettujaKursseja = rs.getInt("tunnustettuja"),
      tunnustettujaKursseja_rahoituksenPiirissa = rs.getInt("tunnustettuja_rahoituksen_piirissa"),
      pakollisia_tai_valtakunnallisiaSyventavia = rs.getInt("pakollisia_tai_valtakunnallisia_syventavia"),
      pakollisiaKursseja = rs.getInt("pakollisia"),
      valtakunnallisestiSyventaviaKursseja = rs.getInt("valtakunnallisia_syventavia"),
      suoritettujaPakollisia_ja_suoritettujaValtakunnallisiaSyventavia = rs.getInt("suoritettuja_pakollisia_ja_valtakunnallisiaSyventavia"),
      suoritettujaPakollisiaKursseja = rs.getInt("suoritettuja_pakollisia"),
      suoritettujaValtakunnallisiaSyventaviaKursseja = rs.getInt("suoritettuja_valtakunnallisia_syventavia"),
      tunnustettujaPakollisiaKursseja = rs.getInt("tunnustettuja_pakollisia"),
      tunnustettujaValtakunnallisiaSyventaviaKursseja = rs.getInt("tunnustettuja_valtakunnallisia_syventavia"),
      tunnustettuja_rahoituksenPiirissa_pakollisia = rs.getInt("pakollisia_tunnustettuja_rahoituksen_piirissa"),
      tunnustettuja_rahoituksenPiirissa_valtakunnallisiaSyventaiva = rs.getInt("valtakunnallisia_syventavia_tunnustettuja_rahoituksen_piirissa")
    )
  })

  val columnSettings: Seq[(String, Column)] = Seq(
    "oppilaitosOid" -> Column("Oppilaitoksen oid-tunniste"),
    "oppilaitos" -> Column("Oppilaitos"),
    "kurssejaYhteensa" -> Column("kurssejaYhteensa"),
    "suoritettujaKursseja" -> Column("suoritettujaKursseja"),
    "tunnustettujaKursseja" -> Column("tunnustettujaKursseja"),
    "tunnustettujaKursseja_rahoituksenPiirissa" -> Column("tunnustettujaKursseja_rahoituksenPiirissa"),
    "pakollisia_tai_valtakunnallisiaSyventavia" -> Column("pakollisia_tai_valtakunnallisiaSyventavia"),
    "pakollisiaKursseja" -> Column("pakollisiaKursseja"),
    "valtakunnallisestiSyventaviaKursseja" -> Column("valtakunnallisestiSyventaviaKursseja"),
    "suoritettujaPakollisia_ja_suoritettujaValtakunnallisiaSyventavia" -> Column("suoritettujaPakollisia_ja_suoritettujaValtakunnallisiaSyventavia"),
    "suoritettujaPakollisiaKursseja" -> Column("suoritettujaPakollisiaKursseja"),
    "suoritettujaValtakunnallisiaSyventaviaKursseja" -> Column("suoritettujaValtakunnallisiaSyventaviaKursseja"),
    "tunnustettujaPakollisiaKursseja" -> Column("tunnustettujaPakollisiaKursseja"),
    "tunnustettujaValtakunnallisiaSyventaviaKursseja" -> Column("tunnustettujaValtakunnallisiaSyventaviaKursseja"),
    "tunnustettuja_rahoituksenPiirissa_pakollisia" -> Column("tunnustettuja_rahoituksenPiirissa_pakollisia"),
    "tunnustettuja_rahoituksenPiirissa_valtakunnallisiaSyventaiva" -> Column("tunnustettuja_rahoituksenPiirissa_valtakunnallisiaSyventaiva")
  )
}

case class LukioKurssikertymaAineopiskelijaRow(
  oppilaitosOid: String,
  oppilaitos: String,
  kurssejaYhteensa: Int,
  suoritettujaKursseja: Int,
  tunnustettujaKursseja: Int,
  tunnustettujaKursseja_rahoituksenPiirissa: Int,
  pakollisia_tai_valtakunnallisiaSyventavia: Int,
  pakollisiaKursseja: Int,
  valtakunnallisestiSyventaviaKursseja: Int,
  suoritettujaPakollisia_ja_suoritettujaValtakunnallisiaSyventavia: Int,
  suoritettujaPakollisiaKursseja: Int,
  suoritettujaValtakunnallisiaSyventaviaKursseja: Int,
  tunnustettujaPakollisiaKursseja: Int,
  tunnustettujaValtakunnallisiaSyventaviaKursseja: Int,
  tunnustettuja_rahoituksenPiirissa_pakollisia: Int,
  tunnustettuja_rahoituksenPiirissa_valtakunnallisiaSyventaiva: Int
)
