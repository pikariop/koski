import bem from "bem-ts"
import React, { useMemo, useState } from "react"
import { useHistory } from "react-router"
import {
  Card,
  CardHeader,
  ConstrainedCardBody,
} from "../../components/containers/cards"
import { Page } from "../../components/containers/Page"
import { Spinner } from "../../components/icons/Spinner"
import {
  getOrganisaatiot,
  OrganisaatioValitsin,
} from "../../components/shared/OrganisaatioValitsin"
import { DataTableCountChangeEvent } from "../../components/tables/DataTable"
import { Counter } from "../../components/typography/Counter"
import { ApiErrors } from "../../components/typography/error"
import { t, T } from "../../i18n/i18n"
import {
  useOrganisaatiotJaKäyttöoikeusroolit,
  withRequiresHakeutumisenValvonta,
} from "../../state/accessRights"
import { Oid } from "../../state/common"
import {
  createHakutilannePathWithOrg,
  HakutilanneViewRouteProps,
} from "../../state/paths"
import { useBoundingClientRect } from "../../state/useBoundingClientRect"
import { nonNull } from "../../utils/arrays"
import { ErrorView } from "../ErrorView"
import { OrganisaatioAutoRedirect } from "../OrganisaatioAutoRedirect"
import { HakutilanneDrawer } from "./HakutilanneDrawer"
import { HakutilanneNavigation } from "./HakutilanneNavigation"
import { HakutilanneTable } from "./HakutilanneTable"
import "./HakutilanneView.less"
import { useOppijatData } from "./useOppijatData"

const b = bem("hakutilanneview")

const organisaatioTyyppi = "OPPILAITOS"
const organisaatioHakuRooli = "OPPILAITOS_HAKEUTUMINEN"

export type HakutilanneViewProps = HakutilanneViewRouteProps

export const HakutilanneViewWithoutOrgOid = withRequiresHakeutumisenValvonta(
  () => (
    <OrganisaatioAutoRedirect
      organisaatioHakuRooli={organisaatioHakuRooli}
      organisaatioTyyppi={organisaatioTyyppi}
      redirectTo={(basePath, organisaatioOid) =>
        createHakutilannePathWithOrg(basePath, {
          organisaatioOid,
        })
      }
      renderError={() => <OrganisaatioMissingView />}
    />
  )
)

export const HakutilanneView = withRequiresHakeutumisenValvonta(
  (props: HakutilanneViewProps) => {
    const history = useHistory()
    const organisaatiotJaKäyttöoikeusroolit = useOrganisaatiotJaKäyttöoikeusroolit()
    const organisaatiot = useMemo(
      () =>
        getOrganisaatiot(
          organisaatiotJaKäyttöoikeusroolit,
          organisaatioHakuRooli,
          organisaatioTyyppi
        ),
      [organisaatiotJaKäyttöoikeusroolit]
    )

    const organisaatioOid = props.match.params.organisaatioOid!

    const { data, isLoading, errors, setMuuHaku } = useOppijatData(
      organisaatioOid
    )

    const [counters, setCounters] = useState<DataTableCountChangeEvent>({
      filteredRowCount: 0,
      unfilteredRowCount: 0,
    })
    const [selectedOppijaOids, setSelectedOppijaOids] = useState<Oid[]>([])

    const organisaatio = organisaatiot.find((o) => o.oid === organisaatioOid)

    const drawerRect = useBoundingClientRect()

    const changeOrganisaatio = (oid?: Oid) => {
      if (oid) {
        history.push(oid)
      }
    }

    const selectedOppijat = useMemo(
      () =>
        data
          ? selectedOppijaOids
              .map((oid) => data.find((o) => o.oppija.henkilö.oid === oid))
              .filter(nonNull)
          : [],
      [data, selectedOppijaOids]
    )

    return organisaatioOid ? (
      <Page className={b("view")}>
        <OrganisaatioValitsin
          organisaatioTyyppi={organisaatioTyyppi}
          organisaatioHierarkia={organisaatiot}
          valittuOrganisaatioOid={organisaatioOid}
          label={t("Oppilaitos")}
          onChange={changeOrganisaatio}
        />
        <HakutilanneNavigation selectedOrganisaatio={organisaatioOid} />
        <Card>
          <CardHeader>
            <T id="hakutilannenäkymä__otsikko" />
            {data && (
              <Counter>
                {counters.filteredRowCount === counters.unfilteredRowCount
                  ? counters.filteredRowCount
                  : `${counters.filteredRowCount} / ${counters.unfilteredRowCount}`}
              </Counter>
            )}
          </CardHeader>
          <ConstrainedCardBody extraMargin={drawerRect.rect?.height}>
            {isLoading && <Spinner />}
            {data && (
              <HakutilanneTable
                data={data}
                organisaatioOid={organisaatioOid}
                onCountChange={setCounters}
                onSelect={setSelectedOppijaOids}
                onSetMuuHaku={setMuuHaku}
              />
            )}
            {errors !== undefined && <ApiErrors errors={errors} />}
          </ConstrainedCardBody>
        </Card>
        {organisaatio && (
          <HakutilanneDrawer
            ref={drawerRect.ref}
            selectedOppijat={selectedOppijat}
            tekijäorganisaatio={organisaatio}
          />
        )}
      </Page>
    ) : (
      <OrganisaatioMissingView />
    )
  }
)

const OrganisaatioMissingView = () => (
  <ErrorView
    title={t("hakutilanne__ei_oikeuksia_title")}
    message={t("hakutilanne__ei_oikeuksia_teksti")}
  />
)
