import React from 'react'
import * as R from 'ramda'

import { t } from '../../i18n/i18n'
import { modelData, modelLookup, modelTitle } from '../../editor/EditorModel'
import { FootnoteHint } from '../../components/footnote'
import {
  isKieliaine,
  isLukio2019Oppiaine,
  isLukionMatematiikka,
  isPaikallinen
} from '../../suoritus/Koulutusmoduuli'
import { Editor } from '../../editor/Editor'
import { ArvosanaEditor } from '../../suoritus/ArvosanaEditor'
import { PropertiesEditor } from '../../editor/PropertiesEditor'
import { sortLanguages } from '../../util/sorting'

const Nimi = ({ oppiaine }) => {
  const { edit } = oppiaine.context
  const koulutusmoduuli = modelLookup(oppiaine, 'koulutusmoduuli')
  const nimi = t(modelData(oppiaine, 'koulutusmoduuli.tunniste.nimi'))
  const nimiJaKieli = modelTitle(oppiaine, 'koulutusmoduuli')
  const hasOptions =
    isKieliaine(koulutusmoduuli) || isLukionMatematiikka(koulutusmoduuli)
  const valinnainenLukio2019 =
    isLukio2019Oppiaine(koulutusmoduuli) &&
    !modelData(koulutusmoduuli, 'pakollinen')
      ? `, ${t('valinnainen')}`
      : ''

  return [
    edit && isPaikallinen(koulutusmoduuli) ? (
      <span key="nimi" className="koodi-ja-nimi">
        <span className="koodi">
          <Editor
            model={koulutusmoduuli}
            path="tunniste.koodiarvo"
            placeholder={t('Koodi')}
          />
        </span>
        <span className="nimi">
          <Editor
            model={koulutusmoduuli}
            path="tunniste.nimi"
            placeholder={t('Oppiaineen nimi')}
          />
        </span>
      </span>
    ) : (
      <span key="nimi" className="nimi">
        {edit && hasOptions ? `${nimi}, ` : nimiJaKieli + valinnainenLukio2019}
      </span>
    ),
    isPaikallinen(koulutusmoduuli) && (
      <FootnoteHint key="footnote" title={'Paikallinen oppiaine'} />
    )
  ]
}

const KoulutusmoduuliPropertiesEditor = ({
  oppiaine,
  additionalEditableProperties
}) => {
  if (!oppiaine.context.edit) return null

  const koulutusmoduuli = modelLookup(oppiaine, 'koulutusmoduuli')

  return (
    <span className="properties">
      {isKieliaine(koulutusmoduuli) && (
        <Editor
          model={koulutusmoduuli}
          path="kieli"
          inline={true}
          sortBy={sortLanguages}
        />
      )}
      {isLukionMatematiikka(koulutusmoduuli) && (
        <Editor model={koulutusmoduuli} path="oppimäärä" inline={true} />
      )}
      {isPaikallinen(koulutusmoduuli) && (
        <PropertiesEditor
          model={koulutusmoduuli}
          propertyFilter={(p) => p.key === 'kuvaus'}
        />
      )}
      {additionalEditableProperties && (
        <PropertiesEditor
          model={koulutusmoduuli}
          propertyFilter={(p) => additionalEditableProperties.includes(p.key)}
        />
      )}
    </span>
  )
}

const Arviointi = ({ oppiaine, suoritetutKurssit, footnote, predicted }) => {
  const { edit, suoritus } = oppiaine.context
  const ishDiploma =
    edit &&
    modelData(suoritus, 'koulutusmoduuli.diplomaType.koodiarvo') === 'ish'

  const filterProps = (p) => {
    const predictedInISH = ishDiploma && p.key === 'predicted'
    return !(p.key === 'arvosana' || p.model.optional || predictedInISH)
  }

  // IB-opintojen predicted- ja päättöarvosanat tallennettiin tietomallissa aiemmin arviointi-kenttään,
  // mutta nykyään ne erotellaan kahteen erilliseen kenttään. Tuetaan datan taaksepäinyhteensopivuutta
  // etsimällä ne tarvittaessa myös vanhasta kentästä. Jos predicted ei ole määritelty, käytetään vanhaa
  // logiikkaa.
  const arviointiField = predicted === true ? 'predictedArviointi' : 'arviointi'
  const arviointi =
    predicted === undefined
      ? modelData(oppiaine, arviointiField)
      : predicted
      ? modelData(oppiaine, 'predictedArviointi') ||
        (modelData(oppiaine, 'arviointi') || []).filter(
          (a) => a.arvosana.predicted
        )
      : (modelData(oppiaine, 'arviointi') || []).filter(
          (a) => !a.arvosana.predicted
        )

  return (
    <div>
      <div className="annettuArvosana">
        {edit || arviointi ? (
          <span className="value">
            <ArvosanaEditor model={oppiaine} arviointiField={arviointiField} />
          </span>
        ) : (
          '-'
        )}
        {edit && (
          <PropertiesEditor
            model={modelLookup(oppiaine, `${arviointiField}.-1`)}
            propertyFilter={filterProps}
            key={'properties'}
          />
        )}
        {!edit && arviointi && footnote && (
          <FootnoteHint title={footnote.title} hint={footnote.hint} />
        )}
      </div>
    </div>
  )
}

export { Nimi, KoulutusmoduuliPropertiesEditor, Arviointi }
