import { Koodistokoodiviite } from './Koodistokoodiviite'
import { LocalizedString } from './LocalizedString'

/**
 * PrimaryLapsiAlaoppimisalue
 *
 * @see `fi.oph.koski.schema.PrimaryLapsiAlaoppimisalue`
 */
export type PrimaryLapsiAlaoppimisalue = {
  $class: 'fi.oph.koski.schema.PrimaryLapsiAlaoppimisalue'
  tunniste: Koodistokoodiviite<
    'europeanschoolofhelsinkiprimarylapsialaoppimisalue',
    string
  >
}

export const PrimaryLapsiAlaoppimisalue = (o: {
  tunniste: Koodistokoodiviite<
    'europeanschoolofhelsinkiprimarylapsialaoppimisalue',
    string
  >
}): PrimaryLapsiAlaoppimisalue => ({
  $class: 'fi.oph.koski.schema.PrimaryLapsiAlaoppimisalue',
  ...o
})

export const isPrimaryLapsiAlaoppimisalue = (
  a: any
): a is PrimaryLapsiAlaoppimisalue => a?.$class === 'PrimaryLapsiAlaoppimisalue'