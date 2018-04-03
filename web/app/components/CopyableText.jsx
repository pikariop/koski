import React from 'baret'
import Bacon from 'baconjs'
import Atom from 'bacon.atom'
import Text from '../i18n/Text'

const CopyState = {
  PENDING: 1,
  SUCCESS: 2,
  ERROR: 3
}

const ConfirmationDuration = 2000

const copyToClipboard = (message, copyState) => () => {
  const el = document.createElement('textarea')

  try {
    el.value = message
    el.setAttribute('readonly', '')
    document.body.appendChild(el)
    el.select()
    el.setSelectionRange(0, el.value.length)
    document.execCommand('copy')
    copyState.set(CopyState.SUCCESS)
    Bacon.later(ConfirmationDuration, CopyState.PENDING).onValue(v => copyState.set(v))
  } catch(e) {
    copyState.set(CopyState.ERROR)
  } finally {
    document.body.contains(el) && document.body.removeChild(el)
  }
}

export const CopyableText = ({heading, message}) => {
  const copyState = Atom(CopyState.PENDING)

  const buttonState = copyState.map(state => {
    switch (state) {
      case CopyState.PENDING: return {isDisabled: false, style: '', text: 'Kopioi'}
      case CopyState.SUCCESS: return {isDisabled: true, style: '--success', text: 'Kopioitu'}
      case CopyState.ERROR: return {isDisabled: true, style: '--error', text: 'Kopiointi epäonnistui'}
    }
  })

  return (
    <div className='copyable-text'>
      <div className='copyable-text__heading'>
        <Text name={heading}/>
      </div>
      <textarea readOnly cols='30' rows='10' defaultValue={message}/>
      {buttonState.map(({isDisabled, style, text}) => (
        <button
          className={style ? `button button${style}` : 'button'}
          disabled={isDisabled}
          onClick={copyToClipboard(message, copyState)}
        >
          <Text name={text}/>
        </button>
      ))}
    </div>
  )
}