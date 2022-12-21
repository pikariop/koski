import * as E from 'fp-ts/Either'
import { pipe } from 'fp-ts/lib/function'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import { Constraint } from '../types/fi/oph/koski/typemodel/Constraint'
import { fetchConstraint } from '../util/koskiApi'

const Loading = Symbol('loading')

export type ConstraintsRecord = Record<string, Constraint | typeof Loading>

export type ConstraintsContextValue = {
  readonly constraints: ConstraintsRecord
  readonly loadConstraint: (schemaClass: string) => void
}

const ConstraintsContext = React.createContext<ConstraintsContextValue>({
  constraints: {},
  loadConstraint: () => {}
})

export type ConstraintsProviderProps = {
  children: React.ReactNode
}

class ConstraintsLoader {
  constraints: ConstraintsRecord = {}

  async loadConstraint(schemaClass: string): Promise<boolean> {
    if (!this.constraints[schemaClass]) {
      this.constraints[schemaClass] = Loading

      pipe(
        await fetchConstraint(schemaClass),
        E.map((response) => {
          this.constraints = {
            ...this.constraints,
            [schemaClass]: response.data
          }
        })
      )
      return true
    }
    return false
  }
}

const constraintsLoaderSingleton = new ConstraintsLoader()

export const ConstraintsProvider = (props: ConstraintsProviderProps) => {
  const [constraints, setConstraints] = useState<ConstraintsRecord>({})

  const loadConstraint = useCallback(async (schemaClass: string) => {
    if (await constraintsLoaderSingleton.loadConstraint(schemaClass)) {
      setConstraints(constraintsLoaderSingleton.constraints)
    }
  }, [])

  const providedValue: ConstraintsContextValue = useMemo(
    () => ({ constraints, loadConstraint }),
    [constraints, loadConstraint]
  )

  return (
    <ConstraintsContext.Provider value={providedValue}>
      {props.children}
    </ConstraintsContext.Provider>
  )
}

export const useConstraint = (
  schemaClass: string | null
): Constraint | null => {
  const context = useContext(ConstraintsContext)
  useEffect(() => {
    if (schemaClass) {
      context.loadConstraint(schemaClass)
    }
  }, [schemaClass])
  const constraint = schemaClass && context.constraints[schemaClass]
  return typeof constraint === 'object' ? constraint : null
}
