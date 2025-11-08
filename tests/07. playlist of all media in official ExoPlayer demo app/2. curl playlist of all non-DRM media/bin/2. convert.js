const fs = require('fs')

if (process.argv.length !== 5)
  throw new Error('ERROR: incorrect number of arguments!' + "\n" + 'Usage: node "2.convert.js" <json_file> <bash_file> <cmd_file>')

const json_file = process.argv[2]
const bash_file = process.argv[3]
const cmd_file  = process.argv[4]

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

const common_curl_data = Object.keys(urls).map(url => `Content-Location: ${url}`).join('\\n')

const bash = `#!/usr/bin/env bash

airplay_ip='192.168.1.100:8192'

curl --silent -X POST -H "Content-Type: text/parameters" --data-binary "${common_curl_data}" "http://\${airplay_ip}/play"
`

const cmd=`@echo off

set airplay_ip=192.168.1.100:8192

curl --silent -X POST -H "Content-Type: text/parameters" --data-binary "${common_curl_data}" "http://%airplay_ip%/play"
`

fs.writeFileSync(
  bash_file,
  bash,
  {encoding: 'utf8'}
)

fs.writeFileSync(
  cmd_file,
  cmd,
  {encoding: 'utf8'}
)
