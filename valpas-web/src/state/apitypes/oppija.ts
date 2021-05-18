import { ISODate, Oid } from "../common"
import { HakuLaajatTiedot, HakuSuppeatTiedot } from "./haku"
import { HenkilöLaajatTiedot, HenkilöSuppeatTiedot } from "./henkilo"
import {
  OpiskeluoikeusLaajatTiedot,
  OpiskeluoikeusSuppeatTiedot,
} from "./opiskeluoikeus"
import { Yhteystiedot, YhteystietojenAlkuperä } from "./yhteystiedot"

export type OppijaHakutilanteillaLaajatTiedot = {
  oppija: OppijaLaajatTiedot
  hakutilanteet: HakuLaajatTiedot[]
  hakutilanneError?: string
  yhteystiedot: Yhteystiedot<YhteystietojenAlkuperä>[]
}

export type OppijaHakutilanteillaSuppeatTiedot = {
  oppija: OppijaSuppeatTiedot
  hakutilanteet: HakuSuppeatTiedot[]
  hakutilanneError?: string
}

export type OppijaLaajatTiedot = {
  henkilö: HenkilöLaajatTiedot
  opiskeluoikeudet: OpiskeluoikeusLaajatTiedot[]
  opiskelee: boolean
  oppivelvollisuusVoimassaAsti?: ISODate
  oikeutetutOppilaitokset: Oid[]
}

export type OppijaSuppeatTiedot = {
  henkilö: HenkilöSuppeatTiedot
  opiskeluoikeudet: OpiskeluoikeusSuppeatTiedot[]
  opiskelee: boolean
  oppivelvollisuusVoimassaAsti?: ISODate
}