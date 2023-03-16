import React, { useCallback, useMemo, useState } from 'react'
import {
  ApiMethodStateSuccess,
  isError,
  useApiMethod,
  useApiWithParams,
  useOnApiError,
  useOnApiSuccess,
  useSafeState
} from '../../api-fetch'
import { formatFinnishDateTime, ISO2FinnishDateTime } from '../../date/date'
import { t, tTemplate } from '../../i18n/i18n'
import { isYtrCertificateBlocked } from '../../types/fi/oph/koski/ytr/YtrCertificateBlocked'
import { isYtrCertificateCompleted } from '../../types/fi/oph/koski/ytr/YtrCertificateCompleted'
import { isYtrCertificateInProgress } from '../../types/fi/oph/koski/ytr/YtrCertificateInProgress'
import { isYtrCertificateInternalError } from '../../types/fi/oph/koski/ytr/YtrCertificateInternalError'
import { isYtrCertificateNotStarted } from '../../types/fi/oph/koski/ytr/YtrCertificateNotStarted'
import { isYtrCertificateOldExamination } from '../../types/fi/oph/koski/ytr/YtrCertificateOldExamination'
import { YtrCertificateResponse } from '../../types/fi/oph/koski/ytr/YtrCertificateResponse'
import { isYtrCertificateServiceUnavailable } from '../../types/fi/oph/koski/ytr/YtrCertificateServiceUnavailable'
import { isYtrCertificateTimeout } from '../../types/fi/oph/koski/ytr/YtrCertificateTimeout'
import { fetchYoTodistusState, generateYoTodistus } from '../../util/koskiApi'
import { useInterval } from '../../util/useInterval'
import { common, CommonProps } from '../CommonProps'
import { RaisedButton } from '../controls/RaisedButton'
import { OptionList, Select, SelectOption } from '../controls/Select'
import { Spinner } from '../texts/Spinner'
import { Trans } from '../texts/Trans'

export type YoTodistusProps = CommonProps<{
  oppijaOid: string
}>

export const YoTodistus: React.FC<YoTodistusProps> = (props) => {
  const [language, setLanguage] = useState<YoTodistusLangage>('fi')

  const stateFetch = useApiWithParams(fetchYoTodistusState, [
    props.oppijaOid,
    language
  ])

  const pollState = useCallback(
    () => stateFetch.call(props.oppijaOid, language),
    [language, props.oppijaOid, stateFetch]
  )

  const statePoller = useInterval(pollState, 2000)

  const generate = useApiMethod(generateYoTodistus)

  const [generatingInitiated, setGeneratingInitiated] = useSafeState(false)

  const startGenerating = useCallback(() => {
    statePoller.start()
    setGeneratingInitiated(true)
    return generate.call(props.oppijaOid, language)
  }, [generate, language, props.oppijaOid, setGeneratingInitiated, statePoller])

  const [state, setState] = useSafeState<YtrCertificateResponse | null>(null)
  const updateStateFromResponse = useCallback(
    (response: ApiMethodStateSuccess<YtrCertificateResponse>) => {
      setState(response.data)
      if (!isYtrCertificateInProgress(response.data)) {
        statePoller.stop()
      }
    },
    [setState, statePoller]
  )
  useOnApiSuccess(stateFetch, updateStateFromResponse)
  useOnApiError(stateFetch, () => {
    statePoller.stop()
    setState(null)
  })
  useOnApiSuccess(generate, () => stateFetch.call(props.oppijaOid, language))

  const blockingErrorText = useMemo(() => {
    if (isYtrCertificateBlocked(state))
      return t(
        'Todistuksen lataaminen on estetty. Syynä voi olla esimerkiksi maksamaton tutkintomaksu.'
      )
    if (isYtrCertificateOldExamination(state))
      return t(
        'Todistus ei ole ladattavissa, sillä tutkinto on aloitettu ennen kevättä 2008.'
      )
  }, [state])

  const errorText = useMemo(() => {
    if (isYtrCertificateTimeout(state))
      return tTemplate(
        '{{time}} aloitettu todistuksen luonti epäonnistui palvelun ruuhkautumisen takia.',
        { time: ISO2FinnishDateTime(state.requestedTime) }
      )
    if (
      isYtrCertificateInternalError(state) ||
      isYtrCertificateServiceUnavailable(state)
    )
      return tTemplate(
        '{{time}} aloitettu todistuksen luonti epäonnistui teknisen ongelman takia. Jos ongelma jatkuu, ota yhteyttä YTL:ään.',
        { time: ISO2FinnishDateTime(state.requestedTime) }
      )
    if (isError(generate) || isError(stateFetch)) {
      return t(
        'Tapahtui odottamaton tekninen ongelma. Jos ongelma jatkuu, ota yhteyttä KOSKI-tiimiin.'
      )
    }
    return null
  }, [generate, state, stateFetch])

  return (
    <>
      <div
        {...common(props, [
          'YoTodistus',
          blockingErrorText && 'YoTodistus--notAvailable'
        ])}
      >
        <span className="YoTodistus__title">{'Ylioppilastodistus'}</span>
        {blockingErrorText ? (
          <span className="YoTodistus__blocked">{blockingErrorText}</span>
        ) : (
          <>
            {!isYtrCertificateInProgress(state) && (
              <LanguageSelect value={language} onChange={setLanguage} />
            )}
            {!isYtrCertificateInProgress(state) &&
              !isYtrCertificateCompleted(state) && (
                <>
                  <RaisedButton onClick={startGenerating}>
                    {'Lataa todistus'}
                  </RaisedButton>
                </>
              )}
            {isYtrCertificateInProgress(state) && (
              <>
                <Spinner inline compact />
                <Trans>{'Luodaan tiedostoa...'}</Trans>
              </>
            )}
            {isYtrCertificateCompleted(state) && (
              <a
                href={todistusUrl(props.oppijaOid, language)}
                target="_blank"
                rel="noreferrer"
              >
                <Trans>{'Näytä todistus'}</Trans>
              </a>
            )}
          </>
        )}
      </div>
      {errorText && <div className="YoTodistus__error">{errorText}</div>}
    </>
  )
}

const todistusUrl = (
  oppijaOid: string,
  language: YoTodistusLangage
): string => {
  return `/koski/api/yotodistus/download/${language}/${oppijaOid}/yo-todistus-${language}.pdf`
}

type YoTodistusLangage = 'fi' | 'sv' | 'en'

type LanguageSelectProps = {
  value: YoTodistusLangage
  onChange: (lang: YoTodistusLangage) => void
}

const yoTodistusLanguages: OptionList<YoTodistusLangage> = [
  { key: 'fi', value: 'fi', label: t('Suomi') },
  { key: 'sv', value: 'sv', label: t('Ruotsi') },
  { key: 'en', value: 'en', label: t('Englanti') }
]

const LanguageSelect: React.FC<LanguageSelectProps> = (props) => {
  const onChange = useCallback(
    (option?: SelectOption<YoTodistusLangage>) => {
      if (option?.value) props.onChange(option.value)
    },
    [props]
  )

  return (
    <div className="YoTodistus__lang">
      <div className="YoTodistus__langLabel">
        <Trans>{'Todistuksen kieli'}</Trans>
      </div>
      {':'}
      <Select
        className="YoTodistus__langSelect"
        options={yoTodistusLanguages}
        value={props.value}
        onChange={onChange}
      />
    </div>
  )
}
