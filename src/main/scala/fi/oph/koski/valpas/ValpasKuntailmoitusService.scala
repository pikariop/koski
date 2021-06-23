package fi.oph.koski.valpas

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.henkilo.{LaajatOppijaHenkilöTiedot, Yhteystiedot}
import fi.oph.koski.http.HttpStatus
import fi.oph.koski.log.Logging
import fi.oph.koski.schema._
import fi.oph.koski.util.Timing
import fi.oph.koski.valpas.opiskeluoikeusrepository.{ValpasOppijaLaajatTiedot, ValpasOppilaitos}
import fi.oph.koski.valpas.valpasrepository._
import fi.oph.koski.valpas.valpasuser.{ValpasRooli, ValpasSession}
import fi.oph.koski.valpas.yhteystiedot.{ValpasYhteystiedot, ValpasYhteystietoHakemukselta, ValpasYhteystietoOppijanumerorekisteristä}

class ValpasKuntailmoitusService(
  application: KoskiApplication
) extends Logging with Timing {
  private val accessResolver = new ValpasAccessResolver
  private val repository = application.valpasKuntailmoitusRepository
  private val oppijaService = application.valpasOppijaService
  private val directoryClient = application.directoryClient
  private val oppijanumerorekisteri = application.opintopolkuHenkilöFacade
  private val koodistoViitePalvelu = application.koodistoViitePalvelu
  private val organisaatioService = application.organisaatioService
  private val organisaatioRepository = application.organisaatioRepository

  def createKuntailmoitus(
    kuntailmoitusInput: ValpasKuntailmoitusLaajatTiedotJaOppijaOid
  )(implicit session: ValpasSession): Either[HttpStatus, ValpasKuntailmoitusLaajatTiedotJaOppijaOid] = {
    val organisaatioOid = kuntailmoitusInput.kuntailmoitus.tekijä.organisaatio.oid

    val rooli = kuntailmoitusInput.kuntailmoitus.tekijä.organisaatio match {
      case o: OrganisaatioWithOid if organisaatioRepository.isKunta(o) => Right(ValpasRooli.KUNTA)
      case _: Oppilaitos => Right(ValpasRooli.OPPILAITOS_HAKEUTUMINEN)
      case o: Any => Left(ValpasErrorCategory.validation.kuntailmoituksenTekijä(
        s"Organisaatio ${o.oid} ei voi olla kuntailmoituksen tekijä (organisaation tyyppi ei ole sallittu)"
      ))
    }

    for {
      r <- rooli
      _ <-
        accessResolver.assertAccessToOrg(r, organisaatioOid)
          .left
          .map(_ => ValpasErrorCategory.forbidden.organisaatio(
            "Käyttäjällä ei ole oikeutta tehdä kuntailmoitusta annetun organisaation nimissä"
          ))
      o <- oppijaService.getOppijaLaajatTiedot(r, kuntailmoitusInput.oppijaOid)
      _ <-
        accessResolver.withOppijaAccessAsOrganisaatio(r, organisaatioOid)(o)
          .left
          .map(_ => ValpasErrorCategory.forbidden.oppija(
            "Käyttäjällä ei ole oikeuksia tehdä kuntailmoitusta annetusta oppijasta"
          ))
      result <- repository.create(kuntailmoitusInput)
    } yield result
  }

  def getKuntailmoitukset(
    oppija: ValpasOppijaLaajatTiedot
  )(implicit session: ValpasSession): Either[HttpStatus, Seq[ValpasKuntailmoitusLaajatTiedot]] = {
    accessResolver.withOppijaAccess(oppija)
      .flatMap(oppija => repository.queryOppijat(oppija.henkilö.kaikkiOidit))
      .map(_.map(karsiHenkilötiedotJosEiOikeuksia))
  }

  def getKuntailmoituksetKunnalle
    (kuntaOid: Organisaatio.Oid)
    (implicit session: ValpasSession)
  : Either[HttpStatus, Seq[ValpasKuntailmoitusLaajatTiedotJaOppijaOid]] = {
    repository.queryByKunta(kuntaOid)
  }

  private def karsiHenkilötiedotJosEiOikeuksia
    (kuntailmoitus: ValpasKuntailmoitusLaajatTiedot)
    (implicit session: ValpasSession)
  : ValpasKuntailmoitusLaajatTiedot = {
    val oikeutetutOrganisaatiot = Set(kuntailmoitus.tekijä.organisaatio.oid, kuntailmoitus.kunta.oid)

    if (Seq(ValpasRooli.OPPILAITOS_HAKEUTUMINEN, ValpasRooli.KUNTA)
      .exists(rooli => accessResolver.accessToSomeOrgs(rooli, oikeutetutOrganisaatiot))
    ) {
      kuntailmoitus
    } else {
      kuntailmoitus.copy(
        tekijä = kuntailmoitus.tekijä.copy(
          henkilö = None
        ),
        yhteydenottokieli = None,
        oppijanYhteystiedot = None,
        hakenutMuualle = None
      )
    }
  }

  def haePohjatiedot(
    pohjatiedotInput: ValpasKuntailmoitusPohjatiedotInput
  )(implicit session: ValpasSession): Either[HttpStatus, ValpasKuntailmoitusPohjatiedot] = {
    val kunnat = organisaatioService.kunnat()
    val maat = haeMaat()

    for {
      tekijä <- tekijänKäyttäjätiedot()
      oppijat <- haeOppijat(pohjatiedotInput)
        .flatMap(tarkistaOikeudetJaJärjestäOppijat(pohjatiedotInput))
        .flatMap(oppijoidenPohjatiedot(pohjatiedotInput.tekijäOrganisaatio, maat, kunnat))
    } yield {
      ValpasKuntailmoitusPohjatiedot(
        tekijäHenkilö = tekijä,
        mahdollisetTekijäOrganisaatiot =
          oppijat.map(_.mahdollisetTekijäOrganisaatiot).map(_.toSet)
            .reduceLeft((a, b) => a.intersect(b))
            .toSeq,
        oppijat = oppijat,
        kunnat = kunnat,
        maat = maat,
        yhteydenottokielet = tuetutYhteydenottokielet()
      )
    }
  }

  private def tekijänKäyttäjätiedot()(implicit session: ValpasSession)
  : Either[HttpStatus, ValpasKuntailmoituksenTekijäHenkilö] = {
    for {
      user <- directoryClient
        .findUser(session.username)
        .toRight(ValpasErrorCategory.internalError("Käyttäjän tietoja ei saatu haettua"))
    } yield {
      val oppijanumerorekisteriTiedot = haeOppijanumerorekisteriTiedot(user.oid)

      ValpasKuntailmoituksenTekijäHenkilö(
        oid = Some(session.oid),
        etunimet = Some(user.etunimet),
        sukunimi = Some(user.sukunimi),
        kutsumanimi = oppijanumerorekisteriTiedot.map(_.kutsumanimi),
        email = oppijanumerorekisteriTiedot.flatMap(emails),
        puhelinnumero = oppijanumerorekisteriTiedot.flatMap(puhelinnumerot)
      )
    }
  }

  private def haeOppijanumerorekisteriTiedot(userOid: String): Option[LaajatOppijaHenkilöTiedot] =
    oppijanumerorekisteri.findOppijaJaYhteystiedotByOid(userOid)

  private def työosoitteet(tiedot: LaajatOppijaHenkilöTiedot) =
    tiedot.yhteystiedot.filter(_.tyyppi.koodiarvo == "yhteystietotyyppi2") // "Työosoite"

  private def emails(tiedot: LaajatOppijaHenkilöTiedot) = {
    val emails = työosoitteet(tiedot)
      .flatMap(_.sähköposti)
      .map(_.trim)
      .filter(_ != "")
    if (emails.isEmpty) None else Some(emails.distinct.sorted.mkString(", "))
  }

  private def puhelinnumerot(tiedot: LaajatOppijaHenkilöTiedot) = {
    val puhelinnumerot = työosoitteet(tiedot)
      .flatMap(o => Seq(o.matkapuhelinnumero, o.puhelinnumero).flatten)
      .map(_.trim)
      .filter(_ != "")
    if (puhelinnumerot.isEmpty) None else Some(puhelinnumerot.distinct.sorted.mkString(", "))
  }

  private def haeMaat()(implicit session: ValpasSession): Seq[Koodistokoodiviite] = {
    val koodistoUri = "maatjavaltiot2"
    val koodisto = koodistoViitePalvelu.getLatestVersionOptional(koodistoUri)
    koodisto match {
      case Some(koodisto) => koodistoViitePalvelu.getKoodistoKoodiViitteet(koodisto)
      case _ =>
        logger.warn("Koodistoa ei löydy koodistopalvelusta: " + koodistoUri)
        Seq.empty
    }
  }

  private def tuetutYhteydenottokielet(): Seq[Koodistokoodiviite] = Seq(
    Koodistokoodiviite("FI", "kieli"),
    Koodistokoodiviite("SV", "kieli")
  ).map(koodistoViitePalvelu.validate).flatten

  private def haeOppijat(
    pohjatiedotInput: ValpasKuntailmoitusPohjatiedotInput
  )(implicit session: ValpasSession): Either[HttpStatus, Seq[OppijaHakutilanteillaLaajatTiedot]] = {
    pohjatiedotInput.tekijäOrganisaatio match {
      case Some(oppilaitos) => haeOppilaitoksenOppijat(oppilaitos.oid, pohjatiedotInput.oppijaOidit)
      case _ => haeYksittäisetOppijat(pohjatiedotInput.oppijaOidit)
    }
  }

  private def haeOppilaitoksenOppijat(
    oppilaitosOid: ValpasOppilaitos.Oid, oppijaOidit: Seq[String]
  )(implicit session: ValpasSession): Either[HttpStatus, Seq[OppijaHakutilanteillaLaajatTiedot]] = {
    oppijaService.getOppijatLaajatTiedotYhteystiedoilla(oppilaitosOid, oppijaOidit)
  }

  private def haeYksittäisetOppijat(
    oppijaOidit: Seq[String]
  )(implicit session: ValpasSession): Either[HttpStatus, Seq[OppijaHakutilanteillaLaajatTiedot]] = {
    // Tämä hakeminen aiheuttaa monta SQL-queryä. Tätä voisi optimoida, mutta käytännössä tähän metodiin ei toistaiseksi
    // koskaan päädytä kuin yhden oppijan näkymästä, koska listanäkymässä ilmoituksia tehtäessä tekijän
    // oppilaitos on aina tiedossa.
    HttpStatus.foldEithers(oppijaOidit.map(oppijaOid => oppijaService.getOppijaLaajatTiedotYhteystiedoilla(oppijaOid)).toSeq)
  }

  private def tarkistaOikeudetJaJärjestäOppijat(pohjatiedotInput: ValpasKuntailmoitusPohjatiedotInput)(
    oppijatHakutilanteilla: Seq[OppijaHakutilanteillaLaajatTiedot]
  )(implicit session: ValpasSession): Either[HttpStatus, Seq[OppijaHakutilanteillaLaajatTiedot]] = {
    def lessThanInputinJärjestyksenMukaan(
      a: OppijaHakutilanteillaLaajatTiedot,
      b: OppijaHakutilanteillaLaajatTiedot
    ): Boolean = {
      pohjatiedotInput.oppijaOidit.indexOf(a.oppija.henkilö.oid) < pohjatiedotInput.oppijaOidit.indexOf(b.oppija.henkilö.oid)
    }

    // Käyttäjällä pitää olla oikeus tehdä kuntailmoitus jokaiselle oppijalle:
    // Oikeus on kaikilla muilla paitsi maksuttomuuskäyttäjillä.
    HttpStatus.foldEithers(
      oppijatHakutilanteilla.map(oppijaHakutilanteilla =>
        accessResolver.withOppijaAccess(
          Seq(
            ValpasRooli.OPPILAITOS_HAKEUTUMINEN,
            ValpasRooli.OPPILAITOS_SUORITTAMINEN,
            ValpasRooli.KUNTA
          ),
          oppijaHakutilanteilla.oppija
        )
      )
    ).flatMap(_ => {
      // Tarkista, että jokainen inputtina annettu oppija löytyy saadulta listalta samalla oppija-oidilla. Oppijoiden
      // master-slave-oideja ei tässä siis käsitellä, vaan palautetaan vaan virhe, jos kysely on tehty slave oidilla.
      // Tämä ei käytännössä haittaa, koska käyttöliittymä perustuu aina master-oideina palautettuun oppija-dataan.
      val oppijaOidsTietokannasta = oppijatHakutilanteilla.map(_.oppija.henkilö.oid)
      if (pohjatiedotInput.oppijaOidit.toSet == oppijaOidsTietokannasta.toSet) {
        // Järjestä oppijat samaan järjestykseen kuin pyynnössä. Melko tehoton sorttaus, mutta dataa ei ole paljon.
        Right(oppijatHakutilanteilla.sortWith(lessThanInputinJärjestyksenMukaan))
      } else {
        Left(ValpasErrorCategory.forbidden.oppijat("Käyttäjällä ei ole oikeuksia kaikkien oppijoiden tietoihin"))
      }
    })
  }

  private def oppijoidenPohjatiedot
    (tekijäOrganisaatio: Option[OrganisaatioWithOid], maat: Seq[Koodistokoodiviite], kunnat: Seq[OrganisaatioWithOid])
    (oppijat: Seq[OppijaHakutilanteillaLaajatTiedot])
    (implicit session: ValpasSession)
  : Either[HttpStatus, Seq[ValpasOppijanPohjatiedot]] = {
    HttpStatus.foldEithers(oppijat.map(oppija => {
      mahdollisetTekijäOrganisaatiot(tekijäOrganisaatio, oppija.oppija.hakeutumisvalvovatOppilaitokset)
        .map(organisaatiot =>
          ValpasOppijanPohjatiedot(
            oppijaOid = oppija.oppija.henkilö.oid,
            mahdollisetTekijäOrganisaatiot = organisaatiot.toSeq,
            yhteydenottokieli = yhteydenottokieli(oppija.oppija.henkilö.äidinkieli),
            turvakielto = oppija.oppija.henkilö.turvakielto,
            yhteystiedot = teeYhteystiedot(oppija.yhteystiedot, maat, kunnat),
            hetu = oppija.oppija.henkilö.hetu
          )
        )
    }))
  }

  private def teeYhteystiedot(
    oppijaYhteystiedot: Seq[ValpasYhteystiedot],
    maat: Seq[Koodistokoodiviite],
    kunnat: Seq[OrganisaatioWithOid]
  ): Seq[ValpasPohjatietoYhteystieto] = {
    val yhteystiedot = oppijaYhteystiedot.map(yhteystiedotOppijanYhteystiedoista(maat, kunnat))

    val hakemusYhteystiedot = yhteystiedot.filter(_.yhteystietojenAlkuperä.isInstanceOf[ValpasYhteystietoHakemukselta])
    val oppijanumerorekisterinYhteystiedot =
      yhteystiedot.filter(_.yhteystietojenAlkuperä.isInstanceOf[ValpasYhteystietoOppijanumerorekisteristä])
    val muutYhteystiedot = yhteystiedot.diff(hakemusYhteystiedot ++ oppijanumerorekisterinYhteystiedot)

    hakemusYhteystiedot ++ oppijanumerorekisterinYhteystiedot ++ muutYhteystiedot
  }

  private def yhteystiedotOppijanYhteystiedoista
    (maat: Seq[Koodistokoodiviite], kunnat: Seq[OrganisaatioWithOid])
    (valpasYhteystiedot: ValpasYhteystiedot)
  : ValpasPohjatietoYhteystieto = {
    val maa = maaOppijanYhteystiedoista(maat, valpasYhteystiedot)
    val kunta = kuntaOppijanYhteystiedoista(kunnat, valpasYhteystiedot)

    ValpasPohjatietoYhteystieto(
      yhteystietojenAlkuperä = valpasYhteystiedot.alkuperä,
      yhteystiedot = ValpasKuntailmoituksenOppijanYhteystiedot(
        puhelinnumero = valpasYhteystiedot.matkapuhelinnumero.orElse(valpasYhteystiedot.puhelinnumero),
        email = valpasYhteystiedot.sähköposti,
        lähiosoite = valpasYhteystiedot.lähiosoite,
        postinumero = valpasYhteystiedot.postinumero,
        postitoimipaikka = valpasYhteystiedot.postitoimipaikka,
        maa = maa
      ),
      kunta = kunta
    )
  }

  private def maaOppijanYhteystiedoista(
    maat: Seq[Koodistokoodiviite],
    yhteystiedot: ValpasYhteystiedot
  ): Option[Koodistokoodiviite] = {
    val haluttuMaanNimiTrimmattuna: Option[String] = yhteystiedot.maa.map(_.get("fi").trim.toLowerCase)

    val maa: Option[Koodistokoodiviite] = (haluttuMaanNimiTrimmattuna match {
      case Some("") => None // Varmuuden vuoksi hylätään kokonaan tyhjä maan nimi, koska jonkin maan nimi
      // jollakin tuetulla kielellä koodistossa saattaa olla tyhjä
      case Some(haluttuMaanNimiTrimmattuna) => maat.find(maa => maa.nimi match {
        case Some(koodistoMaanNimi) if koodistoMaanNimi.get("fi").toLowerCase == haluttuMaanNimiTrimmattuna => true
        case Some(koodistoMaanNimi) if koodistoMaanNimi.get("sv").toLowerCase == haluttuMaanNimiTrimmattuna => true
        case Some(koodistoMaanNimi) if koodistoMaanNimi.get("en").toLowerCase == haluttuMaanNimiTrimmattuna => true
        case _ => false
      })
      case _ => None
    }).orElse(oletusMaa)
    maa
  }

  private def oletusMaa: Option[Koodistokoodiviite] =
    koodistoViitePalvelu.validate(Koodistokoodiviite("246", "maatjavaltiot2")) // Suomi

  private def kuntaOppijanYhteystiedoista(
    kunnat: Seq[OrganisaatioWithOid],
    valpasYhteystiedot: ValpasYhteystiedot
  ): Option[OrganisaatioWithOid] = {
    // Yritetään päätellä kunta ensimmäisenä listalla olevan yhteystiedon perusteella. Jos oppijalla on aktiivisten
    // hakemusten yhteystietoja, se on ensimmäisenä, muussa tapauksessa ensimmäisenä on ensimmäinen
    // oppijanumerorekisteristä saatu DVV:n yhteystieto.
    // Tätä voisi parantaa mm. käyttämällä postinumerohakua, jos maa on Suomi.
    val postitoimipaikkaTrimmattuna: Option[String] = valpasYhteystiedot.postitoimipaikka.map(_.trim.toLowerCase)

    val kunta: Option[OrganisaatioWithOid] = postitoimipaikkaTrimmattuna match {
      case Some("") => None // Varmuuden vuoksi hylätään kokonaan tyhjä postitoimipaikka, koska jonkin kunnan
      // nimi jollain tuetulla kielellä saattaa olla tyhjä koodistossa
      case Some(postitoimipaikkaTrimmattuna) => {
        kunnat.find(_.kotipaikka.flatMap(_.nimi) match {
          case Some(kotipaikanNimi) if kotipaikanNimi.get("fi").toLowerCase == postitoimipaikkaTrimmattuna => true
          case Some(kotipaikanNimi) if kotipaikanNimi.get("sv").toLowerCase == postitoimipaikkaTrimmattuna => true
          case _ => false
        }
        )
      }
      case _ => None
    }
    kunta
  }

  private def mahdollisetTekijäOrganisaatiot(
    tekijäOrganisaatio: Option[OrganisaatioWithOid],
    oppijanOppilaitokset: Set[ValpasOppilaitos.Oid]
  )(implicit session: ValpasSession): Either[HttpStatus, Set[OrganisaatioWithOid]] = {
    val sallitutRoolit = Set(
      ValpasRooli.OPPILAITOS_HAKEUTUMINEN,
      ValpasRooli.OPPILAITOS_SUORITTAMINEN,
      ValpasRooli.KUNTA
    )

    val oidsToCheck = tekijäOrganisaatio match {
      // Jos organisaatio on jo annettu, ei palauteta toistaiseksi mitään muita vaihtoehtoja,
      // vaikka jollekin yksittäiselle oppijalle voisikin lähettää ilmoituksen muusta oppilaitoksesta käsin.
      case Some(o) => Set(o.oid)
      case None => oppijanOppilaitokset
    }

    val organisaatiot = oidsToCheck
      .filter(oid => sallitutRoolit.exists(rooli => accessResolver.accessToOrg(rooli, oid)))
      .map(organisaatioRepository.getOrganisaatio)

    val kunnat = organisaatioService.omatOrganisaatiotJaKayttooikeusroolit
      .filter(_.kayttooikeusrooli == ValpasRooli.KUNTA)
      .flatMap(_.organisaatioHierarkia.toKunta)
      .filter(kunta => accessResolver.accessToOrg(ValpasRooli.KUNTA, kunta.oid))

    if (organisaatiot.contains(None)) {
      Left(ValpasErrorCategory.internalError("Kaikkia oppijan organisaatioita ei löydy organisaatiopalvelusta"))
    } else {
      Right(organisaatiot.flatten ++ kunnat)
    }
  }

  private def yhteydenottokieli(äidinkieli: Option[String]): Option[Koodistokoodiviite] = {
    val koodistoviite = äidinkieli match {
      case Some("sv") => Koodistokoodiviite("SV", "kieli")
      case _ => Koodistokoodiviite("FI", "kieli")
    }
    koodistoViitePalvelu.validate(koodistoviite)
  }
}
