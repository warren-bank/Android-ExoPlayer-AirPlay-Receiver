#!/usr/bin/env bash

# network address for running instance of 'ExoPlayer AirPlay Receiver'
airplay_ip='192.168.1.100:8192'

video_url='https://github.com/ksk-007/newton-netflix/raw/main/TV%20video.m4v'

audio_url_1='https://github.com/SergLam/Audio-Sample-files/raw/refs/heads/master/sample.m4a'
audio_url_2='https://github.com/SergLam/Audio-Sample-files/raw/refs/heads/master/sample1.m4a'
audio_url_3='https://github.com/SergLam/Audio-Sample-files/raw/refs/heads/master/sample2.m4a'

curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${video_url}\nAudio-Location: ${audio_url_3}" \
  "http://${airplay_ip}/play"

curl --silent \
  "http://${airplay_ip}/media-item-info"

curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Audio-Location: ${audio_url_1}\nAudio-Location: ${audio_url_2}\nAudio-Location: ${audio_url_3}" \
  "http://${airplay_ip}/load-audio-tracks"

curl --silent \
  "http://${airplay_ip}/media-item-info"
