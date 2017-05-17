import React from 'baret'
import Bacon from 'baconjs'
import Atom from 'bacon.atom'
import R from 'ramda'
import * as L from 'partial.lenses'
import Http from '../http'
import {UusiPerusopetuksenOppiaineDropdown} from '../editor/UusiPerusopetuksenOppiaineDropdown.jsx'
import {accumulateModelState, modelLookup, modelData, modelSet} from '../editor/EditorModel'
import {editorMapping} from '../editor/Editors.jsx'
import {Editor} from '../editor/Editor.jsx'
import {PropertyEditor} from '../editor/PropertyEditor.jsx'
import KoodistoDropdown from './KoodistoDropdown.jsx'
import {koodistoValues, koodiarvoMatch} from './koodisto'
import {PerusteDropdown} from '../editor/PerusteDropdown.jsx'
import {modelValid} from '../editor/EditorModel';

export default ({suoritusAtom, oppilaitosAtom}) => {
  const oppimääräAtom = Atom()
  const oppiaineenSuoritusAtom = Atom()
  const opetussuunnitelmaAtom = Atom()
  const perusteAtom = Atom()
  const oppimäärätP = koodistoValues('suorituksentyyppi/perusopetuksenoppimaara,perusopetuksenoppiaineenoppimaara')
  oppimäärätP.onValue(oppimäärät => oppimääräAtom.set(oppimäärät.find(koodiarvoMatch('perusopetuksenoppimaara'))))

  const opetussuunnitelmatP = koodistoValues('perusopetuksenoppimaara')
  opetussuunnitelmatP.onValue(tilat => opetussuunnitelmaAtom.set(tilat.find(koodiarvoMatch('perusopetus'))))

  const suoritusPrototypeP = oppimääräAtom.map('.koodiarvo').flatMap(oppimäärä => {
    if (oppimäärä == 'perusopetuksenoppiaineenoppimaara') {
      return Http.cachedGet('/koski/api/editor/prototype/fi.oph.koski.schema.PerusopetuksenOppiaineenOppimääränSuoritus')
    }
  }).toProperty()

  const makeSuoritus = (oppilaitos, oppimäärä, opetussuunnitelma, peruste, oppiaineenSuoritus) => {
    if (oppilaitos && opetussuunnitelma && peruste && koodiarvoMatch('perusopetuksenoppimaara')(oppimäärä)) {
      return makePerusopetuksenOppimääränSuoritus(oppilaitos, opetussuunnitelma, peruste)
    } else if (oppilaitos && koodiarvoMatch('perusopetuksenoppiaineenoppimaara')(oppimäärä) && oppiaineenSuoritus) {
      var suoritusTapaJaToimipiste = {
        toimipiste: oppilaitos
      }
      return R.merge(oppiaineenSuoritus, suoritusTapaJaToimipiste)
    }
  }

  Bacon.combineWith(oppilaitosAtom, oppimääräAtom, opetussuunnitelmaAtom, perusteAtom, oppiaineenSuoritusAtom, makeSuoritus)
    .onValue(suoritus => suoritusAtom.set(suoritus))


  return (<span>
    <Oppimäärä oppimääräAtom={oppimääräAtom} oppimäärätP={oppimäärätP}/>
    {
      oppimääräAtom.map( oppimäärä => koodiarvoMatch('perusopetuksenoppimaara')(oppimäärä)
        ? <Opetussuunnitelma opetussuunnitelmaAtom={opetussuunnitelmaAtom} opetussuunnitelmatP={opetussuunnitelmatP} perusteAtom={perusteAtom}/>
        : <Oppiaine suoritusPrototypeP={suoritusPrototypeP} oppiaineenSuoritusAtom={oppiaineenSuoritusAtom} perusteAtom={perusteAtom}/>
      )
    }
  </span>)
}

const Oppimäärä = ({oppimääräAtom, oppimäärätP}) => {
  return (<div>
    <KoodistoDropdown
      className="oppimaara"
      title="Oppimäärä"
      optionsP = { oppimäärätP }
      atom = {oppimääräAtom}
    />
  </div> )
}

let makePerusopetuksenOppimääränSuoritus = (oppilaitos, opetussuunnitelma, peruste) => {
  return {
    koulutusmoduuli: {
      tunniste: {
        koodiarvo: '201101',
        koodistoUri: 'koulutus'
      },
      perusteenDiaarinumero: peruste
    },
    toimipiste: oppilaitos,
    tila: { koodistoUri: 'suorituksentila', koodiarvo: 'KESKEN'},
    oppimäärä: opetussuunnitelma,
    suoritustapa: { koodistoUri: 'perusopetuksensuoritustapa', koodiarvo: 'koulutus'},
    tyyppi: { koodistoUri: 'suorituksentyyppi', koodiarvo: 'perusopetuksenoppimaara'}
  }
}


const Opetussuunnitelma = ({opetussuunnitelmaAtom, perusteAtom, opetussuunnitelmatP}) => {
  let suoritusP = opetussuunnitelmaAtom.map(opetussuunnitelma => makePerusopetuksenOppimääränSuoritus(null, opetussuunnitelma, null))
  return (<div>
    <KoodistoDropdown
      className="opetussuunnitelma"
      title="Opetussuunnitelma"
      optionsP = { opetussuunnitelmatP }
      atom = { opetussuunnitelmaAtom }
    />
    <Peruste {...{suoritusP, perusteAtom}} />
  </div>
  )
}

const Peruste = ({suoritusP, perusteAtom}) => <label className="peruste">Peruste<PerusteDropdown {...{suoritusP, perusteAtom}}/></label>

const Oppiaine = ({suoritusPrototypeP, oppiaineenSuoritusAtom, perusteAtom}) => { // suoritusPrototypeP = prototyyppi oppiaineen oppimäärän suoritukselle
  return (<span>
    {
      suoritusPrototypeP.map(oppiaineenSuoritus => {
        let oppiainePrototypeAtom = Atom(undefined) // Valittu oppiaine/koulutusmoduuliprototyyppi
        if (!oppiaineenSuoritus) return null
        oppiaineenSuoritus = Editor.setupContext(oppiaineenSuoritus, {edit:true, editorMapping})

        let suoritusModelP = oppiainePrototypeAtom.flatMapLatest(oppiainePrototype => {
          return oppiainePrototype && accumulateModelState(modelSet(oppiaineenSuoritus, oppiainePrototype, 'koulutusmoduuli'))
        }).toProperty()

        let suoritusDataP = suoritusModelP.map(model => model && modelValid(model) ? modelData(model) : null)

        let suoritusP = Bacon.combineWith(suoritusDataP, perusteAtom, (suoritus, diaarinumero) => {
          if (suoritus && diaarinumero) return L.set(L.compose('koulutusmoduuli', 'perusteenDiaarinumero'), diaarinumero, suoritus)
        })

        suoritusP.onValue(suoritus => oppiaineenSuoritusAtom.set(suoritus))

        return (<span>
          <Peruste suoritusP={Bacon.constant(modelData(oppiaineenSuoritus))} perusteAtom={perusteAtom} />
          <label className="oppiaine">Oppiaine <UusiPerusopetuksenOppiaineDropdown oppiaineenSuoritus={oppiaineenSuoritus} selected={oppiainePrototypeAtom} resultCallback={s => oppiainePrototypeAtom.set(s)} pakollinen={true} enableFilter={false}/></label>
          { suoritusModelP.map(model =>
            model && <label><PropertyEditor model={modelLookup(model, 'koulutusmoduuli')} propertyName="kieli"/></label> )
          }
        </span>)
      })
    }
  </span>)
}