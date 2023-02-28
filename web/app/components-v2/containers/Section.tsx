import React from 'react'
import { common, CommonPropsWithChildren, testId } from '../CommonProps'

export type SectionProps = CommonPropsWithChildren

export const Section: React.FC<SectionProps> = (props) => (
  <section {...common(props)} {...testId(props)}>
    {props.children}
  </section>
)