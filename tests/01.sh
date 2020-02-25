#!/usr/bin/env bash

# network address for running instance of 'ExoPlayer AirPlay Receiver'
airplay_ip='192.168.1.100:8192'

# URLs for test videos:
videos_page='https://players.akamai.com/hls/'
video_url_1='https://multiplatform-f.akamaihd.net/i/multi/will/bunny/big_buck_bunny_,640x360_400,640x360_700,640x360_1000,950x540_1500,.f4v.csmil/master.m3u8'
video_url_2='https://multiplatform-f.akamaihd.net/i/multi/april11/hdworld/hdworld_,512x288_450_b,640x360_700_b,768x432_1000_b,1024x576_1400_m,.mp4.csmil/master.m3u8'
video_url_3='https://multiplatform-f.akamaihd.net/i/multi/april11/cctv/cctv_,512x288_450_b,640x360_700_b,768x432_1000_b,1024x576_1400_m,.mp4.csmil/master.m3u8'

# play video #1
curl -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${video_url_1}\nStart-Position: 0" \
  "http://${airplay_ip}/play"

# seek to `30 seconds` within currently playing video
#   note: POST body is required, even when it contains no data
curl -X POST \
  --data-binary "" \
  "http://${airplay_ip}/scrub?position=30.0"

# pause the currently playing video
#   note: POST body is required, even when it contains no data
curl -X POST \
  --data-binary "" \
  "http://${airplay_ip}/rate?value=0.0"

# add video #2 to end of queue (set 'Referer' request header, seek to 50%)
#   note: position < 1 is a percent of the total video length
curl -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${video_url_2}\nReferer: ${videos_page}\nStart-Position: 0.5" \
  "http://${airplay_ip}/queue"

# add video #3 to end of queue (set 'Referer' request header, seek to 30 seconds)
#   note: position >= 1 is a fixed offset (in seconds)
curl -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${video_url_3}\nReferer: ${videos_page}\nStart-Position: 30" \
  "http://${airplay_ip}/queue"

# skip forward to next video in queue
#   note: POST body is required, even when it contains no data
curl -X POST \
  --data-binary "" \
  "http://${airplay_ip}/next"

# resume playback of the current video in queue at normal speed (ie: video #2 @ rate 1x)
#   note: POST body is required, even when it contains no data
curl -X POST \
  --data-binary "" \
  "http://${airplay_ip}/rate?value=1.0"
