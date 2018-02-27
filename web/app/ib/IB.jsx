import React from 'react'
import R from 'ramda'
import {LukionOppiaineEditor} from '../lukio/LukionOppiaineEditor'
import {LukionOppiaineetTableHead} from '../lukio/fragments/LukionOppiaineetTableHead'
import {modelData, modelItems, modelLookup} from '../editor/EditorModel'
import {FootnoteDescriptions} from '../components/footnote'
import {UusiIBOppiaineDropdown} from './UusiIBOppiaineDropdown'

const ArvosanaFootnote = {title: 'Ennustettu arvosana', hint: '*'}

export const IBTutkinnonOppiaineetEditor = ({suorituksetModel}) => {
  const {suoritus: päätasonSuoritusModel} = suorituksetModel.context
  const oppiaineet = modelItems(suorituksetModel)

  const aineryhmittäin = R.groupBy(
    oppiaine => modelData(oppiaine, 'koulutusmoduuli.ryhmä').koodiarvo,
    oppiaineet
  )

  const footnotes = R.any(s => modelData(s, 'arviointi.-1.predicted'), oppiaineet)
    ? [ArvosanaFootnote]
    : []

  return (
    <div>
      <table className='suoritukset oppiaineet'>
        <LukionOppiaineetTableHead />
        <tbody>
        {
          Object.values(aineryhmittäin).map(aineet => [
            <tr className='aineryhmä'>
              <th colSpan='4'>{modelLookup(aineet[0], 'koulutusmoduuli.ryhmä').value.title}</th>
            </tr>,
            aineet.map((oppiaine, oppiaineIndex) => {
              const footnote = modelData(oppiaine, 'arviointi.-1.predicted') && ArvosanaFootnote
              return <LukionOppiaineEditor key={oppiaineIndex} oppiaine={oppiaine} footnote={footnote} />
            }),
            <tr className='uusi-oppiaine'>
              <td colSpan='4'>
                <UusiIBOppiaineDropdown
                  model={päätasonSuoritusModel}
                  aineryhmä={modelData(aineet[0], 'koulutusmoduuli.ryhmä')}
                />
              </td>
            </tr>
          ])
        }
        </tbody>
      </table>
      {!R.isEmpty(footnotes) && <FootnoteDescriptions data={footnotes}/>}
    </div>
  )
}
