import { FullConfig } from '@playwright/test'
import { join } from 'path'
import fs from 'fs/promises'

type Virkailija = {
  username: string
  password: string
  type: 'virkailija'
}

function sanitizeString(str: string) {
  const tmp = str.replace(/[^a-z0-9äöåáéíóúñü \.,_-]/gim, '')
  return tmp.trim()
}

function isVirkailija(x: unknown): x is Virkailija {
  const k = x as any
  return (
    k.type &&
    k.type === 'virkailija' &&
    k.username &&
    typeof k.username === 'string' &&
    k.password &&
    typeof k.password === 'string'
  )
}

function isKansalainen(x: unknown): x is Kansalainen {
  const k = x as any
  return (
    k.type && k.type === 'kansalainen' && k.hetu && typeof k.hetu === 'string'
  )
}

type Kansalainen = {
  hetu: string
  type: 'kansalainen'
}

const virkailija = (username: string, password: string): Virkailija => ({
  username,
  password,
  type: 'virkailija'
})

const kansalainen = (hetu: string): Kansalainen => ({
  hetu,
  type: 'kansalainen'
})

async function globalSetup(config: FullConfig) {
  const usersFile = await fs
    .readFile(join(__dirname, 'users.json'))
    .then((data) => data.toString('utf-8'))
  const userData = JSON.parse(usersFile).data
  if (!Array.isArray(userData)) {
    throw new Error('Userdata is not an Array')
  }
  const users = userData.map((user) => {
    if (isKansalainen(user)) {
      return kansalainen(user.hetu)
    } else if (isVirkailija(user)) {
      return virkailija(user.username, user.password)
    }
    throw new Error(
      'User is not virkailija or kansalainen, please check users.json for errors'
    )
  })

  // Kirjoitetaan user.types.ts -tiedostoon virkailijoiden ja kansalaisten tyyppidefinitiot
  const virkailijaUsers = users.filter(isVirkailija).map((v) => v.username)
  await fs.writeFile(
    join(__dirname, 'users.types.ts'),
    `// AUTOGENERATED FILE, DOT NOT MODIFY
// AUTOGENEROITU TIEDOSTO
export type Virkailija = ${virkailijaUsers
      .map((v) => `'${sanitizeString(v)}'`)
      .join(' | ')}
export type Kansalainen = string`
  )
}

export default globalSetup
