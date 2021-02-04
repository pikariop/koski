import React from "react"
import { IconSection } from "../../components/containers/IconSection"
import { OpiskeluIcon } from "../../components/icons/Icon"
import { TertiaryHeading } from "../../components/typography/headings"
import { getLocalized, T } from "../../i18n/i18n"
import { KoodistoKoodiviite } from "../../state/koodistot"
import { Oppija } from "../../state/oppijat"
import { ISODate } from "../../state/types"
import { parseYear } from "../../utils/date"

export type OppijanOpiskeluhistoriaProps = {
  oppija: Oppija
}

export const OppijanOpiskeluhistoria = (
  props: OppijanOpiskeluhistoriaProps
) => {
  const historia = props.oppija.opiskeluoikeushistoria || []
  return (
    <>
      {historia.map((opiskeluoikeus) => {
        const nimi = koodistonimi(opiskeluoikeus.tyyppi)
        const range = yearRangeString(
          opiskeluoikeus.alkamispäivä,
          opiskeluoikeus.päättymispäivä
        )

        return (
          <IconSection
            key={opiskeluoikeus.oid}
            icon={<OpiskeluIcon color="gray" />}
          >
            <TertiaryHeading>
              {nimi} {range}
            </TertiaryHeading>
            <ul>
              <li>{getLocalized(opiskeluoikeus.oppilaitos.nimi)}</li>
              {opiskeluoikeus.ryhmä && (
                <li>
                  <T id="oppija__ryhma" />: {opiskeluoikeus.ryhmä}
                </li>
              )}
            </ul>
          </IconSection>
        )
      })}
    </>
  )
}

const koodistonimi = (k: KoodistoKoodiviite<string, string>): string =>
  k.nimi ? getLocalized(k.nimi) : k.koodiarvo

const yearRangeString = (a?: ISODate, b?: ISODate): string =>
  a || b ? [yearString(a), "–", yearString(b)].filter((s) => !!s).join(" ") : ""

const yearString = (date?: ISODate): string | undefined =>
  date && parseYear(date).toString()
