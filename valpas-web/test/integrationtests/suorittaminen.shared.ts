import { Oid } from "../../src/state/common"
import {
  createSuorittaminenHetuhakuPath,
  createSuorittaminenPath,
  createSuorittaminenPathWithOrg,
} from "../../src/state/paths"
import {
  clickElement,
  expectElementEventuallyVisible,
} from "../integrationtests-env/browser/content"
import {
  helsinginMedialukioOid,
  jyväskylänNormaalikouluOid,
} from "../integrationtests/oids"

export const jklNormaalikouluSuorittaminenTableHead = "Oppivelvolliset (13)"
export const jklNormaalikouluSuorittaminenTableContent = `
  Jkl-Lukio-Kulosaarelainen Valpas          |  1.1.2004  | Lukion oppimäärä          | Läsnä                   | Jyväskylän normaalikoulu |  1.8.2019  | –         | Kulosaaren ala-aste, Perusopetus             |  1.1.2022 asti
  Jkl-Nivel-Kulosaarelainen Valpas          |  1.1.2004  | Perusopetuksen lisäopetus | Läsnä                   | Jyväskylän normaalikoulu | 15.8.2012  | –         | Kulosaaren ala-aste, Perusopetus             |  1.1.2022 asti
  Kahdella-oppija-oidilla Valpas            | 15.2.2005  | Lukion oppimäärä          | Läsnä                   | Jyväskylän normaalikoulu |  1.8.2019  | –         | –                                            | 15.2.2023 asti
  Kahdella-oppija-oidilla-ilmo Valpas       |  4.6.2005  | Lukion oppimäärä          | Läsnä                   | Jyväskylän normaalikoulu |  1.8.2019  | –         | –                                            |  4.6.2023 asti
  Kahdella-oppija-oidilla-ilmo-2 Valpas     |  3.6.2005  | Lukion oppimäärä          | Läsnä                   | Jyväskylän normaalikoulu |  1.8.2019  | –         | –                                            |  3.6.2023 asti
  Kaksois-tutkinnosta-valmistunut Valpas    | 26.9.2005  | Lukion oppimäärä          | Valmistunut             | Jyväskylän normaalikoulu |  1.8.2019  | 2.9.2021  | –                                            | 26.9.2023 asti
  Lukio-opiskelija Valpas                   |  7.5.2004  | Lukion oppimäärä          | Läsnä                   | Jyväskylän normaalikoulu |  1.8.2019  | –         | –                                            |  7.5.2022 asti
  Lukio-opiskelija-valmistunut Valpas       | 27.11.2005 | Lukion oppimäärä          | Valmistunut             | Jyväskylän normaalikoulu |  1.8.2019  | 2.9.2021  | –                                            | 27.11.2023 asti
  Lukio-väliaikaisesti-keskeytynyt Valpas   | 30.5.2004  | Lukion oppimäärä       | Väliaikaisesti keskeytynyt | Jyväskylän normaalikoulu |  1.8.2021  | –         | –                                            | 30.5.2022 asti
  LukionAloittanut Valpas                   | 29.4.2005  | Lukion oppimäärä          | Läsnä                   | Jyväskylän normaalikoulu | 15.8.2021  | –         | –                                            | 29.4.2023 asti
  LukionAloittanut-ilmo Valpas              | 11.4.2005  | Lukion oppimäärä          | Läsnä                   | Jyväskylän normaalikoulu | 15.8.2021  | –         | –                                            | 11.4.2023 asti
  LukionAloittanutJaLopettanut-ilmo Valpas  |  5.4.2005  | Lukion oppimäärä          | warningEronnut          | Jyväskylän normaalikoulu | 15.8.2021  | 19.9.2021 | –                                            |  5.4.2023 asti
  LukionLokakuussaAloittanut Valpas         | 18.4.2005  | Lukion oppimäärä          | Läsnä                   | Jyväskylän normaalikoulu |  3.10.2021 | –         | –                                            | 18.4.2023 asti
    `

export const stadinAmmattiopistoSuorittaminenTableHead = "Oppivelvolliset (12)"

