import React, { useCallback, useMemo, useRef, useState } from 'react'
import {
  createPreferLocalCache,
  isSuccess,
  useApiWithParams
} from '../../api-fetch'
import { formatYearRange, ISO2FinnishDateTime } from '../../date/date'
import { t } from '../../i18n/i18n'
import { Opiskeluoikeus } from '../../types/fi/oph/koski/schema/Opiskeluoikeus'
import { last, nonNull } from '../../util/fp/arrays'
import { fetchVersiohistoria } from '../../util/koskiApi'
import { viimeisinOpiskelujaksonTila } from '../../util/schema'
import { uncapitalize } from '../../util/strings'
import { currentQueryWith, parseQuery } from '../../util/url'
import { VirkailijaOnly } from '../access/VirkailijaOnly'
import { common, CommonProps, cx, subTestId } from '../CommonProps'
import { Column, ColumnRow } from '../containers/Columns'
import { PositionalPopup } from '../containers/PositionalPopup'
import { FlatButton } from '../controls/FlatButton'
import { LinkButton } from '../controls/LinkButton'
import { Lowercase } from '../texts/Lowercase'
import { Trans } from '../texts/Trans'

export type OpiskeluoikeusTitleProps = CommonProps<{
  opiskeluoikeus: Opiskeluoikeus
  // Nämä propertyt ylikirjoittavat opiskeluoikeudesta oletuksena tulkittavat arvot:
  oppilaitos?: string
  opiskeluoikeudenNimi?: string
}>

const join = (...as: Array<string | undefined>) => as.filter(nonNull).join(', ')

export const OpiskeluoikeusTitle = (props: OpiskeluoikeusTitleProps) => {
  const oppilaitosJaKoulutus = join(
    props.oppilaitos || t(props.opiskeluoikeus.oppilaitos?.nimi),
    uncapitalize(
      props.opiskeluoikeudenNimi ||
        t(props.opiskeluoikeus.suoritukset[0]?.tyyppi.nimi)
    )
  )
  const aikaväliJaTila = join(
    formatYearRange(
      props.opiskeluoikeus.alkamispäivä,
      props.opiskeluoikeus.päättymispäivä
    ),
    t(viimeisinOpiskelujaksonTila(props.opiskeluoikeus.tila)?.nimi)
  )

  const oid: string | undefined = (props.opiskeluoikeus as any).oid

  return (
    <h3 {...common(props, ['OpiskeluoikeusTitle', 'darkBackground'])}>
      <ColumnRow>
        <Column
          className="OpiskeluoikeusTitle__title"
          span={{ default: 12, small: 24 }}
          testId="opiskeluoikeus.nimi"
        >
          {oppilaitosJaKoulutus} {'('}
          <Lowercase>{aikaväliJaTila}</Lowercase>
          {')'}
        </Column>

        {oid && (
          <Column
            className="OpiskeluoikeusTitle__oid"
            span={{ default: 12, small: 24 }}
            align={{ default: 'right', small: 'left' }}
            testId="opiskeluoikeus.oid"
          >
            <Trans>{'Opiskeluoikeuden oid'}</Trans>
            {': '}
            {oid}
            <VirkailijaOnly>
              <VersiohistoriaButton
                opiskeluoikeusOid={oid}
                testId="opiskeluoikeus.versiohistoria"
              />
            </VirkailijaOnly>
          </Column>
        )}
      </ColumnRow>
    </h3>
  )
}

type VersiohistoriaButtonProps = CommonProps<{
  opiskeluoikeusOid: string
}>

const VersiohistoriaButton: React.FC<VersiohistoriaButtonProps> = (props) => {
  const buttonRef = useRef(null)
  const [versiohistoriaVisible, setVersiohistoriaVisible] = useState(false)
  const toggleList = useCallback(
    () => setVersiohistoriaVisible(!versiohistoriaVisible),
    [versiohistoriaVisible]
  )
  const hideList = useCallback(() => setVersiohistoriaVisible(false), [])

  return (
    <span className="VersiohistoriaButton" ref={buttonRef}>
      <FlatButton
        onClick={toggleList}
        aria-haspopup="menu"
        aria-expanded={versiohistoriaVisible}
        testId={subTestId(props, 'button')}
      >
        {t('Versiohistoria')}
      </FlatButton>
      <PositionalPopup
        align="right"
        onDismiss={hideList}
        open={versiohistoriaVisible}
        parentRef={buttonRef}
      >
        <VersiohistoriaList
          opiskeluoikeusOid={props.opiskeluoikeusOid}
          open={versiohistoriaVisible}
          testId={subTestId(props, 'list')}
        />
      </PositionalPopup>
    </span>
  )
}

type VersiohistoriaListProps = CommonProps<{
  opiskeluoikeusOid: string
  open: boolean
}>

const versiolistaCache = createPreferLocalCache(fetchVersiohistoria)

const VersiohistoriaList: React.FC<VersiohistoriaListProps> = (props) => {
  const historia = useApiWithParams(
    fetchVersiohistoria,
    props.open ? [props.opiskeluoikeusOid] : undefined,
    versiolistaCache
  )

  const currentVersion = useMemo(() => {
    const v = parseQuery(window.location.search).versionumero
    return v
      ? parseInt(v)
      : isSuccess(historia)
      ? last(historia.data)?.versionumero
      : undefined
  }, [historia])

  return isSuccess(historia) ? (
    <ul className="VersiohistoriaList" role="navigation">
      {historia.data.map((versio) => (
        <li
          key={versio.versionumero}
          className={cx(
            'VersiohistoriaList__item',
            currentVersion === versio.versionumero &&
              'VersiohistoriaList__item--current'
          )}
        >
          <LinkButton
            href={currentQueryWith({
              opiskeluoikeus: props.opiskeluoikeusOid,
              versionumero: versio.versionumero
            })}
            testId={subTestId(props, versio.versionumero.toString())}
          >
            {`v${versio.versionumero}`} {ISO2FinnishDateTime(versio.aikaleima)}
          </LinkButton>
        </li>
      ))}
    </ul>
  ) : null
}
