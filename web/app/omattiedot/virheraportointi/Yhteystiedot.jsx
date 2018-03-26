import React from 'baret'
import Bacon from 'baconjs'
import {OppilaitoksenYhteystieto} from './OppilaitoksenYhteystieto'
import {modelData, modelTitle} from '../../editor/EditorModel'
import {ISO2FinnishDate} from '../../date/date'
import {CopyableText} from '../../components/CopyableText'
import Text from '../../i18n/Text'
import {VirheraporttiMessage} from './emailMessage'

const Yhteystieto = ({henkilö, yhteystieto}) => {
  const nimi = `${modelData(henkilö, 'etunimet')} ${modelData(henkilö, 'sukunimi')}`
  const syntymäaika = ISO2FinnishDate(modelTitle(henkilö, 'syntymäaika'))
  const oppijaOid = modelData(henkilö, 'oid')

  const messageDetails = VirheraporttiMessage.details(nimi, syntymäaika, oppijaOid)
  const fullMessage = [
    VirheraporttiMessage.placeholder(),
    VirheraporttiMessage.spacer(),
    VirheraporttiMessage.brief(),
    messageDetails
  ].join('\n\n')

  return (
    <div>
      <OppilaitoksenYhteystieto yhteystieto={yhteystieto} message={fullMessage}/>
      <CopyableText heading={'Muista mainita sähköpostissa seuraavat tiedot:'} message={messageDetails}/>
    </div>
  )
}

const EiYhteystietoa = () => <Text name={'Oppilaitokselle ei löytynyt yhteystietoja.'}/>

const MuuVirhe = () => <Text name={'httpStatus.500'}/>

const wrapAsSection = Component => Component ? <div><hr/>{Component}</div> : Component

export const Yhteystiedot = ({henkilö, yhteystietoP, isLoadingP}) => {
  const values = yhteystietoP
    .skipErrors()
    .map(yhteystieto => !yhteystieto ? null
      : !yhteystieto.email ? <EiYhteystietoa/>
        : <Yhteystieto henkilö={henkilö} yhteystieto={yhteystieto}/>)

  const errors = yhteystietoP
    .errors()
    .mapError()
    .map(() => <MuuVirhe/>)

  const loading = isLoadingP
    .filter(v => !!v)
    .map(<div className='yhteystieto loading'><Text name='Haetaan'/></div>)

  return <div>{Bacon.mergeAll(values, errors, loading).map(wrapAsSection)}</div>
}
