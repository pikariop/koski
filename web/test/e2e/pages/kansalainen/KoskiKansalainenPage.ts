import { Page } from '@playwright/test'
import { expect } from '../../base'

export class KoskiKansalainenPage {
  constructor(private readonly page: Page) {}

  static create(page: Page) {
    return new this(page)
  }

  async goto() {
    await this.page.goto(`/koski/omattiedot`)
    await expect(this.page).toHaveURL(/\/koski\/omattiedot$/)
  }
}
