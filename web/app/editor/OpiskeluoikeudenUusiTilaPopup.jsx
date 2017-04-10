import React from 'baret'
import Bacon from 'baconjs'
import {contextualizeSubModel, modelItems, accumulateModelState, modelLookupRequired} from './EditorModel'
import {EnumEditor} from './EnumEditor.jsx'
import {DateEditor} from './DateEditor.jsx'
import ModalDialog from './ModalDialog.jsx'

export const OpiskeluoikeudenUusiTilaPopup = ({edellisenTilanAlkupäivä, suorituksiaKesken, tilaListModel, resultCallback}) => {
  let submitBus = Bacon.Bus()
  let initialModel = contextualizeSubModel(tilaListModel.arrayPrototype, tilaListModel, modelItems(tilaListModel).length)

  let { modelP, errorP } = accumulateModelState(initialModel)

  let isAllowedDate = d => edellisenTilanAlkupäivä ? d >= edellisenTilanAlkupäivä : true

  let alkuPäiväModel = modelP.map(m => modelLookupRequired(m, 'alku'))
  let tilaModel = modelP.map(m => modelLookupRequired(m, 'tila'))
  let tilaSelectedP = tilaModel.changes().map(true).toProperty(false)
  let validP = tilaSelectedP.and(errorP.not())

  modelP.sampledBy(submitBus.filter(validP)).onValue(resultCallback)


  return (<ModalDialog className="lisaa-opiskeluoikeusjakso-modal" onDismiss={resultCallback} onSubmit={() => submitBus.push()}>
    <h2>Opiskeluoikeuden tilan lisäys</h2>
    <div className="property alku">
      <label>Päivämäärä:</label>
      <DateEditor baret-lift model={alkuPäiväModel} isAllowedDate={isAllowedDate}/>
    </div>
    <div className="property tila">
      <label>Tila:</label>
      <EnumEditor baret-lift asRadiogroup={true} model={tilaModel} disabledValue={suorituksiaKesken && 'valmistunut'} />
    </div>
    <button disabled={validP.not()} className="opiskeluoikeuden-tila button" onClick={() => submitBus.push()}>Lisää</button>
  </ModalDialog>)
}