import React from 'react'
import { CommonProps } from '../components-v2/CommonProps'
import { DateEdit, DateView } from '../components-v2/controls/DateField'
import {
  FieldEditorProps,
  FieldViewerProps
} from '../components-v2/forms/FormField'
import {
  ArvosanaEdit,
  ArvosanaView
} from '../components-v2/opiskeluoikeus/ArvosanaField'
import { OsasuoritusSubproperty } from '../components-v2/opiskeluoikeus/OsasuoritusProperty'
import { ArvosanaOf } from '../util/schema'
import {
  VSTArviointi,
  VSTOsasuoritus,
  hasPäiväInArviointi,
  isVSTOsasuoritusArvioinnilla
} from './typeguards'

export type VSTArviointiViewProps = CommonProps<
  FieldViewerProps<VSTArviointi, {}>
>

export const VSTArviointiView = (props: VSTArviointiViewProps) => {
  const startRow = (props.index || 0) * 2
  return props.value ? (
    <>
      <OsasuoritusSubproperty rowNumber={startRow} label="Arvosana">
        <ArvosanaView value={props.value} />
      </OsasuoritusSubproperty>
      {hasPäiväInArviointi(props.value) && (
        <OsasuoritusSubproperty rowNumber={startRow + 1} label="Päivämäärä">
          <DateView value={props.value.päivä} />
        </OsasuoritusSubproperty>
      )}
    </>
  ) : null
}

export type VSTArviointiEditProps<T extends VSTArviointi> = CommonProps<
  FieldEditorProps<
    T,
    {
      createArviointi: (arvosana: ArvosanaOf<T>) => T
      osasuoritus: VSTOsasuoritus
    }
  >
>

export const VSTArviointiEdit = <T extends VSTArviointi>(
  props: VSTArviointiEditProps<T>
) => {
  if (!isVSTOsasuoritusArvioinnilla(props.osasuoritus)) {
    return <div>{'Ei arviointia'}</div>
  }

  const startRow = (props.index || 0) * 2
  return props.value ? (
    <>
      <OsasuoritusSubproperty rowNumber={startRow} label="Arvosana">
        <ArvosanaEdit {...props} createArviointi={props.createArviointi} />
      </OsasuoritusSubproperty>
      {hasPäiväInArviointi(props.value) && (
        <OsasuoritusSubproperty rowNumber={startRow + 1} label="Päivämäärä">
          <DateEdit
            value={props.value.päivä}
            onChange={(päivä) =>
              päivä && props.onChange({ ...props.value, päivä } as T)
            }
          />
        </OsasuoritusSubproperty>
      )}
    </>
  ) : null
}
