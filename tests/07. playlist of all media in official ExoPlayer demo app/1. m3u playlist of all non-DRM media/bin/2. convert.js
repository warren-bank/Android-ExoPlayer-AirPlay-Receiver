const fs = require('fs')

if (process.argv.length !== 4)
  throw new Error('ERROR: incorrect number of arguments!' + "\n" + 'Usage: node "2.convert.js" <json_file> <m3u_file>')

const json_file = process.argv[2]
const m3u_file  = process.argv[3]

const media = JSON.parse(
  fs.readFileSync(json_file, {encoding: 'utf8'})
)

// use hash to prevent dups
const urls = {}

if (Array.isArray(media)) {
  for (let named_media of media) {

    if (named_media && (named_media instanceof Object) && Array.isArray(named_media.samples)) {
      for (let sample of named_media.samples) {

        if (sample && (sample instanceof Object) && sample.uri && !urls[sample.uri] && !sample.drm_scheme && !sample.drm_license_uri) {
          urls[sample.uri] = true
        }
      }
    }
  }
}

fs.writeFileSync(
  m3u_file,
  Object.keys(urls).join("\n"),
  {encoding: 'utf8'}
)
