import React, { useEffect, useState } from 'react'
import { t } from '../../i18n/i18n'
import { Koodistokoodiviite } from '../../types/fi/oph/koski/schema/Koodistokoodiviite'
import { CommonProps } from '../CommonProps'
import { OptionList, Select } from '../controls/Select'
import { FieldViewerProps, FieldEditorProps } from '../forms/FormField'

type PerusteViewProps = CommonProps<FieldViewerProps<string | undefined, {}>>

// TODO: Tyypitys paremmaksi
type Peruste = Omit<
  Koodistokoodiviite<string, string>,
  '$class' | 'koodistoVersio' | 'lyhytNimi'
>

// TODO: Perusteen linkitys
export const PerusteView: React.FC<PerusteViewProps> = (props) => {
  return <div>{props.value || '-'}</div>
}

type PerusteEditProps = CommonProps<
  FieldEditorProps<
    string | undefined,
    {
      diaariNumero: string
    }
  >
>

function useTutkinnonPerusteet(diaariNumero: string) {
  const [perusteet, setPerusteet] = useState<Peruste[]>([])
  useEffect(() => {
    const ab = new AbortController()
    // TODO: Selvitä, onko fetchin käyttö ok kun datan hakemiselle löytyy myös hookkeja
    fetch(
      `/koski/api/tutkinnonperusteet/diaarinumerot/suorituksentyyppi/${diaariNumero}`,
      {
        method: 'GET',
        headers: {
          Accept: 'application/json'
        },
        signal: ab.signal
      }
    )
      .then((res) => res.json())
      .then((data) => {
        setPerusteet(data)
      })
      .catch(console.error)
    return () => {
      ab.abort('Component unmounted')
    }
  }, [diaariNumero])

  return perusteet
}

export const PerusteEdit: React.FC<PerusteEditProps> = (props) => {
  const perusteet = useTutkinnonPerusteet(props.diaariNumero)
  const mappedPerusteet: OptionList<string> = perusteet.map((p) => ({
    key: p.koodiarvo,
    label: `${p.koodiarvo} ${t(p.nimi)}`,
    value: p.koodiarvo
  }))
  return (
    <Select
      onChange={(opt) => {
        props.onChange(opt?.value || '')
      }}
      initialValue={props.value}
      value={props.value || ''}
      options={mappedPerusteet}
    />
  )
}
