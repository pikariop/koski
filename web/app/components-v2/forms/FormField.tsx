import React, { useCallback, useMemo } from 'react'
import { useKoodistoFiller } from '../../appstate/koodisto'
import { deepEqual } from '../../util/fp/objects'
import { FieldRenderer, FormModel, getValue } from './FormModel'

export const FormField = <O extends object, T>({
  form,
  path,
  updateAlso: secondaryPaths,
  view,
  edit,
  auto
}: FieldRenderer<O, T> & { form: FormModel<O> }) => {
  const fillKoodistot = useKoodistoFiller()

  const optics = useMemo(
    () => [path, ...(secondaryPaths || [])],
    [path, ...(secondaryPaths || [])]
  )
  const initialValue = useMemo(
    () => getValue(optics[0])(form.initialState),
    [form.initialState]
  )
  const value = useMemo(() => getValue(optics[0])(form.state), [form.state])

  const set = useCallback(
    async (newValue: T) => {
      const filledValue = await fillKoodistot(newValue)
      const getValue = () => filledValue
      optics.forEach((optic) => form.updateAt(optic, getValue))
      form.validate()
    },
    [form]
  )
  const View = useMemo(() => view, [])
  const Edit = useMemo(() => edit, [])

  if (form.editMode) {
    if (auto) {
      const automaticValue = auto()
      if (automaticValue && !deepEqual(automaticValue, value)) {
        set(automaticValue)
      }
      return <View key="auto" value={automaticValue} />
    }

    if (Edit) {
      return (
        <Edit
          key="edit"
          initialValue={initialValue}
          value={value}
          onChange={set}
        />
      )
    }
  }

  return <View key="view" value={value} />
}
