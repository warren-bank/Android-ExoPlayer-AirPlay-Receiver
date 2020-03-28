#!/usr/bin/env bash

# network address for running instance of 'ExoPlayer AirPlay Receiver'
airplay_ip='192.168.1.100:8192'

# URL for test image:
image_url='https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/Android_robot.svg/654px-Android_robot.svg.png'

# URLs for test video:
videos_page='https://players.akamai.com/hls/'
video_url_1='https://multiplatform-f.akamaihd.net/i/multi/will/bunny/big_buck_bunny_,640x360_400,640x360_700,640x360_1000,950x540_1500,.f4v.csmil/master.m3u8'
video_url_2='https://multiplatform-f.akamaihd.net/i/multi/april11/hdworld/hdworld_,512x288_450_b,640x360_700_b,768x432_1000_b,1024x576_1400_m,.mp4.csmil/master.m3u8'
video_url_3='https://multiplatform-f.akamaihd.net/i/multi/april11/cctv/cctv_,512x288_450_b,640x360_700_b,768x432_1000_b,1024x576_1400_m,.mp4.csmil/master.m3u8'

# URLs for test video text captions:
caption_url_1='https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver/raw/v01/tests/.captions/counter.workaround-exoplayer-issue-7122.srt'
caption_url_2='https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver/raw/v01/tests/.captions/counter.vtt'
caption_url_3='https://github.com/gpac/gpac/raw/master/tests/media/webvtt/comments.vtt'

# URLs for test audio:
audio_flac_nfo='https://archive.org/details/tntvillage_457399'
audio_flac_url='https://archive.org/download/tntvillage_457399/Black%20Sabbath%201970-2013/Studio%20Albums/1970%20Black%20Sabbath/1970%20Black%20Sabbath%20%5B1986%20France%20NELCD%206002%20Castle%5D/Black%20Sabbath%20-%20Black%20Sabbath%20%281986%2C%20Castle%20Communications%2C%20NELCD%206002%29.flac'
audio_m3u_page='https://archive.org/details/Mozart_Vesperae_Solennes_de_Confessore'
audio_mp3s_m3u='https://archive.org/download/Mozart_Vesperae_Solennes_de_Confessore/Mozart%20-%20Vesper%C3%A6%20Solennes%20de%20Confessore%20%28Cooke%29.m3u'
audio_htm_page='https://archive.org/details/tntvillage_455310'
audio_mp3s_htm='https://archive.org/download/tntvillage_455310/S%26G/Live/1967%20-%20Live%20From%20New%20York%20City%20%40320/'

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
#   note: position < 1 is a percent of the total track length
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
  "http://${airplay_ip}/show-captions?toggle=0"

sleep 10

# turn text captions on
curl --silent -X POST -d "" \
  "http://${airplay_ip}/show-captions?toggle=1"

sleep 10

# set time offset for text captions (-30 sec = -30*1e6)
curl --silent -X POST -d "" \
  "http://${airplay_ip}/set-captions-offset?value=-30000000"

sleep 10

# add to current time offset for text captions (-10 sec = -10*1e6)
curl --silent -X POST -d "" \
  "http://${airplay_ip}/add-captions-offset?value=-10000000"

sleep 10

# remove time offset for text captions
curl --silent -X POST -d "" \
  "http://${airplay_ip}/set-captions-offset?value=0"

sleep 10

# stop playback
curl --silent -X POST -d "" \
  "http://${airplay_ip}/stop"

sleep 5

# play audio .flac file (set 'Referer' request header, seek to 50%)
#   note: position < 1 is a percent of the total track length
curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${audio_flac_url}\nReferer: ${audio_flac_nfo}\nStart-Position: 0.5" \
  "http://${airplay_ip}/play"

sleep 30

# play audio .m3u playlist (6 songs, set 'Referer' request header for all songs, seek to 30 seconds in first song)
#   note: position >= 1 is a fixed offset (in seconds)
#   note: after the first song, each additional song is added following a 1 second delay; adding 6 songs will take 5 seconds to complete.
curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${audio_mp3s_m3u}\nReferer: ${audio_m3u_page}\nStart-Position: 30" \
  "http://${airplay_ip}/play"

sleep 10

# add audio .html directory index playlist to end of queue (20 songs, set 'Referer' request header for all songs, seek to beginning of first song)
#   note: after the first song, each additional song is added following a 1 second delay; adding 20 songs will take 19 seconds to complete.
curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${audio_mp3s_htm}\nReferer: ${audio_htm_page}\nStart-Position: 0" \
  "http://${airplay_ip}/queue"

sleep 30

# skip forward to next song in queued playlist (.m3u song #2)
curl --silent -X POST -d "" \
  "http://${airplay_ip}/next"

sleep 15

# skip forward to next song in queued playlist (.m3u song #3)
curl --silent -X POST -d "" \
  "http://${airplay_ip}/next"

sleep 15

# skip forward to next song in queued playlist (.m3u song #4)
curl --silent -X POST -d "" \
  "http://${airplay_ip}/next"

sleep 1

# skip forward to next song in queued playlist (.m3u song #5)
curl --silent -X POST -d "" \
  "http://${airplay_ip}/next"

sleep 1

# skip forward to next song in queued playlist (.m3u song #6)
curl --silent -X POST -d "" \
  "http://${airplay_ip}/next"

sleep 1

# skip forward to next song in queued playlist (.html song #1)
curl --silent -X POST -d "" \
  "http://${airplay_ip}/next"

sleep 15

# skip forward to next song in queued playlist (.html song #2)
curl --silent -X POST -d "" \
  "http://${airplay_ip}/next"

sleep 15

# skip forward to next song in queued playlist (.html song #3)
curl --silent -X POST -d "" \
  "http://${airplay_ip}/next"

sleep 15

# stop playback
curl --silent -X POST -d "" \
  "http://${airplay_ip}/stop"
