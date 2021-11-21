#### [ExoPlayer AirPlay Receiver](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver)
##### (less formally named: _"ExoAirPlayer"_)

Android app to run on a set-top box and play video URLs "cast" to it with a stateless HTTP API (based on AirPlay).

- - - -

#### Overview:

There is no UI when the app starts.
It's a foreground service with a notification, which runs a web server on port 8192.
The IP address of the server is given in the notification message.

When a video URL is "cast" to the server, a video player opens full-screen.

When an audio URL is "cast" to the server, the music plays in the background.. even when the screen is off.

When either audio or video media is playing and the player's window doesn't have focus
(ex: listening to background audio, or by pressing the "home" button while watching a video),
another notification is added to control playback or refocus the player's window.

[This page](http://webcast-reloaded.surge.sh/airplay_sender.html) is the simplest way to send signals to a running instance,
though other ["high level" tools](#usage-high-level) exist to capture media URLs from the wild.

Audio or video files/playlists can also be started directly from the Android file system,
which makes this app a very suitable replacement for a general-purpose video player.

Playlists can be generated dynamically from:
* a single directory in the Android file system
* a recursive directory tree in the Android file system
* any HTML page with anchor tags that link to media files
  - very useful for playing files from a remote directory listing

Playlists can be read explicitly from any text file with an `.m3u` file extension,
which lists one media item path per line:
* the `.m3u` file can be read from either the Android file system or a remote URL
* each item path can refer to either the Android file system or a remote URL

When a video file is played from the Android file system,
its directory is automatically scanned for matching subtitle file(s).
A match will have the same filename and any of the following extensions: `srt,ttml,vtt,webvtt,ssa,ass`.
Nested extension(s) can optionally be used to distinguish between [different languages](https://en.wikipedia.org/wiki/Language_localisation#Language_tags_and_codes) (ex: `.en-US.srt`, `.es-MX.vtt`).

- - - -

#### Background:

* I use Chromecasts _a lot_
  - they are incredibly adaptable
    * though their protocol is [proprietary and locked down](https://blog.oakbits.com/google-cast-protocol-receiver-authentication.html)
  - I very rarely cast video from Android apps
    * though the [Google Cast SDK for Android](https://developers.google.com/cast/docs/android_sender) is nearly ubiquitous
  - I find much better video content to stream on websites, and wrote some tools to identify and cast these URLs
    * [_WebCast-Reloaded_ Chrome extension](https://github.com/warren-bank/crx-webcast-reloaded) to use with desktop web browsers
    * [_WebCast_ Android app](https://github.com/warren-bank/Android-WebCast) to use with mobile devices
* I also really like using Android set-top boxes
  - mainly to play video files stored on an attached drive
  - they are incredibly adaptable
    * able to run any Android apk, such as:
      - VPN client
      - torrent client
      - FTP client
      - HTTP server
* I thought it would be "fun" to write an app to run on Android set-top boxes that could provide the same functionality that I enjoy on Chromecasts
  - and will work equally well on smaller screens (ex: phones and tablets)

#### Scope:

* the goal is __not__ to provide an app that is recognized on the LAN as a virtual Chromecast device
  - [CheapCast](https://github.com/mauimauer/cheapcast) accomplished this in 2013
    * Google quickly [changed its protocol](https://blog.oakbits.com/google-cast-protocol-discovery-and-connection.html)
* AirPlay uses a very simple stateless [HTTP API](http://nto.github.io/AirPlay.html#video)
  - this is a great starting point
    * it supports: play, pause, seek, stop
  - I'd like to extend this API (for a custom sender)
    * to add support for:
      - video queue, next, previous, mute, set volume
      - audio playlists (_m3u_, _html_ directory index)

#### Design:

* [ExoPlayer](https://github.com/google/ExoPlayer)
  - media player used to render video URLs
* [HttpCore](http://hc.apache.org/httpcomponents-core-ga/)
  - low level HTTP transport components used to build a custom HTTP service
* [jmDNS](https://github.com/jmdns/jmdns)
  - multi-cast DNS service registration used to make AirPlay-compatible HTTP service discoverable on LAN

- - - -

#### Usage (low level):

__AirPlay APIs:__

```bash
  # network address for running instance of 'ExoPlayer AirPlay Receiver'
  airplay_ip='192.168.1.100:8192'

  # file path for test image (on sender):
  image_path='/path/to/image.jpg'

  # URL for test image:
  image_page='https://commons.wikimedia.org/wiki/File:Android_robot.svg'
  image_url='https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/Android_robot.svg/654px-Android_robot.svg.png'

  # URLs for test video:
  videos_page='https://players.akamai.com/hls/'
  video_url_1='https://multiplatform-f.akamaihd.net/i/multi/will/bunny/big_buck_bunny_,640x360_400,640x360_700,640x360_1000,950x540_1500,.f4v.csmil/master.m3u8'
  video_url_2='https://multiplatform-f.akamaihd.net/i/multi/april11/hdworld/hdworld_,512x288_450_b,640x360_700_b,768x432_1000_b,1024x576_1400_m,.mp4.csmil/master.m3u8'
  video_url_3='https://multiplatform-f.akamaihd.net/i/multi/april11/cctv/cctv_,512x288_450_b,640x360_700_b,768x432_1000_b,1024x576_1400_m,.mp4.csmil/master.m3u8'

  # URLs for test video text captions:
  captions_page='https://github.com/gpac/gpac/tree/master/tests/media/webvtt'
  caption_url_1='https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver/raw/v02/tests/.captions/counter.workaround-exoplayer-issue-7122.srt'
  caption_url_2='https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver/raw/v02/tests/.captions/counter.vtt'
  caption_url_3='https://github.com/gpac/gpac/raw/master/tests/media/webvtt/comments.vtt'

  # URLs for test video DRM:
  #   https://exoplayer.dev/drm.html
  #     widevine:  requires Android 4.4+
  #     clearkey:  requires Android 5.0+
  #     playready: requires AndroidTV
  drm_videos_page='https://github.com/google/ExoPlayer/blob/r2.14.0/demos/main/src/main/assets/media.exolist.json'
  drm_video_url_1='https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd'
  drm_video_url_1_license_scheme='widevine'
  drm_video_url_1_license_server='https://proxy.uat.widevine.com/proxy?provider=widevine_test'
  drm_video_url_2='https://playready.directtaps.net/smoothstreaming/SSWSS720H264PR/SuperSpeedway_720.ism/Manifest'
  drm_video_url_2_license_scheme='playready'
  drm_video_url_2_license_server='https://playready.directtaps.net/pr/svc/rightsmanager.asmx'

  # URLs for test audio:
  audio_flac_nfo='https://archive.org/details/tntvillage_457399'
  audio_flac_url='https://archive.org/download/tntvillage_457399/Black%20Sabbath%201970-2013/Studio%20Albums/1970%20Black%20Sabbath/1970%20Black%20Sabbath%20%5B1986%20France%20NELCD%206002%20Castle%5D/Black%20Sabbath%20-%20Black%20Sabbath%20%281986%2C%20Castle%20Communications%2C%20NELCD%206002%29.flac'
  audio_m3u_page='https://archive.org/details/Mozart_Vesperae_Solennes_de_Confessore'
  audio_mp3s_m3u='https://archive.org/download/Mozart_Vesperae_Solennes_de_Confessore/Mozart%20-%20Vesper%C3%A6%20Solennes%20de%20Confessore%20%28Cooke%29.m3u'
  audio_htm_page='https://archive.org/details/tntvillage_455310'
  audio_mp3s_htm='https://archive.org/download/tntvillage_455310/S%26G/Live/1967%20-%20Live%20From%20New%20York%20City%20%40320/'

  # file paths for test media (on receiver):
  video_path='/storage/external_SD/test-media/video/file.mp4'
  subtt_path='/storage/external_SD/test-media/video/file.srt'
  audio_path='/storage/external_SD/test-media/audio/file.mp3'
  plist_path='/storage/external_SD/test-media/all audio and video files.m3u'

  # directory paths for test media (on receiver):
  video_dir_path='/storage/external_SD/test-media/video/'
  audio_dir_path='/storage/external_SD/test-media/audio/'
  recursive_path='/storage/external_SD/test-media/'
```

* display image from local file system (on sender):
  ```bash
    curl --silent -X POST \
      --data-binary "@${image_path}" \
      "http://${airplay_ip}/photo"
  ```
* display image from remote URL:
  ```bash
    curl --silent "$image_url" | \
    curl --silent -X POST \
      --data-binary @- \
      "http://${airplay_ip}/photo"
  ```
* play video #1 (seek to beginning):
  ```bash
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${video_url_1}\nStart-Position: 0" \
      "http://${airplay_ip}/play"
  ```
* seek to `30 seconds` within currently playing video:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/scrub?position=30.0"
  ```
* pause the currently playing video:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/rate?value=0.0"
  ```
* resume playback of the currently paused video:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/rate?value=1.0"
  ```
* increase speed of playback to 10x:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/rate?value=10.0"
  ```
* stop playback:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/stop"
  ```

__extended APIs:__

* seek `30 seconds` forward relative to current position within currently playing video (30 second = 30*1e3 milliseconds):
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/add-scrub-offset?value=30000"
  ```
* seek `30 seconds` backward relative to current position within currently playing video (30 second = 30*1e3 milliseconds):
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/add-scrub-offset?value=-30000"
  ```
* play video #1 (add text captions, set 'Referer' request header, seek to beginning):
  ```bash
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${video_url_1}\nCaption-Location: ${caption_url_1}\nReferer: ${videos_page}\nStart-Position: 0" \
      "http://${airplay_ip}/play"
  ```
* add video #2 to end of queue (add text captions, set 'Referer' request header, seek to 50%):
  ```bash
    # note: position < 1 is a percent of the total track length
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${video_url_2}\nCaption-Location: ${caption_url_2}\nReferer: ${videos_page}\nStart-Position: 0.5" \
      "http://${airplay_ip}/queue"
  ```
* add video #3 to end of queue (add text captions, set 'Referer' request header, seek to 30 seconds):
  ```bash
    # note: position >= 1 is a fixed offset (in seconds)
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${video_url_3}\nCaption-Location: ${caption_url_3}\nReferer: ${videos_page}\nStart-Position: 30" \
      "http://${airplay_ip}/queue"
  ```
* play DRM video #1 (seek to 10 seconds, end playback at 30 seconds):
  ```bash
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${drm_video_url_1}\nDRM-License-Scheme: ${drm_video_url_1_license_scheme}\nDRM-License-Server: ${drm_video_url_1_license_server}\nStart-Position: 10\nStop-Position: 30" \
      "http://${airplay_ip}/play"
  ```
* add DRM video #2 to end of queue (seek to 50%):
  ```bash
    # note: position < 1 is a percent of the total track length
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${drm_video_url_2}\nDRM-License-Scheme: ${drm_video_url_2_license_scheme}\nDRM-License-Server: ${drm_video_url_2_license_server}\nStart-Position: 0.5" \
      "http://${airplay_ip}/queue"
  ```
* skip forward to next video in queue:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/next"
  ```
* skip backward to previous video in queue:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/previous"
  ```
* mute audio:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/volume?value=0.0"
  ```
* set audio volume to 50%:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/volume?value=0.5"
  ```
* set audio volume to 100%:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/volume?value=1.0"
  ```
* set audio volume to 100% and amplify by 10.5 dB:
  ```bash
    # note: audio amplification requires Android 4.4+
    curl --silent -X GET \
      "http://${airplay_ip}/volume?value=11.5"
  ```
* load new text captions for current video in queue:
  ```bash
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Caption-Location: ${caption_url_1}" \
      "http://${airplay_ip}/load-captions"
  ```
* turn text captions off:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/show-captions?toggle=0"
  ```
* turn text captions on:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/show-captions?toggle=1"
  ```
* set time offset for text captions (1 second = 1e6 microseconds):
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/set-captions-offset?value=1000000"
  ```
* add to current time offset for text captions (60 second = 60*1e6 microseconds):
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/add-captions-offset?value=60000000"
  ```
* remove time offset for text captions:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/set-captions-offset?value=0"
  ```
* play audio .flac file (set 'Referer' request header, seek to 50%):
  ```bash
    # note: position < 1 is a percent of the total track length
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${audio_flac_url}\nReferer: ${audio_flac_nfo}\nStart-Position: 0.5" \
      "http://${airplay_ip}/play"
  ```
* play audio .m3u playlist (6 songs, set 'Referer' request header for all songs, seek to 30 seconds in first song):
  ```bash
    # note: position >= 1 is a fixed offset (in seconds)
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${audio_mp3s_m3u}\nReferer: ${audio_m3u_page}\nStart-Position: 30" \
      "http://${airplay_ip}/play"
  ```
* add audio .html directory index playlist to end of queue (20 songs, set 'Referer' request header for all songs, seek to beginning of first song):
  ```bash
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${audio_mp3s_htm}\nReferer: ${audio_htm_page}\nStart-Position: 0" \
      "http://${airplay_ip}/queue"
  ```
* play video from file system on receiver (add text captions, seek to beginning):
  ```bash
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${video_path}\nCaption-Location: ${subtt_path}\nStart-Position: 0" \
      "http://${airplay_ip}/play"
  ```
* add audio from file system on receiver to end of queue (seek to 50%):
  ```bash
    # note: position < 1 is a percent of the total track length
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${audio_path}\nStart-Position: 0.5" \
      "http://${airplay_ip}/queue"
  ```
* play combination of audio and video files in order specified by .m3u playlist from file system on receiver:
  ```bash
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${plist_path}" \
      "http://${airplay_ip}/play"
  ```
* play all audio and video files in specified directory from file system on receiver:
  ```bash
    # note: IF the specified directory contains one or more media files, THEN does not recursively search for media files in subdirectories
    #       IF the specified directory does not contain any media files, THEN does recursively search for media files in all subdirectories
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${video_dir_path}" \
      "http://${airplay_ip}/play"
  ```
* queue all audio and video files in specified directory from file system on receiver:
  ```bash
    # note: IF the specified directory contains one or more media files, THEN does not recursively search for media files in subdirectories
    #       IF the specified directory does not contain any media files, THEN does recursively search for media files in all subdirectories
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${audio_dir_path}" \
      "http://${airplay_ip}/queue"
  ```
* play all audio and video files by recursively searching within specified directory from file system on receiver:
  ```bash
    # note: IF the specified directory contains one or more media files, THEN does not recursively search for media files in subdirectories
    #       IF the specified directory does not contain any media files, THEN does recursively search for media files in all subdirectories
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${recursive_path}" \
      "http://${airplay_ip}/play"
  ```
* show the video player in the top-most foreground Activity:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/show-player"
  ```
* hide the video player so it is no-longer the top-most foreground Activity:
  ```bash
    # note: audio playback will continue in the background
    curl --silent -X GET \
      "http://${airplay_ip}/hide-player"
  ```
* show a Toast containing a custom message:
  ```bash
    curl --silent -X POST \
      -H "Content-Type: text/plain" \
      --data-binary "Lorem Ipsum" \
      "http://${airplay_ip}/show-toast"
  ```
* start an Activity with custom Intent attributes:
  ```bash
    post_body='
      package:
      class:
      action: android.intent.action.VIEW
      data: http://example.com/video.m3u8
      type: application/x-mpegurl
      category: android.intent.category.DEFAULT
      category: android.intent.category.BROWSABLE
      flag: 0x10000000
      flag: 0x00008000
      extra-referUrl: http://example.com/videos.html
      extra-textUrl: http://example.com/video.srt
      extra-useCache: true
      extra-startPos:
      extra-stopPos:
      extra-drmScheme: widevine
      extra-drmUrl: http://widevine.example.com/

      extra-reqHeader: Referer: http://example.com/videos.html
      extra-reqHeader: Origin: http://example.com
      extra-reqHeader: X-Requested-With: XMLHttpRequest
      extra-reqHeader: User-Agent: Chrome/90

      extra-drmHeader: Authorization: Bearer xxxxx
      extra-drmHeader: Cookie: token=xxxxx; sessionID=yyyyy

      chooser-title: Open HLS video stream in:
    '

    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "${post_body}" \
      "http://${airplay_ip}/start-activity"
  ```
* exit the Service:
  ```bash
    # note: also closes the video player foreground Activity, and kills the process
    curl --silent -X GET \
      "http://${airplay_ip}/exit-service"
  ```

#### Notes:

* POST data sent in requests to `/play` and `/queue` API endpoints:
  - contains one _key:value_ pair per line of text
  - lines of text containing unrecognized keys are ignored
  - keys and values can be separated by either `:` or `=` characters, with optional whitespace
  - keys are not case sensitive
  - recognized keys include:
    * _content-location_
    * _caption-location_
    * _referer_
    * _req-header_
      - use key on multiple lines to declare more than one value
    * _use-cache_
    * _start-position_
    * _stop-position_
    * _drm-license-scheme_
      - valid values include:
        * _widevine_
        * _clearkey_
        * _playready_
    * _drm-license-server_
    * _drm-header_
      - use key on multiple lines to declare more than one value
  - keys required:
    * _content-location_
* POST data sent in requests to `/load-captions` API endpoint:
  - contains one _key:value_ pair per line of text
  - lines of text containing unrecognized keys are ignored
  - keys and values can be separated by either `:` or `=` characters, with optional whitespace
  - keys are not case sensitive
  - recognized keys include:
    * _caption-location_
* POST data sent in requests to `/start-activity` API endpoint:
  - contains one _key:value_ pair per line of text
  - lines of text containing unrecognized keys are ignored
  - lines of text containing no value are ignored
  - keys and values can be separated by either `:` or `=` characters, with optional whitespace
  - keys __are__ case sensitive
  - recognized keys include:
    * _package_
    * _class_
    * _action_
    * _data_
      - URI
    * _type_
      - content/mime type
      - value is not normalized to lower-case
    * _category_
      - use key on multiple lines to declare more than one value
    * _flag_
      - use key on multiple lines to declare more than one value
      - format value in decimal (base 10), or hex (base 16) with "0x" prefix
    * _extra-*_
      - name of extra matches the "*" glob
      - value of extra is either a String or String[]
        * depending on whether the fully qualified key had been used on multiple lines to declare more than one value
    * _chooser-title_
      - a non-empty value indicates that a chooser dialog should always be shown
      - the value is the title to display in the chooser dialog
  - keys required to start an explicit Intent:
    * _package_ and _class_
  - keys required to start an implicit Intent:
    * _action_
  - all other keys are optional
* POST data sent in requests to `/share-video` API endpoint:
  - contains one _key:value_ pair per line of text
  - lines of text containing unrecognized keys are ignored
  - keys and values can be separated by either `:` or `=` characters, with optional whitespace
  - keys are not case sensitive
  - recognized keys include:
    * _referUrl_
    * _reqHeader_
    * _textUrl_
    * _drmScheme_
    * _drmUrl_
    * _drmHeader_
  - the Intent to start a new Activity with values derived from the current video in the queue includes..
    * data URI and type
    * extras:
      - (String)   referUrl
      - (String)   textUrl
      - (String)   drmScheme
      - (String)   drmUrl
      - (String[]) reqHeader
      - (String[]) drmHeader
  - POST data can map extras from the default names (above) to alias names
  - extras having String[] values..
    * format each String in the Array as "name: value"
    * since this is a non-standard format,
      when these extras are duplicated to an alias name,
      the format is converted to a Bundle w/ String based key-value pairs
  - POST data example:
    ```text
      referUrl:  Referer
      reqHeader: android.media.intent.extra.HTTP_HEADERS
    ```
* POST data sent in requests to `/edit-preferences` API endpoint:
  - contains one _key:value_ pair per line of text
  - lines of text containing unrecognized keys are ignored
  - keys and values can be separated by either `:` or `=` characters, with optional whitespace
  - keys are not case sensitive
  - recognized keys include:
    * _default-user-agent_
      - type: string
      - description: default _User-Agent_ HTTP request header
      - default: `Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4710.39 Safari/537.36`
    * _max-audio-volume-boost-db_
      - type: integer
      - description: maximum number of dB that the audio can be amplified
      - default: `50`
    * _max-parallel-downloads_
      - type: integer
      - description: maximum number of threads used to download each video in parallel
      - default: `6`
      - limitation: update does not take effect until the app is restarted
    * _seek-back-ms-increment_
      - type: integer
      - description: number of milliseconds that the ![rewind](https://github.com/google/material-design-icons/raw/4.0.0/png/av/fast_rewind/materialicons/18dp/1x/baseline_fast_rewind_black_18dp.png) icon in the video controls overlay will rewind within the current video
      - default: `5000` (ie: 5 seconds)
      - limitation: update does not take effect until the app is restarted
    * _seek-forward-ms-increment_
      - type: integer
      - description: number of milliseconds that the ![fast-forward](https://github.com/google/material-design-icons/raw/4.0.0/png/av/fast_forward/materialicons/18dp/1x/baseline_fast_forward_black_18dp.png) icon in the video controls overlay will fast-forward within the current video
      - default: `15000` (ie: 15 seconds)
      - limitation: update does not take effect until the app is restarted
    * _audio-volume-percent-increment_
      - type: float
      - description: percent of full volume that is changed each time a hardware volume button is pressed
      - context: applies when volume is &lt; 100%
      - default: `0.05` (ie: 5%)
    * _audio-volume-boost-db-increment_
      - type: float
      - description: number of dB that volume amplification is changed each time a hardware volume button is pressed
      - context: applies when volume is &gt; 100%
      - default: `0.50`
    * _ts-extractor-timestamp-search-bytes-factor_
      - type: float
      - description: multiplication factor used to adjust the maximum number of bytes to search at the start/end of each media file to obtain its first and last Program Clock Reference (PCR) values to compute the duration
        * [reference](https://github.com/google/ExoPlayer/issues/8571)
      - default: `2.50`
    * _enable-tunneled-video-playback_
      - type: boolean
      - description: enable tunneled video playback?
        * [reference](https://medium.com/google-exoplayer/tunneled-video-playback-in-exoplayer-84f084a8094d)
      - default: `false`
    * _enable-hdmv-dts-audio-streams_
      - type: boolean
      - description: enable the handling of HDMV DTS audio streams?
        * [reference](https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/extractor/ts/DefaultTsPayloadReaderFactory.html#FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS)
      - default: `false`
    * _pause-on-change-to-audio-output-device_
      - type: boolean
      - description: pause automatically when audio is rerouted from a headset to device speakers?
        * [reference](https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/ExoPlayer.html#setHandleAudioBecomingNoisy(boolean))
      - default: `false`
    * _prefer-extension-renderer_
      - type: boolean
      - description: prefer to use an extension renderer to a core renderer?
        * [reference](https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/DefaultRenderersFactory.html#EXTENSION_RENDERER_MODE_PREFER)
      - default: `false`
      - limitation: update does not take effect until the app is restarted
* POST data sent in requests to `/show-toast` API endpoint:
  - contains an arbitrary block of text

#### Usage (high level):

* [single-page application (SPA)](http://webcast-reloaded.surge.sh/airplay_sender.html) that can run in any web browser, and be used to:
  - send commands to a running instance of [ExoPlayer AirPlay Receiver](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver)
    * "cast" video URLs to its playlist
    * control all aspects of playback

* [_WebCast-Reloaded_ Chrome extension](https://github.com/warren-bank/crx-webcast-reloaded) that can run in any Chromium-based desktop web browser, and be used to:
  - intercept the URL of (nearly) all videos on any website
  - display these video URLs as a list of links
    * clicking on any link will transfer the URL of the video (as well as the URL of the referer webpage) to the [SPA](http://webcast-reloaded.surge.sh/airplay_sender.html) (above)
      - more precisely, the link to the SPA is displayed as a small AirPlay icon ![AirPlay icon](https://github.com/warren-bank/crx-webcast-reloaded/raw/v0.6.0/chrome_extension/data/airplay.png)
      - the other links transfer the video URL to other tools
        * webpage to watch the video in an HTML5 player with the ability to "cast" the video to a Chromecast
        * a running instance of [HLS-Proxy](https://github.com/warren-bank/HLS-Proxy)

* [_WebCast_ Android app](https://github.com/warren-bank/Android-WebCast) that is open-source, and can be used to:
  - intercept the URL of (nearly) all videos on any website
  - display these video URLs as a list
  - when the app's settings are configured to use an external video player:
    * clicking on any video will broadcast an Intent to start the video in another application (ex: _ExoAirPlayer_)

* [Greasemonkey userscripts](https://warren-bank.github.io/Android-WebMonkey/index.html)
  - that can run in any web browser with support for userscripts:
    * [Tampermonkey](https://chrome.google.com/webstore/detail/tampermonkey/dhdgffkkebhmkfjojejmpbldmpobfkfo?hl=en) extension for Chrome/Chromium
    * [Greasemonkey](https://addons.mozilla.org/en-US/firefox/addon/greasemonkey/) addon for Firefox
    * [WebMonkey](https://github.com/warren-bank/Android-WebMonkey) application for Android
    * etc&hellip;
  - and be used to:
    * apply site-specific knowledge to obtain the URL of a video on the requested page
    * in _WebMonkey_:
      - broadcast an Intent to start the video in another application (ex: _ExoAirPlayer_)
    * in other web browsers:
      - automatically redirect to the [SPA](http://webcast-reloaded.surge.sh/airplay_sender.html) (above)

* [_DroidPlay_ Android app](https://github.com/tutikka/DroidPlay) that is open-source, and can be used to:
  - discover AirPlay receivers on the same LAN
  - display images from the local file system
    * sends the entire file in POST data to the receiver
  - stream videos from the local file system
    * runs a local web server
    * casts the URL of the video to the receiver
    * serves the video file when requested by the receiver

- - - -

#### Credits:

* [AirPlay-Receiver-on-Android](https://github.com/gpfduoduo/AirPlay-Receiver-on-Android)
  - __brilliant__
  - what I like:
    * quality of code is excellent
    * implements 90% of what I've described
      - [media player](https://github.com/yixia/VitamioBundle) used to render video URLs
      - _HttpCore_ web server that implements all _AirPlay_ video APIs
      - _jmDNS_ Bonjour registration
  - what I dislike:
    * all libraries are 5 years old
    * doesn't use _ExoPlayer_
    * the repo includes a lot of unused code
      - needs a little housekeeping

- - - -

#### Legal:

* copyright: [Warren Bank](https://github.com/warren-bank)
* license: [GPL-2.0](https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt)
