import * as A from 'fp-ts/Array'
import { pipe } from 'fp-ts/lib/function'
import * as O from 'fp-ts/Option'
import React, { useCallback, useState } from 'react'
import { Arvioitsija } from '../../types/fi/oph/koski/schema/Arvioitsija'
import { common, CommonProps } from '../CommonProps'
import { FlatButton } from '../controls/FlatButton'
import { Removable } from '../controls/Removable'
import { TextEdit } from '../controls/TextField'
import { FieldEditBaseProps, FieldViewBaseProps } from '../forms/FormField'
import { narrowErrorsToLeaf } from '../forms/validator'

export type ArvioitsijatViewProps = CommonProps<
  FieldViewBaseProps<Arvioitsija[] | undefined>
>

export const ArvioitsijatView: React.FC<ArvioitsijatViewProps> = (props) => {
  return props.value && A.isNonEmpty(props.value) ? (
    <ul {...common(props, ['ArvioitsijatView'])}>
      {props.value.map((a, i) => (
        <li key={i}>{a.nimi}</li>
      ))}
    </ul>
  ) : (
    <span {...common(props, ['ArvioitsijatView'])}>{'–'}</span>
  )
}

export type ArvioitsijatEditProps = CommonProps<
  FieldEditBaseProps<Arvioitsija[] | undefined>
>

export const ArvioitsijatEdit: React.FC<ArvioitsijatEditProps> = (props) => {
  const [focusNew, setFocusNew] = useState(false)

  const { onChange, value } = props
  const onChangeCB = (index: number) => (nimi?: string) => {
    pipe(
      value || [],
      A.updateAt(index, Arvioitsija({ nimi: nimi || '' })),
      O.fold(
        () =>
          console.error(
            `Could not add ${nimi} at ${index}, original array:`,
            value
          ),
        onChange
      )
    )
  }

  const addNew = useCallback(() => {
    onChange([...(value || []), Arvioitsija({ nimi: '' })])
    setFocusNew(true)
  }, [onChange, value])

  const removeAt = (index: number) => () => {
    pipe(
      props.value || [],
      A.deleteAt(index),
      O.fold(
        () =>
          console.error(
            `Could not remove at ${index}, original array:`,
            props.value
          ),
        props.onChange
      )
    )
  }

  return (
    <ul {...common(props, ['ArvioitsijatEdit'])}>
      {props.value &&
        props.value.map((a, i) => (
          <li key={i}>
            <Removable onClick={removeAt(i)}>
              <TextEdit
                optional
                value={a.nimi}
                onChange={onChangeCB(i)}
                errors={narrowErrorsToLeaf(`${i}.nimi`)(props.errors)}
                autoFocus={
                  props.value && i === props.value.length - 1 && focusNew
                }
              />
            </Removable>
          </li>
        ))}
      <li>
        <FlatButton onClick={addNew}>{'lisää uusi'}</FlatButton>
      </li>
    </ul>
  )
}