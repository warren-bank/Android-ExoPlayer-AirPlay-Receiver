#!/usr/bin/env bash

# network address for running instance of 'ExoPlayer AirPlay Receiver'
airplay_ip='192.168.1.100:8192'

# URL for test image:
image_url='https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/Android_robot.svg/654px-Android_robot.svg.png'

# URLs for test videos:
videos_page='https://players.akamai.com/hls/'
video_url_1='https://multiplatform-f.akamaihd.net/i/multi/will/bunny/big_buck_bunny_,640x360_400,640x360_700,640x360_1000,950x540_1500,.f4v.csmil/master.m3u8'
video_url_2='https://multiplatform-f.akamaihd.net/i/multi/april11/hdworld/hdworld_,512x288_450_b,640x360_700_b,768x432_1000_b,1024x576_1400_m,.mp4.csmil/master.m3u8'
video_url_3='https://multiplatform-f.akamaihd.net/i/multi/april11/cctv/cctv_,512x288_450_b,640x360_700_b,768x432_1000_b,1024x576_1400_m,.mp4.csmil/master.m3u8'

# URLs for test video text captions:
caption_url_1='https://d12zt1n3pd4xhr.cloudfront.net/fp/subtitles-demo.vtt'
caption_url_2='https://d12zt1n3pd4xhr.cloudfront.net/fp/subtitles-demo.vtt'
caption_url_3='https://d12zt1n3pd4xhr.cloudfront.net/fp/subtitles-demo.vtt'

# display image from remote URL
curl --silent "$image_url" | \
curl --silent -X POST \
  --data-binary @- \
  "http://${airplay_ip}/photo"

sleep 10

# play video #1 (add text captions, set 'Referer' request header, seek to beginning)
curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${video_url_1}\nCaption-Location: ${caption_url_1}\nReferer: ${videos_page}\nStart-Position: 0" \
  "http://${airplay_ip}/play"

sleep 30

# seek to `90 seconds` within currently playing video
curl --silent -X POST -d "" \
  "http://${airplay_ip}/scrub?position=90.0"

sleep 30

# pause the currently playing video
curl --silent -X POST -d "" \
  "http://${airplay_ip}/rate?value=0.0"

sleep 10

# resume playback of the currently paused video
curl --silent -X POST -d "" \
  "http://${airplay_ip}/rate?value=1.0"

sleep 10

# increase speed of playback to 10x
curl --silent -X POST -d "" \
  "http://${airplay_ip}/rate?value=10.0"

# add video #2 to end of queue (add text captions, set 'Referer' request header, seek to 50%)
#   note: position < 1 is a percent of the total video length
curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${video_url_2}\nCaption-Location: ${caption_url_2}\nReferer: ${videos_page}\nStart-Position: 0.5" \
  "http://${airplay_ip}/queue"

# add video #3 to end of queue (add text captions, set 'Referer' request header, seek to 30 seconds)
#   note: position >= 1 is a fixed offset (in seconds)
curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${video_url_3}\nCaption-Location: ${caption_url_3}\nReferer: ${videos_page}\nStart-Position: 30" \
  "http://${airplay_ip}/queue"

sleep 10

# decrease speed of playback to 1x
curl --silent -X POST -d "" \
  "http://${airplay_ip}/rate?value=1.0"

# skip forward to next video in queue (video #2 @ 50%)
curl --silent -X POST -d "" \
  "http://${airplay_ip}/next"

sleep 30

# skip forward to next video in queue (video #3 @ 30 seconds)
curl --silent -X POST -d "" \
  "http://${airplay_ip}/next"

sleep 30

# skip backward to previous video in queue (video #2 @ 50%)
curl --silent -X POST -d "" \
  "http://${airplay_ip}/previous"

sleep 30

# mute audio
curl --silent -X POST -d "" \
  "http://${airplay_ip}/volume?value=0.0"

sleep 10

# set audio volume to 50%
curl --silent -X POST -d "" \
  "http://${airplay_ip}/volume?value=0.5"

sleep 10

# set audio volume to 100%
curl --silent -X POST -d "" \
  "http://${airplay_ip}/volume?value=1.0"

# seek to beginning of currently playing video
curl --silent -X POST -d "" \
  "http://${airplay_ip}/scrub?position=0"

sleep 10

# turn text captions off
curl --silent -X POST -d "" \
  "http://${airplay_ip}/captions?toggle=0"

sleep 10

# turn text captions on
curl --silent -X POST -d "" \
  "http://${airplay_ip}/captions?toggle=1"

sleep 10

# stop playback
curl --silent -X POST -d "" \
  "http://${airplay_ip}/stop"
