import { isString } from 'fp-ts/string'
import React from 'react'
import { filterObjByKey } from '../util/fp/objects'

export type CommonProps<T extends object = object> = T & {
  className?: string
  style?: React.CSSProperties
  testId?: string
  role?: string
}

export type CommonPropsWithChildren<T extends object = object> =
  React.PropsWithChildren<CommonProps<T>>

export type MaybeClassName = string | undefined | null | false | 0

export const cx = (...args: MaybeClassName[]): string =>
  args.filter(isString).join(' ')

export const common = <T extends object>(
  props: CommonProps<T>,
  classNames: MaybeClassName[] = []
): object => ({
  style: props.style,
  className: cx(props.className, ...classNames),
  role: props.role,
  ...filterObjByKey(startsWith('aria-', 'data-'))(props)
})

export const subTestId = <T extends object>(
  props: CommonProps<T>,
  subId: string
) => {
  if (subId === '') {
    throw new Error('subId must not be empty')
  }
  if (props.testId === undefined) {
    console.warn(`props.testId undefined, trying to add subId ${subId}`)
  }
  return props.testId && `${props.testId}.${subId}`
}

export const testId = <T extends object>(
  props: CommonProps<T>,
  subId?: string
): object | undefined =>
  props.testId
    ? {
        'data-testid': subId ? `${props.testId}.${subId}` : props.testId
      }
    : undefined

export const rest = <T extends object>({
  testId: _,
  style,
  className,
  role,
  ...restOfProps
}: CommonProps<T>) => doesNotStartWith('aria-', 'data-')(restOfProps)

export const startsWith =
  (...searchStrings: string[]) =>
  (input: any): boolean =>
    typeof input === 'string' && searchStrings.some((s) => input.startsWith(s))

export const doesNotStartWith =
  (...searchStrings: string[]) =>
  (input: any): boolean =>
    !startsWith(...searchStrings)(input)
