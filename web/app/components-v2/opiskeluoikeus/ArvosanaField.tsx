import * as A from 'fp-ts/Array'
import * as O from 'fp-ts/Option'
import { pipe } from 'fp-ts/lib/function'
import React, { useMemo } from 'react'
import { useConstraint } from '../../appstate/constraints'
import { useKoodistoOfConstraint } from '../../appstate/koodisto'
import { t } from '../../i18n/i18n'
import { constraintObjectProp } from '../../util/constraints'
import { toKoodistokoodiviiteValue } from '../../util/koodisto'
import { ArviointiLike, viimeisinArviointi } from '../../util/schema'
import { schemaClassName } from '../../util/types'
import { common, CommonProps } from '../CommonProps'
import {
  groupKoodistoToOptions,
  Select,
  SelectOption
} from '../controls/Select'
import { FieldViewBaseProps, FieldEditBaseProps } from '../forms/FormField'

export type ArviointiViewProps<T extends ArviointiLike> = CommonProps<
  FieldViewBaseProps<T[] | undefined>
>

export const ArvosanaView = <T extends ArviointiLike>(
  props: ArviointiViewProps<T>
) => {
  const arviointi = props.value && viimeisinArviointi(props.value)
  return arviointi ? (
    <span {...common(props)}>{t(arviointi.arvosana?.nimi)}</span>
  ) : null
}

export type ArviointiEditProps<T extends ArviointiLike> = CommonProps<
  FieldEditBaseProps<T[] | undefined> & {
    createArviointi: (arvosana: T['arvosana']) => T
  }
>

export const ArvosanaEdit = <T extends ArviointiLike>(
  props: ArviointiEditProps<T>
) => {
  const schemaClass = useMemo(
    // @ts-ignore - koska value ja initialValue voivat olla tyhjiä, saadaan $class varmuudella selvitettyä syöttämällä createArviointi-callbackille tyhjä arvosana
    () => schemaClassName(props.createArviointi(null).$class),
    []
  )
  const arviointiC = useConstraint(schemaClass)
  const koodisto = useKoodistoOfConstraint(
    constraintObjectProp('arvosana')(arviointiC)
  )
  const groupedKoodisto = useMemo(
    () => koodisto && groupKoodistoToOptions(koodisto),
    [koodisto]
  )

  const initialArviointi =
    props.initialValue && viimeisinArviointi(props.initialValue)
  const initialValue =
    initialArviointi?.arvosana &&
    toKoodistokoodiviiteValue(initialArviointi.arvosana)
  const arviointi = props.value && viimeisinArviointi(props.value)
  const selectedValue =
    arviointi?.arvosana && toKoodistokoodiviiteValue(arviointi?.arvosana)

  const onChange = (option?: SelectOption<T['arvosana']>) => {
    if (option) {
    }
    props.onChange(
      option &&
        updateArvioinnit(
          props.createArviointi(option.value),
          props.initialValue || []
        )
    )
  }

  return (
    groupedKoodisto && (
      <Select
        initialValue={initialValue}
        value={selectedValue}
        options={groupedKoodisto}
        onChange={onChange}
      />
    )
  )
}

const updateArvioinnit = <T extends ArviointiLike>(
  arviointi: T,
  arvioinnit: T[]
): T[] =>
  pipe(
    arvioinnit,
    A.init,
    O.fold(
      () => [arviointi],
      (vanhatArvioinnit) => [...vanhatArvioinnit, arviointi]
    )
  )
