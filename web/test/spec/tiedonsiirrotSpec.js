describe('Tiedonsiirrot', function() {
  var tiedonsiirrot = TiedonsiirrotPage()
  var authentication = Authentication()


  before(
    authentication.login('stadin-palvelu'),
    resetFixtures,
    insertOppija('<oppija></oppija>'),
    insertOppija('{"henkilö": {}}'),
    insertExample('tiedonsiirto - epäonnistunut.json'),
    insertExample('tiedonsiirto - onnistunut.json'),
    insertExample('tiedonsiirto - epäonnistunut 2.json'),
    insertExample('tiedonsiirto - vain syntymäaika.json'),
    syncTiedonsiirrot,
    tiedonsiirrot.openPage
  )

  describe('Tiedonsiirtoloki', function() {
    function sortByName(a, b) {
      return a[1].localeCompare(b[1])
    }

    it('Näytetään', function() {
      expect(tiedonsiirrot.tiedot().sort(sortByName)).to.deep.equal([
        ['280618-402H', 'Aarne Ammattilainen', 'Aalto-yliopisto', 'virhe', 'tiedot'],
        ['24.2.1977', 'Heikki Hetuton', 'Stadin ammattiopisto', '', ''],
        ['270303-281N', 'Tiina Tiedonsiirto', 'Stadin ammattiopisto', '', ''],
        ['', '', '', 'virhe', 'tiedot']
      ].sort(sortByName))
    })
  })

  describe('Virhelistaus', function() {
    before(tiedonsiirrot.openVirhesivu)

    it('Näytetään', function() {
      expect(tiedonsiirrot.tiedot()).to.deep.equal([
        ['280618-402H', 'Aarne Ammattilainen', 'Aalto-yliopisto', 'Ei oikeuksia organisatioon 1.2.246.562.10.56753942459virhe', 'tiedot'],
        ['', '', '', 'Viesti ei ole skeeman mukainen (notAnyOf henkilö)virhe', 'tiedot']
      ])
    })

    describe('Poistettaessa', function () {
      describe('Aluksi', function () {
        it('Poista valitut nappi on disabloitu', function () {
          expect(tiedonsiirrot.poistaNappiEnabloitu()).to.equal(false)
        })
      })
      describe('Kun valitaan rivi', function() {
        before(tiedonsiirrot.setValinta('tiedonsiirto-1.2.246.562.10.346830761110_', true))

        it('Poista valitut nappi enabloituu', function() {
          expect(tiedonsiirrot.poistaNappiEnabloitu()).to.equal(true)
        })

        describe('Kun valitaan toinen rivi', function() {
          before(tiedonsiirrot.setValinta('tiedonsiirto-1.2.246.562.10.346830761110_280618-402H', true))

          it('Poista valitut nappi on edelleen enabloitu', function() {
            expect(tiedonsiirrot.poistaNappiEnabloitu()).to.equal(true)
          })

          describe('Kun poistetaan toinen rivi', function() {
            before(tiedonsiirrot.setValinta('tiedonsiirto-1.2.246.562.10.346830761110_280618-402H', false))

            it('Poista valitut nappi on edelleen enabloitu', function() {
              expect(tiedonsiirrot.poistaNappiEnabloitu()).to.equal(true)
            })

            describe('Kun poistetaan viimeinen rivi', function() {
              before(tiedonsiirrot.setValinta('tiedonsiirto-1.2.246.562.10.346830761110_', false))

              it('Poista valitut nappi on disabloitu', function() {
                expect(tiedonsiirrot.poistaNappiEnabloitu()).to.equal(false)
              })
            })
          })
        })
      })
      describe('Kun poistetaan valittu rivi', function() {
        before(
          tiedonsiirrot.setValinta('tiedonsiirto-1.2.246.562.10.346830761110_', true),
          tiedonsiirrot.poista
        )
        it('Se poistuu listauksesta', function() {
          expect(tiedonsiirrot.tiedot()).to.deep.equal([
            ['280618-402H', 'Aarne Ammattilainen', 'Aalto-yliopisto', 'Ei oikeuksia organisatioon 1.2.246.562.10.56753942459virhe', 'tiedot']
          ])
        })
        it('Poista valitut nappi on disabloitu', function () {
          expect(tiedonsiirrot.poistaNappiEnabloitu()).to.equal(false)
        })
        describe('Kun valitaan rivi', function() {
          before(tiedonsiirrot.setValinta('tiedonsiirto-1.2.246.562.10.346830761110_280618-402H', false))
          it('Poista valitut nappi enabloituu', function () {
            expect(tiedonsiirrot.poistaNappiEnabloitu()).to.equal(false)
          })
        })
      })
    })
    describe('Poistettaessa useampi kerralla', function() {
      before(
        insertExample('tiedonsiirto - epäonnistunut.json'),
        insertExample('tiedonsiirto - epäonnistunut 2.json'),
        syncTiedonsiirrot,
        tiedonsiirrot.openPage,
        tiedonsiirrot.openVirhesivu
      )
      it('Kaikki valitut rivit poistuvat', function() {

      })
    })
  })

  describe('Yhteenveto', function() {
    before(tiedonsiirrot.openYhteenveto)

    it('Näytetään', function() {
      expect(tiedonsiirrot.tiedot().map(function(row) { return row[0]})).to.deep.equal(['Aalto-yliopisto', 'HELSINGIN KAUPUNKI', 'Stadin ammattiopisto'])
    })
  })


  function insertExample(name) {
    return function() {
      return getJson('/koski/api/documentation/examples/' + name).then(function(data) {
        return putJson('/koski/api/oppija', data).catch(function(){})
      })
    }
  }

  function insertOppija(dataString) {
    return function() {
      return sendAjax('/koski/api/oppija', 'application/json', dataString, 'PUT').catch(function(){})
    }
  }
})
