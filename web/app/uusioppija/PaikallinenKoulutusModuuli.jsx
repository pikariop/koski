import React from 'baret'
import Text from '../i18n/Text'

const textInput = (atom) => <input type='text' value={atom.or('')} onChange={ e => atom.set(e.target.value)} />
const textArea = (atom) => <textarea type='text' value={atom.or('')} onChange={ e => atom.set(e.target.value)} />

export default ({nimi, koodi, kuvaus}) => (
  <div className='koulutusmoduuli'>

    <label className='nimi'>
      <Text name='Nimi' />
      {textInput(nimi)}
    </label>

    <label className='koodiarvo'>
      <Text name='Koodiarvo' />
      {textInput(koodi)}
    </label>

    <label className='kuvaus'>
      <Text name='Kuvaus' />
      {textArea(kuvaus)}
    </label>

  </div>
)
