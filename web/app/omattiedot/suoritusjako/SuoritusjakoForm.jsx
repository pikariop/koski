import React from 'react'
import Http from '../../util/http'
import {flatMapArray} from '../../util/util'
import Text from '../../i18n/Text'
import {modelItems, modelLookup, modelTitle} from '../../editor/EditorModel'
import {suorituksenTyyppi, suoritusTitle} from '../../suoritus/Suoritus'
import SuoritusIdentifier from './SuoritusIdentifier'

const doShare = suoritusIds => Http.put('/koski/api/suoritusjako', [...suoritusIds])

const Ingressi = () => (
  <div className='suoritusjako-form__caption'>
    <Text name={
      'Luomalla jakolinkin voit näyttää suoritustietosi haluamillesi henkilöille (esimerkiksi työtä tai opiskelupaikkaa hakiessasi). ' +
      'Henkilötietoja tai muita arkaluontoisia tietoja ei jaeta.'}
    />
  </div>
)

const SelectableSuoritusList = ({opiskeluoikeudet, selectedSuoritusIds}) => (
  <ul className='create-suoritusjako__list'>
    {
      opiskeluoikeudet.map(oppilaitoksenOpiskeluoikeudet => {
        const oppilaitos = modelLookup(oppilaitoksenOpiskeluoikeudet, 'oppilaitos')
        const groupTitle = modelTitle(oppilaitos)
        const suoritukset = flatMapArray(
          modelItems(oppilaitoksenOpiskeluoikeudet, 'opiskeluoikeudet'),
          oo => modelItems(oo, 'suoritukset')
        )

        return [
          <li className='oppilaitos-group-header' key={groupTitle}>
            {groupTitle}
          </li>,
          suoritukset.map(s => {
            const id = SuoritusIdentifier(oppilaitos, s)
            const title = suorituksenTyyppi(s) === 'perusopetuksenoppimaara'
              ? <Text name="Päättötodistus"/>
              : suoritusTitle(s)

            return (
              <li key={id}>
                <input
                  type='checkbox'
                  id={id}
                  name='suoritusjako'
                  value='newsletter'
                  onChange={event => event.target.checked ? selectedSuoritusIds.add(id) : selectedSuoritusIds.delete(id)}
                />
                <label htmlFor={id}>{title}</label>
              </li>
            )
          })
        ]
      })
    }
  </ul>
)

const CreateNewButton = ({selectedSuoritusIds}) => (
  <div className='create-suoritusjako__button'>
    <button onClick={() => doShare(selectedSuoritusIds)}>
      <Text name='Jaa valitsemasi opinnot'/>
    </button>
  </div>
)

const CreateSuoritusjako = ({opiskeluoikeudet}) => {
  const selectedSuoritusIds = new Set()

  return (
    <div className='suoritusjako-form__create-suoritusjako'>
      <h2><Text name='Valitse jaettavat suoritustiedot'/></h2>
      <div className='create-suoritusjako'>
        <SelectableSuoritusList opiskeluoikeudet={opiskeluoikeudet} selectedSuoritusIds={selectedSuoritusIds}/>
        <CreateNewButton selectedSuoritusIds={selectedSuoritusIds}/>
      </div>
    </div>
  )
}

export const SuoritusjakoForm = ({opiskeluoikeudet}) => {
  return (
    <section className='suoritusjako-form'>
      <Ingressi/>
      <CreateSuoritusjako opiskeluoikeudet={opiskeluoikeudet}/>
    </section>
  )
}
