import React, { useEffect } from "react"
import { Redirect } from "react-router-dom"
import { runningLocally } from "../utils/environment"
import { getSearchQueryEntries } from "./searchQuery"

export type Feature = "dummy" | "kuntailmoitusMitätöinti"

const disabledFeatures: string[] = getSearchQueryEntries().reduce(
  (acc: string[], entry) => {
    const match = entry[0].match(/disable-(\w+)/)
    return match && match[1] ? [...acc, match[1]] : acc
  },
  [],
)

export const featureFlags: Record<Feature, string> = {
  dummy: "dummy",
  kuntailmoitusMitätöinti: "valpas-kuntailmoitus-mitatointi",
}

const featureFlagEnabledValue = "enabled"

export type FeatureFlagEnablerProps = {
  features: Feature[]
  redirectTo: string
}

export const FeatureFlagEnabler = (props: FeatureFlagEnablerProps) => {
  useEffect(() => {
    props.features.map(enableFeature)
  })

  return <Redirect to={props.redirectTo} />
}

export const isFeatureFlagEnabled = (feature: Feature) =>
  runningLocally() ||
  (window.localStorage.getItem(featureFlags[feature]) ===
    featureFlagEnabledValue &&
    !disabledFeatures.includes(feature))

export const enableFeature = (feature: Feature) => {
  window.localStorage.setItem(featureFlags[feature], featureFlagEnabledValue)
}
