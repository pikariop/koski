package fi.oph.tor.opintooikeus

import fi.oph.tor.oppija.{Exists, CreationResult, Oppija}
import fi.oph.tor.user.UserContext

trait OpintoOikeusRepository {
  def filterOppijat(oppijat: List[Oppija])(implicit userContext: UserContext): List[Oppija]
  def findByOppijaOid(oid: String)(implicit userContext: UserContext): List[OpintoOikeus]
  def create(opintoOikeus: OpintoOikeus): CreationResult
  def resetFixtures {}

  def findOrCreate(opintoOikeus: OpintoOikeus)(implicit userContext: UserContext): CreationResult = {
    val opintoOikeudet: List[OpintoOikeus] = findByOppijaOid(opintoOikeus.oppijaOid)
    opintoOikeudet.find(_ == opintoOikeus) match {
      case _ => create(opintoOikeus)
    }
  }
}

