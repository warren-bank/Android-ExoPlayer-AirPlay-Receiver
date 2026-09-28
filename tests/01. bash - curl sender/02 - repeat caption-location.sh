#!/usr/bin/env bash

# network address for running instance of 'ExoPlayer AirPlay Receiver'
airplay_ip='192.168.1.100:8192'

video_url='https://github.com/chthomos/video-media-samples/raw/master/big-buck-bunny-480p-30sec.mp4'

caption_url_1='https://github.com/moust/MediaPlayer/raw/master/demo/subtitles.srt'
caption_url_2='https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver/raw/v02/tests/05.%20issues/ExoPlayer/7122/.captions/counter.workaround-exoplayer-issue-7122.srt'
caption_url_3='https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver/raw/v02/tests/05.%20issues/ExoPlayer/7122/.captions/counter.vtt'
caption_url_4='http://example.com/subtitles.404.vtt'

curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${video_url}\nCaption-Location: ${caption_url_1}\nCaption-Location: ${caption_url_2}\nCaption-Location: ${caption_url_3}" \
  "http://${airplay_ip}/play"

curl --silent \
  "http://${airplay_ip}/media-item-info"

curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Caption-Location: ${caption_url_2}\nCaption-Location: ${caption_url_3}\nCaption-Location: ${caption_url_4}" \
  "http://${airplay_ip}/load-captions"

curl --silent \
  "http://${airplay_ip}/media-item-info"
