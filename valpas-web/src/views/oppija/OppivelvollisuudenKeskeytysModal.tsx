import bem from "bem-ts"
import { isNonEmpty } from "fp-ts/lib/Array"
import React, { useCallback, useState } from "react"
import { createOppivelvollisuudenKeskeytys } from "../../api/api"
import { ApiError } from "../../api/apiFetch"
import { useApiMethod, useOnApiSuccess } from "../../api/apiHooks"
import { isError } from "../../api/apiUtils"
import { RaisedButton } from "../../components/buttons/RaisedButton"
import { Modal } from "../../components/containers/Modal"
import { LabeledCheckbox } from "../../components/forms/Checkbox"
import { DatePicker } from "../../components/forms/DatePicker"
import {
  DateRange,
  DateRangePicker,
} from "../../components/forms/DateRangePicker"
import {
  Dropdown,
  organisaatiotToOptions,
} from "../../components/forms/Dropdown"
import { RadioButton } from "../../components/forms/RadioButton"
import { ApiErrors } from "../../components/typography/error"
import { SecondaryHeading } from "../../components/typography/headings"
import { T, t } from "../../i18n/i18n"
import {
  kuntavalvontaAllowed,
  useOrganisaatiotOfRole,
} from "../../state/accessRights"
import { HenkilöLaajatTiedot } from "../../state/apitypes/henkilo"
import { Organisaatio } from "../../state/apitypes/organisaatiot"
import { ISODate, Oid } from "../../state/common"
import { today } from "../../utils/date"
import "./OppivelvollisuudenKeskeytysModal.less"

const b = bem("ovkeskeytys")

export type OppivelvollisuudenKeskeytysModalProps = {
  henkilö: HenkilöLaajatTiedot
  onClose: () => void
  onSubmit: () => void
}

export const OppivelvollisuudenKeskeytysModal = (
  props: OppivelvollisuudenKeskeytysModalProps
) => {
  const organisaatiot = useOrganisaatiotOfRole(kuntavalvontaAllowed)
  const create = useApiMethod(createOppivelvollisuudenKeskeytys)
  const submit = useCallback(
    (form: OppivelvollisuudenKeskeytysFormValues) => {
      create.call({
        ...form,
        oppijaOid: props.henkilö.oid,
      })
    },
    [create, props.henkilö.oid]
  )

  useOnApiSuccess(create, props.onSubmit)

  return (
    <Modal title={t("ovkeskeytys__otsikko")} onClose={props.onClose}>
      <SecondaryHeading>
        {props.henkilö.sukunimi} {props.henkilö.etunimet}
        {props.henkilö.hetu && ` (${props.henkilö.hetu})`}
      </SecondaryHeading>
      <OppivelvollisuudenKeskeytysForm
        organisaatiot={organisaatiot}
        onSubmit={submit}
        errors={isError(create) ? create.errors : []}
      />
    </Modal>
  )
}

// Lomake

type OppivelvollisuudenKeskeytysFormProps = {
  organisaatiot: Organisaatio[]
  onSubmit: (aikaväli: OppivelvollisuudenKeskeytysFormValues) => void
  errors: ApiError[]
}

type OppivelvollisuudenKeskeytysFormValues = {
  alku: ISODate
  loppu?: ISODate
  tekijäOrganisaatioOid: Oid
}

type Aikavalinta = "määräaikainen" | "toistaiseksi"

const OppivelvollisuudenKeskeytysForm = (
  props: OppivelvollisuudenKeskeytysFormProps
) => {
  const [aikavalinta, setAikavalinta] = useState<Aikavalinta>("määräaikainen")
  const [toistaiseksiVahvistettu, setToistaiseksiVahvistettu] = useState(false)
  const [dateRange, setDateRange] = useState<DateRange>([today(), null])
  const [organisaatio, setOrganisaatio] = useState<Oid | undefined>(
    props.organisaatiot[0]?.oid
  )

  const määräaikainenSelected = aikavalinta === "määräaikainen"
  const toistaiseksiSelected = aikavalinta === "toistaiseksi"
  const isOk =
    organisaatio !== undefined &&
    (määräaikainenSelected
      ? dateRange.every((d) => d != null)
      : toistaiseksiVahvistettu)

  const { onSubmit } = props
  const submit = useCallback(() => {
    if (
      määräaikainenSelected &&
      dateRange[0] !== null &&
      dateRange[1] !== null
    ) {
      onSubmit({
        alku: dateRange[0],
        loppu: dateRange[1],
        tekijäOrganisaatioOid: organisaatio!,
      })
    } else if (
      toistaiseksiSelected &&
      dateRange[0] &&
      toistaiseksiVahvistettu
    ) {
      onSubmit({
        alku: dateRange[0],
        tekijäOrganisaatioOid: organisaatio!,
      })
    }
  }, [
    dateRange,
    määräaikainenSelected,
    onSubmit,
    organisaatio,
    toistaiseksiSelected,
    toistaiseksiVahvistettu,
  ])

  return (
    <section className={b()}>
      {props.organisaatiot.length !== 1 ? (
        <Dropdown
          label={t("ovkeskeytys__organisaatio")}
          options={organisaatiotToOptions(props.organisaatiot)}
          value={organisaatio}
          onChange={setOrganisaatio}
          testId="organisaatio"
        />
      ) : null}

      <OppivelvollisuudenKeskeytysOption
        selected={määräaikainenSelected}
        onSelect={() => setAikavalinta("määräaikainen")}
        label={t("ovkeskeytys__keskeytys_määräajalle")}
      >
        <DateRangePicker
          value={dateRange}
          onChange={setDateRange}
          disabled={!määräaikainenSelected}
        />
      </OppivelvollisuudenKeskeytysOption>

      <OppivelvollisuudenKeskeytysOption
        selected={toistaiseksiSelected}
        onSelect={() => setAikavalinta("toistaiseksi")}
        label={t("ovkeskeytys__keskeytys_toistaiseksi")}
      >
        <DatePicker
          value={dateRange[0]}
          onChange={(startDate) => setDateRange([startDate, dateRange[1]])}
          disabled={!toistaiseksiSelected}
        />
        <LabeledCheckbox
          label={t("ovkeskeytys__keskeytys_toistaiseksi_vahvistus")}
          value={toistaiseksiVahvistettu}
          onChange={setToistaiseksiVahvistettu}
          disabled={!toistaiseksiSelected}
          className={b("confirmcb")}
        />
      </OppivelvollisuudenKeskeytysOption>

      {isNonEmpty(props.errors) && <ApiErrors errors={props.errors} />}

      <RaisedButton
        id="ovkeskeytys-submit"
        className={b("submit")}
        onClick={submit}
        disabled={!isOk}
      >
        <T id="ovkeskeytys__keskeytä_oppivelvollisuus_nappi" />
      </RaisedButton>
    </section>
  )
}

type OppivelvollisuudenKeskeytysOptionProps = {
  selected: boolean
  onSelect: () => void
  label: string
  children: React.ReactNode
}

const OppivelvollisuudenKeskeytysOption = (
  props: OppivelvollisuudenKeskeytysOptionProps
) => (
  <div className={b("option")}>
    <RadioButton
      selected={props.selected}
      onChange={(checked) => checked && props.onSelect()}
    >
      {props.label}
    </RadioButton>
    <div className={b("optionform")}>{props.children}</div>
  </div>
)
