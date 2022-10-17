import Http from '../util/http'
import Bacon from 'baconjs'

const valtionosuusrahoitteinen = '1'

export const defaultRahoitusmuotoP = Bacon.constant(valtionosuusrahoitteinen)
  .flatMap((vos) =>
    Http.cachedGet(`/koski/api/editor/koodit/opintojenrahoitus/${vos}`)
  )
  .map((rahoitukset) =>
    rahoitukset.find(
      (rahoitus) => rahoitus.data.koodiarvo === valtionosuusrahoitteinen
    )
  )
  .toProperty()

export const autoFillRahoitusmuoto = ({
  vaatiiRahoituksen,
  rahoitusValittu,
  setDefaultRahoitus,
  setRahoitusNone
}) => {
  if (vaatiiRahoituksen && !rahoitusValittu) {
    setDefaultRahoitus()
  } else if (!vaatiiRahoituksen && rahoitusValittu) {
    setRahoitusNone()
  }
}

export const opiskeluoikeudenTilaVaatiiRahoitusmuodon = (
  opiskeluoikeudenTyyppi,
  tila,
  suoritus
) => {
  // TODO: TOR-1685 Eurooppalainen koulu
  if (suoritus && suoritus.koodiarvo === 'vstjotpakoulutus') {
    return ['lasna', 'hyvaksytystisuoritettu'].includes(tila)
  }
  if (
    [
      'aikuistenperusopetus',
      'lukiokoulutus',
      'luva',
      'ibtutkinto',
      'internationalschool',
      'diatutkinto',
      'tuva'
    ].includes(opiskeluoikeudenTyyppi)
  ) {
    return ['lasna', 'valmistunut'].includes(tila)
  }
  if (opiskeluoikeudenTyyppi === 'ammatillinenkoulutus') {
    return ['lasna', 'valmistunut', 'loma'].includes(tila)
  }
  return false
}
