import React, { useCallback } from 'react'
import { CommonProps, cx, common } from '../CommonProps'

export type NumberFieldProps = CommonProps<{
  value?: number
  onChange: (text: number) => void
  hasErrors?: boolean
}>

export const NumberField: React.FC<NumberFieldProps> = (props) => {
  const onChange: React.ChangeEventHandler<HTMLInputElement> = useCallback(
    (event) => {
      props.onChange(parseFloat(event.target.value))
    },
    [props.onChange]
  )
  return (
    <div {...common(props, ['NumberField'])}>
      <input
        className={cx(
          'NumberField__input',
          props.hasErrors && 'NumberField__input--error'
        )}
        type="number"
        value={props.value}
        onChange={onChange}
      />
    </div>
  )
}
