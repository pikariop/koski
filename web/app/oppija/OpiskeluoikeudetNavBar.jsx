import React from 'baret'
import Atom from 'bacon.atom'
import { modelData, modelTitle, modelItems } from '../editor/EditorModel'
import Link from '../components/Link'
import { currentLocation, navigateTo } from '../util/location.js'
import { yearFromIsoDateString } from '../date/date'
import { UusiOpiskeluoikeusPopup } from './UusiOpiskeluoikeusPopup'
import { postNewOppija } from '../uusioppija/UusiOppija'
import { reloadOppija } from '../virkailija/VirkailijaOppijaView'
import { userP } from '../util/user'
import Text from '../i18n/Text'

export default ({ oppijaOid, opiskeluoikeusTyypit, selectedIndex }) => {
  const addingAtom = Atom(false)
  const toggleAdd = () => addingAtom.modify((x) => !x)
  const addOpiskeluoikeus = (opiskeluoikeus) => {
    if (!opiskeluoikeus) {
      addingAtom.set(false)
    } else {
      const oppija = {
        henkilö: { oid: oppijaOid },
        opiskeluoikeudet: [opiskeluoikeus]
      }
      const tyyppi = opiskeluoikeus.tyyppi.koodiarvo
      postNewOppija(oppija)
        .doError(() => addingAtom.set(false))
        .onValue(() => {
          reloadOppija()
          navigateTo('?opiskeluoikeudenTyyppi=' + tyyppi)
        })
    }
  }

  const canAddOpiskeluoikeusP = userP.map((u) => !!u.hasWriteAccess)
  return (
    <ul className="opiskeluoikeustyypit-nav">
      {opiskeluoikeusTyypit.map((opiskeluoikeudenTyyppi, tyyppiIndex) => {
        const selected = tyyppiIndex == selectedIndex
        const koodiarvo = modelData(opiskeluoikeudenTyyppi).tyyppi.koodiarvo
        const className = selected ? koodiarvo + ' selected' : koodiarvo
        const content = (
          <div>
            <div className="opiskeluoikeustyyppi">
              {modelTitle(opiskeluoikeudenTyyppi, 'tyyppi')}
            </div>
            <ul className="oppilaitokset">
              {modelItems(opiskeluoikeudenTyyppi, 'opiskeluoikeudet').map(
                (oppilaitoksenOpiskeluoikeudet, oppilaitosIndex) => (
                  <li key={oppilaitosIndex}>
                    <span className="oppilaitos">
                      {modelTitle(oppilaitoksenOpiskeluoikeudet, 'oppilaitos')}
                    </span>
                    <ul className="opiskeluoikeudet">
                      {modelItems(
                        oppilaitoksenOpiskeluoikeudet,
                        'opiskeluoikeudet'
                      ).map((opiskeluoikeus, opiskeluoikeusIndex) => (
                        <li
                          className="opiskeluoikeus"
                          key={opiskeluoikeusIndex}
                        >
                          <span className="koulutus">
                            {modelTitle(opiskeluoikeus, 'suoritukset.0.tyyppi')}
                          </span>{' '}
                          {modelData(opiskeluoikeus, 'alkamispäivä') ? (
                            <span>
                              <span className="alku pvm">
                                {yearFromIsoDateString(
                                  modelTitle(opiskeluoikeus, 'alkamispäivä')
                                )}
                              </span>
                              {'—'}
                              <span className="loppu pvm">
                                {yearFromIsoDateString(
                                  modelTitle(opiskeluoikeus, 'päättymispäivä')
                                )}
                                {', '}
                              </span>
                            </span>
                          ) : null}
                          <span className="tila">
                            {modelTitle(
                              opiskeluoikeus,
                              'tila.opiskeluoikeusjaksot.-1.tila'
                            )}
                          </span>
                        </li>
                      ))}
                    </ul>
                  </li>
                )
              )}
            </ul>
          </div>
        )
        return (
          <li className={className} key={tyyppiIndex}>
            {selected ? (
              content
            ) : (
              <Link href={'?opiskeluoikeudenTyyppi=' + koodiarvo}>
                {content}
              </Link>
            )}
          </li>
        )
      })}
      {canAddOpiskeluoikeusP.map(
        (canAdd) =>
          canAdd && (
            <li
              key="new"
              className={
                'add-opiskeluoikeus' +
                (currentLocation().params.edit ? ' disabled' : '')
              }
            >
              <span className="plus" onClick={toggleAdd}>
                {''}
              </span>
              <a onClick={toggleAdd}>
                <Text name="Lisää opiskeluoikeus" />
              </a>
              {addingAtom.map(
                (adding) =>
                  adding && (
                    <UusiOpiskeluoikeusPopup
                      resultCallback={addOpiskeluoikeus}
                    />
                  )
              )}
            </li>
          )
      )}
    </ul>
  )
}
