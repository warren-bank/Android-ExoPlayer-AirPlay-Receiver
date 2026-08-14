#!/usr/bin/env bash

# network address for running instance of 'ExoPlayer AirPlay Receiver'
airplay_ip='192.168.1.100:8192'

# URL for sequentially numbered test videos:
playlist_url='http://192.168.1.101:80/sequential_numbers.m3u'

# turn shuffle on
curl --silent -X GET \
  "http://${airplay_ip}/shuffle?toggle=1"

# add all videos in playlist
curl --silent -X POST \
  -H "Content-Type: text/parameters" \
  --data-binary "Content-Location: ${playlist_url}" \
  "http://${airplay_ip}/play"
