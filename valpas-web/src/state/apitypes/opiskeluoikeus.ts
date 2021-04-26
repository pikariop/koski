import * as A from "fp-ts/Array"
import * as Ord from "fp-ts/Ord"
import * as string from "fp-ts/string"
import { ISODate, Language, Oid } from "../common"
import { Opiskeluoikeudentyyppi } from "./koodistot"
import { Oppilaitos, Toimipiste } from "./organisaatiot"
import { ValpasOpiskeluoikeudenTila } from "./valpasopiskeluoikeudentila"

export type OpiskeluoikeusLaajatTiedot = {
  oid: Oid
  onValvottava: boolean
  tyyppi: Opiskeluoikeudentyyppi
  oppilaitos: Oppilaitos
  toimipiste?: Toimipiste
  alkamispäivä: ISODate
  päättymispäivä?: ISODate
  ryhmä?: string
  tarkastelupäivänTila: ValpasOpiskeluoikeudenTila
  oppivelvollisuudenSuorittamiseenKelpaava: boolean
}

export type OpiskeluoikeusSuppeatTiedot = {
  oid: Oid
  onValvottava: boolean
  tyyppi: Opiskeluoikeudentyyppi
  oppilaitos: Oppilaitos
  toimipiste?: Toimipiste
  ryhmä?: string
  tarkastelupäivänTila: ValpasOpiskeluoikeudenTila
  alkamispäivä: ISODate
  oppivelvollisuudenSuorittamiseenKelpaava: boolean
}

const opiskeluoikeusDateOrd = (key: keyof OpiskeluoikeusLaajatTiedot) =>
  Ord.contramap(
    (o: OpiskeluoikeusLaajatTiedot) => (o[key] as ISODate) || "0000-00-00"
  )(string.Ord)

const alkamispäiväOrd = opiskeluoikeusDateOrd("alkamispäivä")
const päättymispäiväOrd = opiskeluoikeusDateOrd("päättymispäivä")
const tyyppiNimiOrd = (lang: Language) =>
  Ord.contramap((o: OpiskeluoikeusLaajatTiedot) => o.tyyppi.nimi?.[lang] || "")(
    string.Ord
  )

export const sortOpiskeluoikeusLaajatTiedot = (lang: Language) =>
  A.sortBy<OpiskeluoikeusLaajatTiedot>([
    Ord.reverse(alkamispäiväOrd),
    Ord.reverse(päättymispäiväOrd),
    tyyppiNimiOrd(lang),
  ])

export const isValvottavaOpiskeluoikeus = (
  organisaatioOid: string | undefined
) => (oo: OpiskeluoikeusSuppeatTiedot) =>
  oo.onValvottava && oo.oppilaitos.oid == organisaatioOid

export const valvottavatOpiskeluoikeudet = (
  organisaatioOid: string | undefined,
  opiskeluoikeudet: Array<OpiskeluoikeusSuppeatTiedot>
) => opiskeluoikeudet.filter(isValvottavaOpiskeluoikeus(organisaatioOid))

export const taulukossaNäytettäväOpiskeluoikeus = (
  opiskeluoikeus: OpiskeluoikeusSuppeatTiedot
): boolean => {
  const tila = opiskeluoikeus.tarkastelupäivänTila.koodiarvo
  return (
    opiskeluoikeus.oppivelvollisuudenSuorittamiseenKelpaava &&
    (tila === "voimassa" || tila === "voimassatulevaisuudessa")
  )
}
