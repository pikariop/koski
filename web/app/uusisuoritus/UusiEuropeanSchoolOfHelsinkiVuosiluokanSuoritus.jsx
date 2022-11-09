import React from 'baret'
import * as L from 'partial.lenses'
import Atom from 'bacon.atom'
import Text from '../i18n/Text'
import ModalDialog from '../editor/ModalDialog'
import {
  modelData,
  modelLookup,
  modelSet,
  modelSetData
} from '../editor/EditorModel'
import {
  copySuorituskieli,
  copyToimipiste,
  newSuoritusProto
} from '../suoritus/Suoritus'
import { suoritusPrototypeKey } from '../esh/europeanschoolofhelsinkiSuoritus'
import UusiEuropeanSchoolOfHelsinkiSuoritus from '../uusioppija/UusiEuropeanSchoolOfHelsinkiSuoritus'

export const UusiEuropeanSchoolOfHelsinkiVuosiluokanSuoritus = ({
  opiskeluoikeus,
  resultCallback
}) => {
  const suoritusAtom = Atom()
  const oppilaitosAtom = Atom(modelData(opiskeluoikeus, 'oppilaitos'))
  const lisääSuoritus = () => {
    const suoritus = suoritusAtom.get()
    let proto = newSuoritusProto(
      opiskeluoikeus,
      suoritusPrototypeKey(suoritus.tyyppi.koodiarvo)
    )
    proto = withKoulutusmoduulinTunniste(
      proto,
      suoritus.koulutusmoduuli.tunniste
    )
    proto = copyToimipiste(modelLookup(opiskeluoikeus, 'suoritukset.0'), proto)
    proto = copySuorituskieli(
      modelLookup(opiskeluoikeus, 'suoritukset.0'),
      proto
    )
    resultCallback(proto)
  }

  return (
    <div>
      <ModalDialog
        className="lisaa-suoritus-modal"
        onDismiss={resultCallback}
        onSubmit={lisääSuoritus}
        okTextKey="Lisää"
      >
        <h2>
          <Text name="Suorituksen lisäys" />
        </h2>
        <UusiEuropeanSchoolOfHelsinkiSuoritus
          suoritusAtom={suoritusAtom}
          oppilaitosAtom={oppilaitosAtom}
        />
      </ModalDialog>
    </div>
  )
}

UusiEuropeanSchoolOfHelsinkiVuosiluokanSuoritus.canAddSuoritus = (
  opiskeluoikeus
) =>
  modelData(opiskeluoikeus, 'tyyppi.koodiarvo') === 'europeanschoolofhelsinki'

UusiEuropeanSchoolOfHelsinkiVuosiluokanSuoritus.addSuoritusTitle = () => (
  <Text name="lisää vuosiluokan suoritus" />
)

const withKoulutusmoduulinTunniste = (suoritusProto, tunniste) => {
  let kmt = modelLookup(suoritusProto, 'koulutusmoduuli.tunniste')
  kmt = modelSetData(kmt, tunniste)
  kmt = L.set(['title'], tunniste.nimi.fi, kmt)
  return modelSet(suoritusProto, kmt, 'koulutusmoduuli.tunniste')
}