import React, { useCallback } from 'react'
import { useKoodistoFiller } from '../../appstate/koodisto'
import { CommonProps } from '../../components-v2/CommonProps'
import { Column, ColumnRow } from '../../components-v2/containers/Columns'
import { FormModel, FormOptic } from '../../components-v2/forms/FormModel'
import { KoodistoSelect } from '../../components-v2/opiskeluoikeus/KoodistoSelect'
import { t } from '../../i18n/i18n'
import { Koodistokoodiviite } from '../../types/fi/oph/koski/schema/Koodistokoodiviite'
import { VapaanSivistystyönOpiskeluoikeus } from '../../types/fi/oph/koski/schema/VapaanSivistystyonOpiskeluoikeus'
import { OsasuoritusOf } from '../../util/schema'
import { VSTSuoritusOsasuorituksilla } from './types'
import { appendOptional } from '../../util/array'

type AddKoodistonOsasuoritusProps<
  T extends VSTSuoritusOsasuorituksilla,
  URI extends string
> = CommonProps<{
  form: FormModel<VapaanSivistystyönOpiskeluoikeus>
  path: FormOptic<VapaanSivistystyönOpiskeluoikeus, T>
  koodistoUri: URI
  createOsasuoritus: (koodiviite: Koodistokoodiviite<URI>) => OsasuoritusOf<T>
  level: number
  placeholder?: string
}>

export const AddKoodistonOsasuoritus = <
  T extends VSTSuoritusOsasuorituksilla,
  URI extends string
>(
  props: AddKoodistonOsasuoritusProps<T, URI>
) => {
  const { createOsasuoritus, form, path } = props
  const fillKoodistot = useKoodistoFiller()

  const onSelect = useCallback(
    async (koodiviite: Koodistokoodiviite<URI>) => {
      const osasuorituksetPath = path.prop(
        'osasuoritukset'
      ) as any as FormOptic<
        VapaanSivistystyönOpiskeluoikeus,
        OsasuoritusOf<T>[] | undefined
      >
      const osasuoritus = await fillKoodistot(createOsasuoritus(koodiviite))
      form.updateAt(osasuorituksetPath, appendOptional(osasuoritus))
    },
    [createOsasuoritus, fillKoodistot, form, path]
  )

  return (
    <ColumnRow indent={props.level + 1}>
      <Column span={10}>
        <KoodistoSelect
          koodistoUri={props.koodistoUri}
          addNewText={props.placeholder || t('Lisää osasuoritus')}
          onSelect={onSelect}
          testId="addOsasuoritus"
        />
      </Column>
    </ColumnRow>
  )
}
