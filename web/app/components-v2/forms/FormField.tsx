import * as A from 'fp-ts/Array'
import * as NEA from 'fp-ts/NonEmptyArray'
import { constant } from 'fp-ts/lib/function'
import React, { useCallback, useEffect, useMemo } from 'react'
import { useKoodistoFiller } from '../../appstate/koodisto'
import { deepEqual } from '../../util/fp/objects'
import { FormModel, FormOptic, getValue } from './FormModel'
import { useFormErrors } from './useFormErrors'
import { ValidationError } from './validator'

export type FieldViewBaseProps<T, P extends object = object> = P & {
  value?: T | undefined
}

export type FieldEditBaseProps<T, P extends object = object> = P & {
  initialValue?: T | undefined
  value?: T | undefined
  optional?: boolean
  onChange: (value?: T) => void
  errors?: NEA.NonEmptyArray<ValidationError>
}

export type FormFieldProps<
  O extends object,
  T,
  VP extends object,
  EP extends object,
  VIEW_PROPS extends FieldViewBaseProps<T, VP>,
  EDIT_PROPS extends FieldEditBaseProps<T, EP>
> = {
  form: FormModel<O>

  updateAlso?: Array<SideUpdate<O, T, any>>
  errorsFromPath?: string
  view: React.FC<VIEW_PROPS>
  viewProps?: VP
  edit?: React.FC<EDIT_PROPS>
  editProps?: EP
  auto?: () => T | undefined
} & (
  | { path: FormOptic<O, T>; optional?: false }
  | { path: FormOptic<O, T | undefined>; optional: true }
)

export type SideUpdate<O extends object, T, S> = (value?: T) => {
  path: FormOptic<O, S>
  update: (sideValue?: S) => S
}

export const sideUpdate =
  <O extends object, T, S>(
    path: FormOptic<O, S>,
    transform: (value?: T, sideValue?: S) => S
  ): SideUpdate<O, T, S> =>
  (value?: T) => ({
    path,
    update: (sideValue?: S) => transform(value, sideValue)
  })

export const FormField = <
  O extends object,
  T,
  VP extends object,
  EP extends object,
  VIEW_PROPS extends FieldViewBaseProps<T, VP>,
  EDIT_PROPS extends FieldEditBaseProps<T, EP>
>(
  props: FormFieldProps<O, T, VP, EP, VIEW_PROPS, EDIT_PROPS>
) => {
  const {
    form,
    path,
    updateAlso: secondaryPaths,
    view,
    viewProps,
    edit,
    editProps,
    auto,
    errorsFromPath,
    optional
  } = props
  const fillKoodistot = useKoodistoFiller()

  const initialValue = useMemo(
    () => getValue(path as FormOptic<O, T | undefined>)(form.initialState),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [form.initialState]
  )
  const value = useMemo(
    () => getValue(path as FormOptic<O, T | undefined>)(form.state),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [form.state]
  )

  const errors = useFormErrors(
    form,
    errorsFromPath || (path as any as FormOptic<O, object>)
  )

  const set = useCallback(
    async (newValue?: T) => {
      const filledValue = await fillKoodistot(newValue)
      form.updateAt(path as FormOptic<O, T | undefined>, constant(filledValue))
      secondaryPaths?.forEach(<S,>(update: SideUpdate<O, T, S>) => {
        const side = update(filledValue)
        form.updateAt(side.path, side.update)
      })
      form.validate()
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [form]
  )

  useEffect(() => {
    if (form.editMode && auto) {
      const automaticValue = auto()
      if (automaticValue && !deepEqual(automaticValue, value)) {
        set(automaticValue)
      }
    }
  }, [form.editMode, auto, value, set])

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const View = useMemo(() => view, [JSON.stringify(viewProps)])
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const Edit = useMemo(() => edit, [JSON.stringify(editProps)])

  if (form.editMode) {
    if (Edit) {
      return (
        // @ts-ignore - TODO tyyppicastaus?
        <Edit
          {...editProps}
          key="edit"
          initialValue={initialValue}
          value={value}
          optional={Boolean(optional)}
          onChange={set}
          errors={A.isNonEmpty(errors) ? errors : undefined}
        />
      )
    }
  }

  // @ts-ignore - TODO tyyppicastaus?
  return <View {...viewProps} key="view" value={value} />
}

export const Nothing: React.FC = () => null