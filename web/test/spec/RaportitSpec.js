describe('Raporttien luominen', function() {
  var page = RaportitPage()
  var login = LoginPage()

  describe('Tietoturva', function() {
    before(Authentication().logout, page.openPage())

    it('näytetään login, kun käyttäjä ei ole kirjautunut sisään', function() {
      expect(login.isVisible()).to.equal(true)
    })
  })

  describe('Avataan organisaatiovalitsin', function() {
    before(Authentication().login('kalle'), page.openPage(), page.avaaOrganisaatioValitsin())
    it('Organisaatiohaku avautuu', function () {
      expect(page.organisaatioHakuNäkyvissä()).to.equal(true)
    })
    // Poista kommentit, tää testi takas
    /*describe('Valitaan organisaatio ja luodaan "Esiopetuksen opiskeluoikeus- ja suoritustietojen tarkistusraportti" raportti', function() {
      before(page.valitseOrganisaatio('Jyväskylän normaalikoulu'), page.syötäPäivämäärä('1.1.2007'), page.luoRaportti())
      it('Raportti latautuu', function () {
        expect(page.latausIndikaatioPoistuu()).to.equal(true)
      })
    })*/
  })
})