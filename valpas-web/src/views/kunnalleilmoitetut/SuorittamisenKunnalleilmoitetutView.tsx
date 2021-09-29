import React from "react"
import { t } from "../../i18n/i18n"
import { withRequiresSuorittamisenValvonta } from "../../state/accessRights"
import {
  createSuorittamisvalvonnanKunnalleIlmoitetutPathWithOrg,
  KunnalleIlmoitetutViewRouteProps,
} from "../../state/paths"
import { ErrorView } from "../ErrorView"
import { useSuorittamisvalvonnanKunnalleTehdytIlmoitukset } from "../hakutilanne/useOppijatData"
import { OrganisaatioAutoRedirect } from "../OrganisaatioAutoRedirect"
import { SuorittaminenNavigation } from "../suorittaminen/SuorittaminenNavigation"
import { KunnalleIlmoitetutView } from "./KunnalleIlmoitetutView"

const organisaatioTyyppi = "OPPILAITOS"
const organisaatioHakuRooli = "OPPILAITOS_SUORITTAMINEN"

export const SuorittamisenKunnalleIlmoitetutViewWithoutOrgOid = withRequiresSuorittamisenValvonta(
  () => (
    <OrganisaatioAutoRedirect
      organisaatioHakuRooli={organisaatioHakuRooli}
      organisaatioTyyppi={organisaatioTyyppi}
      redirectTo={(basePath, organisaatioOid) =>
        createSuorittamisvalvonnanKunnalleIlmoitetutPathWithOrg(basePath, {
          organisaatioOid,
        })
      }
      renderError={() => <OrganisaatioMissingView />}
    />
  )
)

export type SuorittamisenKunnalleIlmoitetutViewProps = KunnalleIlmoitetutViewRouteProps

export const SuorittamisenKunnalleIlmoitetutView = withRequiresSuorittamisenValvonta(
  (props: SuorittamisenKunnalleIlmoitetutViewProps) => (
    <KunnalleIlmoitetutView
      organisaatioOid={props.match.params.organisaatioOid!}
      organisaatioHakuRooli={organisaatioHakuRooli}
      organisaatioTyyppi={organisaatioTyyppi}
      dataFetcher={useSuorittamisvalvonnanKunnalleTehdytIlmoitukset}
      backRefName="suorittaminenIlmoitetutRef"
      storageName="suorittaminenIlmoitetut"
      navigation={
        <SuorittaminenNavigation
          selectedOrganisaatio={props.match.params.organisaatioOid!}
        />
      }
    />
  )
)

const OrganisaatioMissingView = () => (
  <ErrorView
    title={t("hakutilanne__ei_oikeuksia_title")}
    message={t("hakutilanne__ei_oikeuksia_teksti")}
  />
)