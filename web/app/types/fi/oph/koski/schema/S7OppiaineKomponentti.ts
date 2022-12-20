import { Koodistokoodiviite } from './Koodistokoodiviite'
import { LocalizedString } from './LocalizedString'

/**
 * S7OppiaineKomponentti
 *
 * @see `fi.oph.koski.schema.S7OppiaineKomponentti`
 */
export type S7OppiaineKomponentti = {
  $class: 'fi.oph.koski.schema.S7OppiaineKomponentti'
  tunniste: Koodistokoodiviite<
    'europeanschoolofhelsinkis7oppiaineenkomponentti',
    string
  >
}

export const S7OppiaineKomponentti = (o: {
  tunniste: Koodistokoodiviite<
    'europeanschoolofhelsinkis7oppiaineenkomponentti',
    string
  >
}): S7OppiaineKomponentti => ({
  $class: 'fi.oph.koski.schema.S7OppiaineKomponentti',
  ...o
})

export const isS7OppiaineKomponentti = (a: any): a is S7OppiaineKomponentti =>
  a?.$class === 'S7OppiaineKomponentti'