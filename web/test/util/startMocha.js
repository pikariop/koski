console.log('Starting mocha, foo')
var runner = mocha.run()
if (window.callPhantom) {
  console.log("PhantomJS mode")
}

// For fi.oph.koski.browserstack.BrowserstackMochaTest
runner.errors = []
function mapError(e) {
  var logEntry = {title: e.title}
  if (e.err) logEntry.message = e.err.message
  if (e.parent) logEntry.parent = mapError(e.parent)
  return logEntry
}
runner.on("fail", function(e) { runner.errors.push(mapError(e)) })

runner.on("fail", function(e) { takeScreenshot(e.title)() })

$(document).keyup(function(e) {
  if (e.keyCode === 27) {
    runner.abort()
  }
})
