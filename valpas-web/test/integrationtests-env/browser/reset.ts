import { By, Key, until } from "selenium-webdriver"
import {
  clickElement,
  expectElementEventuallyVisible,
  textEventuallyEquals,
} from "./content"
import { $, deleteCookies, goToLocation } from "./core"
import { driver } from "./driver"
import { getTextInput, setTextInput } from "./forms"
import { defaultTimeout, longTimeout } from "./timeouts"
import { eventually } from "./utils"

export const loginAs = async (
  initialPath: string,
  username: string,
  forceReset: boolean = false,
  tarkastelupäivä?: string
) => {
  await eventually(async () => {
    await reset(initialPath, forceReset, tarkastelupäivä)
    await expectElementEventuallyVisible("#username")
  }, longTimeout)
  ;(await $("#username")).sendKeys(username)
  ;(await $("#password")).sendKeys(username, Key.ENTER)
  await driver.wait(
    until.elementLocated(By.css("article.page:not(#login-app)")),
    defaultTimeout
  )
  await driver.wait(
    until.elementLocated(By.css("article.page")),
    defaultTimeout
  )
}

export const defaultLogin = async (initialPath: string) =>
  loginAs(initialPath, "valpas-helsinki")

export const reset = async (
  initialPath: string,
  force: boolean = false,
  tarkastelupäivä?: string
) => {
  await deleteCookies()
  await goToLocation(initialPath)
  await driver.wait(until.elementLocated(By.css("article")), defaultTimeout)
  await resetMockData(tarkastelupäivä, force)
}

export const resetMockData = async (
  tarkastelupäivä: string = "2021-09-05",
  force: boolean = false
) => {
  const inputSelector = "#tarkastelupäivä"

  await expectElementEventuallyVisible(inputSelector)
  const currentTarkastelupäivä = await getTextInput(inputSelector)
  const currentFixture = await (await $("#current-fixture")).getText()

  if (
    currentTarkastelupäivä !== tarkastelupäivä ||
    currentFixture !== "VALPAS" ||
    force
  ) {
    await setTextInput(inputSelector, tarkastelupäivä)
    await clickElement("#resetMockData")
    await textEventuallyEquals("#resetMockDataState", "success", longTimeout)
  }
}

export const clearMockData = async () => {
  await clickElement("#clearMockData")
  await textEventuallyEquals("#clearMockDataState", "success", longTimeout)
}

export const clearLocalStorage = async () => {
  await driver.executeScript("window.localStorage.clear()")
}

export const clearSessionStorage = async () => {
  await driver.executeScript("window.sessionStorage.clear()")
}
