const fs = require('fs')

if (process.argv.length !== 5)
  throw new Error('ERROR: incorrect number of arguments!' + "\n" + 'Usage: node "2.convert.js" <json_file> <bash_file> <cmd_file>')

const json_file = process.argv[2]
const bash_file = process.argv[3]
const cmd_file  = process.argv[4]

const media = JSON.parse(
  fs.readFileSync(json_file, {encoding: 'utf8'})
)

const urls = []

if (Array.isArray(media)) {
  for (let named_media of media) {

    if (named_media && (named_media instanceof Object) && Array.isArray(named_media.samples)) {
      for (let sample of named_media.samples) {

        if (sample && (sample instanceof Object) && sample.uri && !urls[sample.uri] && sample.drm_scheme && sample.drm_license_uri) {
          urls.push(sample)
        }
      }
    }
  }
}

const bash_curls = urls.map(sample => `curl --silent -X POST -H "Content-Type: text/parameters" --data-binary "Content-Location: ${sample.uri}\\nDRM-License-Scheme: ${sample.drm_scheme}\\nDRM-License-Server: ${sample.drm_license_uri}" "http://\${airplay_ip}/queue"`)
const cmd_curls  = urls.map(sample => `curl --silent -X POST -H "Content-Type: text/parameters" --data-binary "Content-Location: ${sample.uri}\\nDRM-License-Scheme: ${sample.drm_scheme}\\nDRM-License-Server: ${sample.drm_license_uri}" "http://%airplay_ip%/queue"`)

//tweak
if (bash_curls.length) bash_curls[0] = bash_curls[0].replace(/queue"$/, 'play"')
if ( cmd_curls.length)  cmd_curls[0] =  cmd_curls[0].replace(/queue"$/, 'play"')

const bash = `#!/usr/bin/env bash

airplay_ip='192.168.1.100:8192'

${bash_curls.join("\n")}
`

const cmd=`@echo off

set airplay_ip=192.168.1.100:8192

${cmd_curls.join("\n")}
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
