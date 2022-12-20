import { OsaamisenHankkimistapa } from './OsaamisenHankkimistapa'

/**
 * OsaamisenHankkimistapajakso
 *
 * @see `fi.oph.koski.schema.OsaamisenHankkimistapajakso`
 */
export type OsaamisenHankkimistapajakso = {
  $class: 'fi.oph.koski.schema.OsaamisenHankkimistapajakso'
  alku: string
  loppu?: string
  osaamisenHankkimistapa: OsaamisenHankkimistapa
}

export const OsaamisenHankkimistapajakso = (o: {
  alku: string
  loppu?: string
  osaamisenHankkimistapa: OsaamisenHankkimistapa
}): OsaamisenHankkimistapajakso => ({
  $class: 'fi.oph.koski.schema.OsaamisenHankkimistapajakso',
  ...o
})

export const isOsaamisenHankkimistapajakso = (
  a: any
): a is OsaamisenHankkimistapajakso =>
  a?.$class === 'OsaamisenHankkimistapajakso'