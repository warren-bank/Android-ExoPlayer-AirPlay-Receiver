#!/usr/bin/env bash

# network address for running instance of 'ExoPlayer AirPlay Receiver'
airplay_ip='192.168.1.100:8192'

# URL for sequentially numbered test videos:
localhost_ip='http://192.168.1.101:80'

# turn shuffle on
curl --silent -X GET \
  "http://${airplay_ip}/shuffle?toggle=1"

# add videos w/ captions to queue

curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${localhost_ip}/sequential_numbers/0.mp4\nCaption-Location: ${localhost_ip}/captions/0.en.srt" \
  "http://${airplay_ip}/queue"

curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${localhost_ip}/sequential_numbers/1.mp4\nCaption-Location: ${localhost_ip}/captions/1.en.srt" \
  "http://${airplay_ip}/queue"

curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${localhost_ip}/sequential_numbers/2.mp4\nCaption-Location: ${localhost_ip}/captions/2.en.srt" \
  "http://${airplay_ip}/queue"

curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${localhost_ip}/sequential_numbers/3.mp4\nCaption-Location: ${localhost_ip}/captions/3.en.srt" \
  "http://${airplay_ip}/queue"

curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${localhost_ip}/sequential_numbers/4.mp4\nCaption-Location: ${localhost_ip}/captions/4.en.srt" \
  "http://${airplay_ip}/queue"

curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${localhost_ip}/sequential_numbers/5.mp4\nCaption-Location: ${localhost_ip}/captions/5.en.srt" \
  "http://${airplay_ip}/queue"

curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${localhost_ip}/sequential_numbers/6.mp4\nCaption-Location: ${localhost_ip}/captions/6.en.srt" \
  "http://${airplay_ip}/queue"

curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${localhost_ip}/sequential_numbers/7.mp4\nCaption-Location: ${localhost_ip}/captions/7.en.srt" \
  "http://${airplay_ip}/queue"

curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${localhost_ip}/sequential_numbers/8.mp4\nCaption-Location: ${localhost_ip}/captions/8.en.srt" \
  "http://${airplay_ip}/queue"

curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${localhost_ip}/sequential_numbers/9.mp4\nCaption-Location: ${localhost_ip}/captions/9.en.srt" \
  "http://${airplay_ip}/queue"

# show video player
curl --silent -X GET \
  "http://${airplay_ip}/show-player"

# begin playback
curl --silent -X GET \
  "http://${airplay_ip}/pause?toggle=0"