export const stadinAmmattiopistoSuorittaminenTableContent = `
  Amis-eronnut Valpas                                                  | 1.8.2005   | Ammatillinen tutkinto | warningEronnut | Stadin ammatti- ja aikuisopisto, Lehtikuusentien toimipaikka | 1.8.2021 | 2.9.2021 | –                                                             | 1.8.2023 asti
  Amis-eronnut-nivelvaihe-ei-kelpaa Valpas                             | 10.11.2005 | Ammatillinen tutkinto | warningEronnut | Stadin ammatti- ja aikuisopisto, Lehtikuusentien toimipaikka | 1.8.2021 | 2.9.2021 | Helsingin Saksalainen koulu, Perusopetuksen lisäopetus        | 10.11.2023 asti
  Amis-eronnut-nivelvaihe-vstssa-ei-kelpaa Valpas                      | 9.6.2004   | Ammatillinen tutkinto | warningEronnut | Stadin ammatti- ja aikuisopisto, Lehtikuusentien toimipaikka | 1.8.2021 | 2.9.2021 | Varsinais-Suomen kansanopisto, Vapaan sivistystyön koulutus   | 9.6.2022 asti
  Amis-eronnut-uusi-oo-tulevaisuudessa-keskeyttänyt Valpas             | 24.9.2005  | Ammatillinen tutkinto | warningEronnut | Stadin ammatti- ja aikuisopisto, Lehtikuusentien toimipaikka | 1.8.2021 | 2.9.2021 | hourglass_empty1.10.2021 alkaen: Omnia, Ammatillinen koulutus | 24.9.2023 asti*
  Amis-eronnut-uusi-peruskoulussa-keskeyttänyt-tulevaisuudessa Valpas  | 10.2.2005  | Ammatillinen tutkinto | warningEronnut | Stadin ammatti- ja aikuisopisto, Lehtikuusentien toimipaikka | 1.8.2021 | 2.9.2021 | Helsingin Saksalainen koulu, Perusopetus                      | 10.2.2023 asti
  Amis-lomalla Valpas                                                  | 3.9.2005   | Ammatillinen tutkinto | Loma           | Stadin ammatti- ja aikuisopisto, Lehtikuusentien toimipaikka | 1.8.2021 | –        | –                                                             | 3.9.2023 asti
  Amis-monta-oota Valpas                                               | 28.1.2005  | Ammatillinen tutkinto | Läsnä          | Stadin ammatti- ja aikuisopisto, Lehtikuusentien toimipaikka | 1.9.2012 | –        | Stadin ammatti- ja aikuisopisto, Ammatillinen koulutus        | 28.1.2023 asti
  Amis-monta-oota Valpas                                               | 28.1.2005  | VALMA                 | Läsnä          | Stadin ammatti- ja aikuisopisto                              | 1.9.2012 | –        | Stadin ammatti- ja aikuisopisto, Ammatillinen koulutus        | 28.1.2023 asti
  Amis-opiskelija Valpas                                               | 23.10.2005 | Ammatillinen tutkinto | Läsnä          | Stadin ammatti- ja aikuisopisto, Lehtikuusentien toimipaikka | 1.9.2012 | –        | –                                                             | 23.10.2023 asti
  Amis-useita-pts Valpas                                               | 28.5.2005  | Ammatillinen tutkinto | Läsnä          | Useita toimipisteitä                                         | 1.9.2012 | –        | –                                                             | 28.5.2023 asti
  Telma-opiskelija Valpas                                              | 2.8.2005   | TELMA                 | Läsnä          | Stadin ammatti- ja aikuisopisto                              | 1.9.2012 | –        | –                                                             | 2.8.2023 asti
  Valma-opiskelija Valpas                                              | 19.1.2005  | VALMA                 | Läsnä          | Stadin ammatti- ja aikuisopisto                              | 1.9.2012 | –        | –                                                             | 19.1.2023 asti
    `

export const suorittaminenHetuhakuPath = createSuorittaminenHetuhakuPath(
  "/virkailija"
)
export const suorittaminenListaPath = createSuorittaminenPath("/virkailija")

export const suorittaminenListaJklPath = createSuorittaminenPathWithOrg(
  "/virkailija",
  jyväskylänNormaalikouluOid
)

export const suorittaminenListaHkiPath = createSuorittaminenPathWithOrg(
  "/virkailija",
  helsinginMedialukioOid
)

export const openSuorittaminenOppijaView = async (oppijaOid: Oid) => {
  const selector = `.suorittaminen .table__row[data-row*="${oppijaOid}"] td:first-child a`
  await expectElementEventuallyVisible(selector)
  await clickElement(selector)
}

export const openSuorittaminenAnyOppijaView = () =>
  clickElement(`.suorittaminen .table__row:first-child td:first-child a`)