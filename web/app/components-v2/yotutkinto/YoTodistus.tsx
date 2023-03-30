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
import { ISO2FinnishDateTime } from '../../date/date'
import { t, tTemplate } from '../../i18n/i18n'
import { isYtrCertificateBlocked } from '../../types/fi/oph/koski/ytr/YtrCertificateBlocked'
import { isYtrCertificateCompleted } from '../../types/fi/oph/koski/ytr/YtrCertificateCompleted'
import { isYtrCertificateInProgress } from '../../types/fi/oph/koski/ytr/YtrCertificateInProgress'
import { isYtrCertificateInternalError } from '../../types/fi/oph/koski/ytr/YtrCertificateInternalError'
import { isYtrCertificateOldExamination } from '../../types/fi/oph/koski/ytr/YtrCertificateOldExamination'
import { YtrCertificateResponse } from '../../types/fi/oph/koski/ytr/YtrCertificateResponse'
import { isYtrCertificateServiceUnavailable } from '../../types/fi/oph/koski/ytr/YtrCertificateServiceUnavailable'
import { isYtrCertificateTimeout } from '../../types/fi/oph/koski/ytr/YtrCertificateTimeout'
import { fetchYoTodistusState, generateYoTodistus } from '../../util/koskiApi'
import { useInterval } from '../../util/useInterval'
import { common, CommonProps, subTestId, testId } from '../CommonProps'
import { RaisedButton } from '../controls/RaisedButton'
import { OptionList, Select, SelectOption } from '../controls/Select'
import { Spinner } from '../texts/Spinner'
import { TextWithLinks } from '../texts/TextWithLinks'
import { Trans } from '../texts/Trans'

export type YoTodistusProps = CommonProps<{
  oppijaOid: string
}>

export const YoTodistus: React.FC<YoTodistusProps> = (props) => {
  const [language, setLanguage] = useState<YoTodistusLanguage>('fi')

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

  const startGenerating = useCallback(async () => {
    await generate.call(props.oppijaOid, language)
    await pollState()
    statePoller.start()
  }, [generate, language, pollState, props.oppijaOid, statePoller])

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
    if (isYtrCertificateBlocked(state)) return t('yotodistus-error: blocked')
    if (isYtrCertificateOldExamination(state))
      return t('yotodistus-error: old examination')
  }, [state])

  const errorText = useMemo(() => {
    if (isYtrCertificateTimeout(state))
      return tTemplate('yotodistus-error: timeout', {
        time: ISO2FinnishDateTime(state.requestedTime)
      })
    if (
      isYtrCertificateInternalError(state) ||
      isYtrCertificateServiceUnavailable(state)
    )
      return t('yotodistus-error: ytr error')
    if (isError(generate) || isError(stateFetch)) {
      return t('yotodistus-error: koski error')
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
          <TextWithLinks
            className="YoTodistus__blocked"
            {...testId(props, 'error')}
          >
            {blockingErrorText}
          </TextWithLinks>
        ) : (
          <>
            {!isYtrCertificateInProgress(state) && (
              <LanguageSelect
                value={language}
                onChange={setLanguage}
                testId={subTestId(props, 'language')}
              />
            )}
            {!isYtrCertificateInProgress(state) &&
              !isYtrCertificateCompleted(state) && (
                <>
                  <RaisedButton
                    onClick={startGenerating}
                    {...testId(props, 'start')}
                  >
                    {'Lataa todistus'}
                  </RaisedButton>
                </>
              )}
            {isYtrCertificateInProgress(state) && (
              <>
                <Spinner inline compact />
                <span {...testId(props, 'loading')}>
                  <Trans>{'Ladataan todistusta...'}</Trans>
                </span>
              </>
            )}
            {isYtrCertificateCompleted(state) && (
              <a
                href={todistusUrl(props.oppijaOid, language)}
                target="_blank"
                rel="noreferrer"
                {...testId(props, 'open')}
              >
                <Trans>{'Näytä todistus'}</Trans>
              </a>
            )}
          </>
        )}
      </div>
      {errorText && (
        <div className="YoTodistus__error" {...testId(props, 'error')}>
          <TextWithLinks>{errorText}</TextWithLinks>
        </div>
      )}
    </>
  )
}

const todistusUrl = (
  oppijaOid: string,
  language: YoTodistusLanguage
): string => {
  return `/koski/api/yotodistus/download/${language}/${oppijaOid}/yo-todistus-${language}.pdf`
}

export type YoTodistusLanguage = 'fi' | 'sv' | 'en'

type LanguageSelectProps = CommonProps<{
  value: YoTodistusLanguage
  onChange: (lang: YoTodistusLanguage) => void
}>

const yoTodistusLanguages: OptionList<YoTodistusLanguage> = [
  { key: 'fi', value: 'fi', label: t('Fi') },
  { key: 'sv', value: 'sv', label: t('Sv') },
  { key: 'en', value: 'en', label: t('En') }
]

const LanguageSelect: React.FC<LanguageSelectProps> = (props) => {
  const onChange = useCallback(
    (option?: SelectOption<YoTodistusLanguage>) => {
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
        testId={props.testId}
      />
    </div>
  )
}
