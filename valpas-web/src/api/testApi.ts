import { apiGet } from "./apiFetch"

export const resetMockData = async () =>
  apiGet<string>("valpas/test/reset-mock-data")

export const resetMockData2 = async (tarkasteluPäivä: string) =>
  apiGet<string>("valpas/test/reset-mock-data/" + tarkasteluPäivä)

export const clearMockData = async () =>
  apiGet<string>("valpas/test/clear-mock-data")
