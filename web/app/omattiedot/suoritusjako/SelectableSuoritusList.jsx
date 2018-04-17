import React from 'baret'
import Bacon from 'baconjs'
import R from 'ramda'
import {modelItems, modelLookup, modelTitle} from '../../editor/EditorModel'
import SuoritusIdentifier from './SuoritusIdentifier'
import {suoritusjakoSuoritusTitle} from './suoritusjako'
import {suorituksenTyyppi} from '../../suoritus/Suoritus'
import Text from '../../i18n/Text'

const isKorkeakoulunOpintojakso = suoritus => suorituksenTyyppi(suoritus) === 'korkeakoulunopintojakso'

const groupSuoritukset = suoritukset => isKorkeakoulunOpintojakso(suoritukset[0]) ? [suoritukset[0]] : suoritukset
const withIdentifiers = opiskeluoikeus => suoritukset => suoritukset.map(suoritus => ({suoritus, id: SuoritusIdentifier(opiskeluoikeus, suoritus)}))
const withoutDuplicates = suorituksetWithIdentifiers => R.uniqBy(sWithId => sWithId.id, suorituksetWithIdentifiers)

export const SelectableSuoritusList = ({opiskeluoikeudet, selectedSuoritusIds}) => {
  const toggleSelection = id => event =>
    selectedSuoritusIds.modify(ids => event.target.checked ? R.append(id, ids) : R.without([id], ids))

  return (
    <ul className='create-suoritusjako__list'>
      {
        opiskeluoikeudet.map(oppilaitoksittain => {
          const oppilaitos = modelLookup(oppilaitoksittain, 'oppilaitos')
          const groupTitle = modelTitle(oppilaitos)
          const oppilaitoksenOpiskeluoikeudet = Bacon.constant(modelItems(oppilaitoksittain, 'opiskeluoikeudet'))

          return [
            <li className='oppilaitos-group-header' key={groupTitle}>
              {groupTitle}
            </li>,
            Bacon.combineWith(oppilaitoksenOpiskeluoikeudet, selectedSuoritusIds, (opiskeluoikeudeModels, selectedIds) =>
              opiskeluoikeudeModels.map(oo => {
                const identifiersWithTitles = suorituksetWithIdentifiers => suorituksetWithIdentifiers.map(
                  ({suoritus, id}) => ({
                    id,
                    Title: () => suorituksenTyyppi(suoritus) === 'korkeakoulunopintojakso'
                      ? <span>{päätasonSuoritukset.length} <Text name='opintojaksoa'/></span>
                      : <span>{suoritusjakoSuoritusTitle(suoritus)}</span>
                  }))

                const päätasonSuoritukset = modelItems(oo, 'suoritukset')
                const näytettävätSuoritukset = groupSuoritukset(päätasonSuoritukset)
                const options = R.compose(identifiersWithTitles, withoutDuplicates, withIdentifiers(oo))(näytettävätSuoritukset)

                return options.map(({id, Title}) => (
                    <li key={id}>
                      <input
                        type='checkbox'
                        id={id}
                        name='suoritusjako'
                        checked={R.contains(id, selectedIds)}
                        onChange={toggleSelection(id)}
                      />
                      <label htmlFor={id}><Title/></label>
                    </li>
                  )
                )
              })
            )
          ]
        })
      }
    </ul>
  )
}
