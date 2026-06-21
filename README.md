#### [ExoPlayer AirPlay Receiver](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver)
##### (less formally named: _"ExoAirPlayer"_)

Android app to run on a set-top box and play video URLs "cast" to it with a stateless HTTP API (based on AirPlay v1).

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

[This page](http://webcast-reloaded.frii.site/airplay_sender.html) is the simplest way to send signals to a running instance,
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
    * [collection of userscripts](https://warren-bank.github.io/Android-WebMonkey/index.html) to use with both [mobile devices](https://github.com/warren-bank/Android-WebMonkey) and [desktop web browsers](https://www.tampermonkey.net/)
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
    * Google quickly [changed its protocol](https://web.archive.org/web/20210117143825/https://blog.oakbits.com/google-cast-protocol-discovery-and-connection.html)
* AirPlay v1 uses a very simple stateless [HTTP API](http://nto.github.io/AirPlay.html#video)
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
  - multi-cast DNS service registration used to make the AirPlay v1 compatible HTTP service discoverable on LAN

- - - -

#### Usage (low level):

__AirPlay v1 compatible APIs:__

```bash
  # network address for running instance of 'ExoPlayer AirPlay Receiver'
  airplay_ip='192.168.1.100:8192'

  # file path for test image (on sender):
  image_path='/path/to/image.jpg'

  # URL for test image:
  image_page='https://commons.wikimedia.org/wiki/File:Android_robot.svg'
  image_url='https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/Android_robot.svg/654px-Android_robot.svg.png'

  # URLs for test video:
  videos_page='https://test-streams.mux.dev/'
  video_url_1='https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'
  video_url_2='https://test-streams.mux.dev/tos_ismc/main.m3u8'
  video_url_3='https://bitmovin-a.akamaihd.net/content/sintel/sintel.mpd'

  # URLs for test video text captions:
  captions_page='https://github.com/gpac/gpac/tree/master/tests/media/webvtt'
  caption_url_1='https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver/raw/v02/tests/05.%20issues/ExoPlayer/7122/.captions/counter.workaround-exoplayer-issue-7122.srt'
  caption_url_2='https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver/raw/v02/tests/05.%20issues/ExoPlayer/7122/.captions/counter.vtt'
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
  audio_flac_nfo='https://archive.org/details/black-sabbath-black-sabbath-1970-lp-flac'
  audio_flac_url='https://archive.org/download/black-sabbath-black-sabbath-1970-lp-flac/BLACK%20SABBATH%20-%201970%20-%20Black%20Sabbath%20%5BUK%20PBTHAL%20LP%2024-96%5D%20%5BFLAC%5D/01.-Black%20Sabbath.flac'
  audio_m3u_page='https://archive.org/details/Mozart_Vesperae_Solennes_de_Confessore'
  audio_mp3s_m3u='https://archive.org/download/Mozart_Vesperae_Solennes_de_Confessore/Mozart%20-%20Vesper%C3%A6%20Solennes%20de%20Confessore%20%28Cooke%29.m3u'
  audio_htm_page='https://archive.org/details/Simon-and-Garfunkel-1966-10-21'
  audio_mp3s_htm='https://archive.org/download/Simon-and-Garfunkel-1966-10-21/'

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
* toggle the 'on/off' state of whether to pause playback:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/pause"
  ```
* set the state of whether to pause playback to 'on':
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/pause?toggle=1"
  ```
* set the state of whether to pause playback to 'off':
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/pause?toggle=0"
  ```
* increase speed of playback to 10x:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/rate?value=10.0"
  ```
* reset speed of playback to 1x:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/rate?value=1.0"
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
* play video #1 and add videos #2 and #3 to end of queue (set 'Referer' request header):
  ```bash
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${video_url_1}\nContent-Location: ${video_url_2}\nContent-Location: ${video_url_3}\nReferer: ${videos_page}" \
      "http://${airplay_ip}/play"
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
* silence audio:
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
* toggle the 'on/off' state of whether the audio volume is mute:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/mute-volume"
  ```
* set the state of whether the audio volume is mute to 'on':
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/mute-volume?toggle=1"
  ```
* set the state of whether the audio volume is mute to 'off':
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/mute-volume?toggle=0"
  ```
* load new text captions for current video in queue:
  ```bash
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Caption-Location: ${caption_url_1}" \
      "http://${airplay_ip}/load-captions"
  ```
* toggle the 'on/off' state of whether the text captions are visible:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/show-captions"
  ```
* set the state of whether the text captions are visible to 'on':
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/show-captions?toggle=1"
  ```
* set the state of whether the text captions are visible to 'off':
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/show-captions?toggle=0"
  ```
* set font style and size options for text captions to custom values:
  ```bash
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Apply-Embedded: false\nFont-Size: 20" \
      "http://${airplay_ip}/set-captions-style"
  ```
* set font style and size options for text captions to default values:
  ```bash
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Apply-Embedded: true\nFont-Size: 0" \
      "http://${airplay_ip}/set-captions-style"
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
* set regex filters for text captions (one per line):
  ```bash
    curl --silent -X POST \
      -H "Content-Type: text/plain" \
      --data-binary "\[[^\]]*\]\n\([^\)]*\)" \
      "http://${airplay_ip}/set-captions-filters"
  ```
* add additional regex filters for text captions (one per line):
  ```bash
    # note: regex patterns can include embedded flag expressions
    curl --silent -X POST \
      -H "Content-Type: text/plain" \
      --data-binary "(?i)\b(?:foo|bar|baz)\b\n(?i)\b(?:hello|world)\b" \
      "http://${airplay_ip}/add-captions-filters"
  ```
* remove all regex filters for text captions:
  ```bash
    curl --silent -X POST \
      -H "Content-Type: text/plain" \
      --data-binary "" \
      "http://${airplay_ip}/set-captions-filters"
  ```
* set repeat mode:
  ```bash
    # note: supported values: [off,one,all]. default: all
    curl --silent -X GET \
      "http://${airplay_ip}/repeat-mode?value=all"
  ```
* set resize mode:
  ```bash
    # note: supported values: [fit,width,height,fill,zoom]. default: fit
    curl --silent -X GET \
      "http://${airplay_ip}/resize-mode?value=fit"
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
* show the app settings in the top-most foreground Activity:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/show-settings"
  ```
* show the video player in the top-most foreground Activity:
  ```bash
    curl --silent -X GET \
      "http://${airplay_ip}/show-player"
  ```
* show the video player in picture-in-picture (PiP) mode:
  ```bash
    # note: PiP mode is only available on Android TV 7.0 or Android 8.0 and higher
    curl --silent -X GET \
      "http://${airplay_ip}/show-player-pip"
  ```
* hide the video player so it is neither the top-most foreground Activity nor in PiP mode:
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
      - use key on multiple lines to declare more than one value
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
* POST data sent in requests to `/set-captions-style` API endpoint:
  - contains one _key:value_ pair per line of text
  - lines of text containing unrecognized keys are ignored
  - keys and values can be separated by either `:` or `=` characters, with optional whitespace
  - keys are not case sensitive
  - recognized keys include:
    * _apply-embedded_
      - apply styles and sizes embedded in the text captions?
    * _font-size_
      - unit: _sp_
      - note: value `0` is special and used to revert to default
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
      - where "*" is a glob pattern
        * to indicate that the recognized key can contain any additional sequence of characters
        * to capture the case sensitive name of an Intent extra
      - the data type of the Intent extra is determined as follows:
        * if a distinct key occurs on multiple lines
          - `String[]`
        * if the value begins with an explicit type cast
          - matching any of the following case insensitive substrings:
            * `(String)`
            * `(String[])`
            * `(bool)`
            * `(bool[])`
            * `(boolean)`
            * `(boolean[])`
            * `(byte)`
            * `(byte[])`
            * `(char)`
            * `(char[])`
            * `(double)`
            * `(double[])`
            * `(float)`
            * `(float[])`
            * `(int)`
            * `(int[])`
            * `(integer)`
            * `(integer[])`
            * `(long)`
            * `(long[])`
            * `(short)`
            * `(short[])`
          - notes:
            * when a value is explicitly cast to an array..
              - if the data type is: `String[]`
                * the value is _not_ tokenized
                * the array:
                  - has length: 1
                  - is equal to: `[value]`
              - if the data type is: `char[]`
                * the value is parsed as an ordered sequence of characters
                * commas are removed
              - for all other data types
                * the value is parsed as an ordered comma-separated list
        * if the value can be implicitly cast<br>(note: the following regex patterns are only descriptive)
          - matches the regex pattern: `/(true|false)/i`
            * `boolean`
          - matches the regex pattern: `/[+-]?\d+/`
            * `int`
          - matches the regex pattern: `/[+-]?\d+L/i`
            * `long`
          - matches the regex pattern: `/[+-]?\d+(\.\d+)?F/i`
            * `float`
          - matches the regex pattern: `/[+-]?\d+(\.\d+)?D/i`
            * `double`
        * otherwise
          - `String`
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
    * _http-api-password_
      - type: string
      - description: secret shared only with authorized clients
      - default: ``
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
    * _enable-audio-passthrough_
      - type: boolean
      - description: allow audio passthrough based on hardware capability detection?
      - default: `true`
      - limitation: update does not take effect until the app is restarted
    * _enable-downmix-surround-sound-to-stereo_
      - type: boolean
      - description: downmix surround sound to stereo?
      - default: `false`
    * _enable-downmix-stereo-sound-to-mono_
      - type: boolean
      - description: downmix stereo sound to mono?
      - default: `false`
    * _enable-keycode-event-notifications_
      - type: boolean
      - description: show KeyCode event notifications?
      - default: `false`
* POST data sent in requests to `/set-keycode-map` API endpoint:
  - contains one _key:value_ pair per line of text
  - lines of text containing unrecognized keys or values are ignored
  - keys and values can be separated by either `:` or `=` characters, with optional whitespace
  - keys and values are case sensitive
  - recognized keys and values include:
    * [`KEYCODE_0`](https://developer.android.com/reference/android/view/KeyEvent#KEYCODE_0) &hellip; [`KEYCODE_ZOOM_OUT`](https://developer.android.com/reference/android/view/KeyEvent#KEYCODE_ZOOM_OUT)
  - the purpose for which is to remap keyboard keys to different keycode values
  - POST data example:
    ```text
      KEYCODE_DPAD_DOWN:  KEYCODE_G
      KEYCODE_DPAD_UP:    KEYCODE_H
      KEYCODE_DPAD_LEFT:  KEYCODE_MEDIA_REWIND
      KEYCODE_DPAD_RIGHT: KEYCODE_MEDIA_FAST_FORWARD
      KEYCODE_ENTER:      KEYCODE_MEDIA_PLAY_PAUSE
    ```
  - which remaps:
    * down arrow key to "G" keycode
      - decrease subtitle offset by 1 second
    * up arrow key to "H" keycode
      - increase subtitle offset by 1 second
    * left arrow key to "![fast_rewind](https://github.com/google/material-design-icons/raw/4.0.0/png/av/fast_rewind/materialicons/18dp/1x/baseline_fast_rewind_black_18dp.png)" keycode
      - rewind by 5 seconds
    * right arrow key to "![fast_forward](https://github.com/google/material-design-icons/raw/4.0.0/png/av/fast_forward/materialicons/18dp/1x/baseline_fast_forward_black_18dp.png)" keycode
      - fast forward by 15 seconds
    * enter key to "![play_arrow](https://github.com/google/material-design-icons/raw/4.0.0/png/av/play_arrow/materialicons/18dp/1x/baseline_play_arrow_black_18dp.png)![pause](https://github.com/google/material-design-icons/raw/4.0.0/png/av/pause/materialicons/18dp/1x/baseline_pause_black_18dp.png)" keycode
      - toggle _play/pause_
* POST data sent in requests to `/show-toast` API endpoint:
  - contains an arbitrary block of text
* POST data sent in requests to `/set-captions-filters` and `/add-captions-filters` API endpoints:
  - contains one regex pattern per line of text
  - regex patterns can include [embedded flag expressions](https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html#embedded)
  - if POST data is absent in a request to `/set-captions-filters`, then the list of regex filters is cleared

#### HTTP API Password:

* configure _ExoAirPlayer_ preferences&hellip;
  - assign a value to: `HTTP API Password`
  - default: _none_
* this is a shared secret
  - it is never sent over the LAN in clear text
* when a client makes a network request to any HTTP API endpoint,
  - _ExoAirPlayer_ will look for either&hellip;
    * the HTTP request header: `X-ExoAirPlayer-Password: VALUE`
    * the querystring parameter: `password=VALUE`
  - where:
    * `VALUE` is a hex-encoded SHA-1 hash of: `<IP-of-Client>:<PASSWORD>`
    * `VALUE` is not case sensitive, and `0x` prefix is optional
    * `<IP-of-Client>` is intended to prevent replay attacks,<br>by making the hash valid only when the client's request originates from a particular IP
* when this value is either not found or not correct,
  - _ExoAirPlayer_ will respond with:
    * _code_: `401 Unauthorized`
    * _body_: `<IP-of-Client>`

```bash
  # network address for running instance of 'ExoPlayer AirPlay Receiver'
  airplay_ip='192.168.1.100:8192'

  # network address for client
  client_ip=$(curl --fail-with-body --silent -X GET "http://${airplay_ip}/")

  # SHA-1 hash to authenticate HTTP API requests
  password='abc'
  sha1_hash=$(echo -n "${client_ip}:${password}" | sha1sum -t -z | awk '{print $1}')

  # toggle the 'on/off' state of whether to pause playback:
  curl --silent -X GET \
    -H "X-ExoAirPlayer-Password: ${sha1_hash}" \
    "http://${airplay_ip}/pause"

  # toggle the 'on/off' state of whether to pause playback:
  curl --silent -X GET \
    "http://${airplay_ip}/pause?password=${sha1_hash}"
```

- - - -

#### Usage (high level):

* [single-page application (SPA)](http://webcast-reloaded.frii.site/airplay_sender.html) that can run in any web browser, and be used to:
  - send commands to a running instance of [ExoPlayer AirPlay Receiver](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver)
    * "cast" video URLs to its playlist
    * control all aspects of playback

* [_WebCast-Reloaded_ Chrome extension](https://github.com/warren-bank/crx-webcast-reloaded) that can run in any Chromium-based desktop web browser, and be used to:
  - intercept the URL of (nearly) all videos on any website
  - display these video URLs as a list of links
    * clicking on any link will transfer the URL of the video (as well as the URL of the referer webpage) to the [SPA](http://webcast-reloaded.frii.site/airplay_sender.html) (above)
      - more precisely, the link to the SPA is displayed as a small AirPlay icon ![AirPlay icon](https://github.com/warren-bank/crx-webcast-reloaded/raw/v0.6.0/chrome_extension/data/airplay.png)
      - the other links transfer the video URL to other tools
        * webpage to watch the video in an HTML5 player with the ability to "cast" the video to a Chromecast
        * a running instance of [HLS-Proxy](https://github.com/warren-bank/HLS-Proxy)

* [_WebCast_ Android app](https://github.com/warren-bank/Android-WebCast) that is open-source, and can be used to:
  - intercept the URL of (nearly) all videos on any website
  - display these video URLs as a list
  - when the app's settings are configured to use an external video player:
    * clicking on any video will broadcast an Intent to start the video in another application (ex: _ExoAirPlayer_)

* [collection of userscripts](https://warren-bank.github.io/Android-WebMonkey/index.html)
  - that can run in any web browser with support for userscripts:
    * [WebMonkey](https://github.com/warren-bank/Android-WebMonkey) application for Android
    * [Tampermonkey](https://www.tampermonkey.net/) extension for [Chrome/Chromium](https://chrome.google.com/webstore/detail/tampermonkey/dhdgffkkebhmkfjojejmpbldmpobfkfo) and [Firefox/Fenix](https://addons.mozilla.org/en-US/firefox/addon/tampermonkey/)
    * [Violentmonkey](https://violentmonkey.github.io/) extension for [Chrome/Chromium](https://chrome.google.com/webstore/detail/violent-monkey/jinjaccalgkegednnccohejagnlnfdag) and [Firefox/Fenix](https://addons.mozilla.org/firefox/addon/violentmonkey/)
    * [Greasemonkey](https://addons.mozilla.org/en-US/firefox/addon/greasemonkey/) addon for Firefox
    * etc&hellip;
  - and be used to:
    * apply site-specific knowledge to obtain the URL of a video on the requested page
    * in _WebMonkey_:
      - broadcast an Intent to start the video in another application (ex: _ExoAirPlayer_)
    * in other web browsers:
      - automatically redirect to the [SPA](http://webcast-reloaded.frii.site/airplay_sender.html) (above)

* [_MpcFreemote_ Android app](https://github.com/warren-bank/Android-MpcFreemote) that is open-source, and can be used to:
  - discover [Media Player Classic (MPC) receivers](#media-player-classic-home-cinema-mpc-hc-api) on the same LAN
  - browse the remote file system to find A/V media, and initiate playback
  - send commands to control most aspects of playback

* [_Toaster Cast_ Android app](https://apkpure.com/toaster-cast-dlna-upnp-player/com.n7mobile.simpleupnpplayer) that can be used to:
  - discover AirPlay v1 receivers on the same LAN
  - discover DLNA media servers on the same LAN
    * optionally, runs a local DLNA media server
    * browse media on all servers
    * casts the URL of media hosted by a DLNA server to the receiver

* [_WebTorrent Desktop_ app](https://github.com/webtorrent/webtorrent-desktop) (for Windows, Mac, Linux) that is open-source, and can be used to:
  - download videos from a p2p network
    * supports connections to peers using both BitTorrent (TCP) and WebTorrent (WebRTC)
  - discover AirPlay v1 receivers on the same LAN
  - stream videos that are fully or partially downloaded
    * runs a local web server
    * casts the URL of the video to the receiver
    * serves the video file when requested by the receiver

* [_DroidPlay_ Android app](https://github.com/tutikka/DroidPlay) that is open-source, and can be used to:
  - discover AirPlay v1 receivers on the same LAN
  - display images from the local file system
    * sends the entire file in POST data to the receiver
  - stream videos from the local file system
    * runs a local web server
    * casts the URL of the video to the receiver
    * serves the video file when requested by the receiver

* [fork of: _DroidPlay_ Android app](https://github.com/warren-bank/Android-AirPlay-Client) that is open-source, and can additionally be used to:
  - mirror the screen to an AirPlay v1 receiver
    * only available on Android 5.0 and higher

* [_airplay_ desktop app](https://github.com/zeppelsoftware/airplay) (for Java JRE) that is open-source, and can be used to:
  - mirror the screen to an AirPlay v1 receiver
    ```bash
      airplay_ip='192.168.1.100:8192'
      java -jar "airplay.jar" -h "$airplay_ip" -d
    ```

* [_exoairtube_ desktop app](https://github.com/warren-bank/node-ExoAirPlayer-YouTube-sender) (for Node.js) that is open-source, and can be used to:
  - discover AirPlay v1 receivers on the same LAN
  - stream videos hosted by YouTube
    * supports YouTube playlists
    * casts the URL of the highest quality video format available to the receiver

#### Other Tools (high level):

* [_HTTP Shortcuts_ Android app](https://github.com/Waboodoo/HTTP-Shortcuts) that is open-source, and can be used to:
  - configure shortcuts to make HTTP requests that target _ExoAirPlayer_ API endpoints
  - user contribution:
    * [configuration data file](./etc/3rd-party%20high-level%20client%20apps%20%5Buser%20contributions%5D/HTTP%20Shortcuts%20for%20Android/shortcuts.json) by [heinnovator](https://github.com/heinnovator)

* [_Bookmarks_ Android app](https://github.com/warren-bank/Android-Bookmarks) that is open-source, and can be used to:
  - bookmark media streams
    - configure [_Intents_](https://developer.android.com/reference/android/content/Intent) that play media streams in _ExoAirPlayer_&hellip; running on the same device
      * example:
        - _Name_ = `CBS News`
        - _Action_ = `android.intent.action.VIEW`
        - _Package Name_ = `com.github.warren_bank.exoplayer_airplay_receiver`
        - _Class Name_ = `com.github.warren_bank.exoplayer_airplay_receiver.ui.StartNetworkingServiceActivity`
        - _Data URI_ = `https://www.cbsnews.com/common/video/cbsn_header_prod.m3u8`
        - _Data (MIME) Type_ = `application/x-mpegurl`
        - extra:
          * _Name_ = `referUrl`
          * _Type of Value_ = `String`
          * _Value_ = `https://www.cbsnews.com/live/`
      * notes:
        - this example configures an _explicit_ Intent
          * the media stream will always play in _ExoAirPlayer_&hellip; running on the same device
        - an _implicit_ Intent could also be configured
          * the following configuration fields can be empty:
            - _Package Name_
            - _Class Name_
          * as a result, an Activity chooser dialog will prompt the user to select from a list of _all_ installed apps that match the _implicit_ Intent&hellip; which includes _ExoAirPlayer_
        - the HLS video stream for `CBS News` doesn't actually require a `Referer` HTTP request header, but one is added to illustrate how to do so
    - start an Activity for any such bookmarked _Intent_ by either:
      * triggering manually (method 1)
        - click on the chosen _Intent_
      * triggering manually (method 2)
        - long click on the chosen _Intent_ to open its context menu
        - select: _Perform&hellip;_ &gt; _Start Activity_
      * scheduling an alarm
        - long click on the chosen _Intent_ to open its context menu
        - select: _Schedule&hellip;_
          * Perform = _Start Activity_
          * configure other settings: date, time, interval, precision, etc&hellip;
  - simulate keypress of media keys
    - configure [_Intents_](https://developer.android.com/reference/android/content/Intent) that broadcast [media key events](https://developer.android.com/reference/android/view/KeyEvent) to _ExoAirPlayer_&hellip; running on the same device
      * example:
        - _Name_ = `stop`
        - _Action_ = `android.intent.action.MEDIA_BUTTON`
        - _Package Name_ = `com.github.warren_bank.exoplayer_airplay_receiver`
        - _Class Name_ = `androidx.media.session.MediaButtonReceiver`
        - extra:
          * _Name_ = `android.intent.extra.KEY_EVENT`
          * _Type of Value_ = `int`
          * _Value_ = [`86`](https://developer.android.com/reference/android/view/KeyEvent#KEYCODE_MEDIA_STOP)
      * notes:
        - this example configures an _explicit_ Intent
        - Android 8.0+ [does not allow](https://developer.android.com/guide/components/broadcast-exceptions) any app to receive _implicit_ broadcasts for media key events
        - if _ExoAirPlayer_ is used on a device that is running a version of Android &lt; 8.0, then an _implicit_ Intent is sufficient
          * the following configuration fields can be empty:
            - _Package Name_
            - _Class Name_
          * as a result, broadcast receivers in _all_ installed apps that match the _implicit_ Intent will be notified
    - broadcast any such bookmarked _Intent_ by either:
      * triggering manually
        - long click on the chosen _Intent_ to open its context menu
        - select: _Perform&hellip;_ &gt; _Send Broadcast_
      * scheduling an alarm
        - long click on the chosen _Intent_ to open its context menu
        - select: _Schedule&hellip;_
          * Perform = _Send Broadcast_
          * configure other settings: date, time, interval, precision, etc&hellip;

- - - -

#### Media Player Classic Home Cinema (MPC-HC) API

* support for this API was added as an after-thought
  - introduced in [v3.6.0](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver/releases/tag/v3.6.0)
* it allows access to a subset of features and functionality from any of the readily available off-the-shelf client software
* tested with:
  - [_MpcFreemote_](https://github.com/warren-bank/Android-MpcFreemote) v3.0.0
  - [MPC-HC Remote Control](https://github.com/burdukowsky/mpc-hc-android) v1.0
  - [Remote for MPC](https://play.google.com/store/apps/details?id=com.andreformosa.playerremote) v1.2.5
  - [MPC REMOTE](https://play.google.com/store/apps/details?id=com.wethole.mpcrc) v1.10c
* the offical MPC-HC web interface [does not support any method of user authentication](https://github.com/clsid2/mpc-hc/issues/2172)
  - consequently, the [`HTTP API Password`](#http-api-password) preference in _ExoAirPlayer_ will prevent standard MPC-HC clients from being able to work properly
  - in the future, I might add support for authentication to [_MpcFreemote_](https://github.com/warren-bank/Android-MpcFreemote)&hellip; TBA

- - - -

#### Keyboard hotkeys

* `space` = toggle _play/pause_
* `N` = next track
* `P` = previous track
* `S` = stop
* `M` = toggle _on/off_ volume mute
* `V` = toggle _on/off_ text captions
* `F` = reset subtitle offset to 0
* `G` = decrease subtitle offset by 1 second (-1000000)
* `H` = increase subtitle offset by 1 second (+1000000)

note: not case sensitive, unless `SHIFT` is explicitly specified.

#### Keyboard media keys / Hardware buttons

* play
* pause
* play/pause
* stop
* previous
* next
* rewind = 5 seconds
* fast forward = 15 seconds
* captions = toggle _on/off_
* volume mute = toggle _on/off_
* volume up
* volume down

- - - -

#### Final release for `minSdkVersion`:

* [v3.4.8](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver/releases/tag/v3.4.8)
  - is the final release that supports API 16 (Android 4.1, Jelly Bean)
    * includes AndroidX Media3 [1.2.0](https://github.com/androidx/media/blob/1.2.0/constants.gradle#L17)
* [v3.8.1](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver/releases/tag/v3.8.1)
  - is the final release that supports API 21 (Android 5.0, Lollipop)
    * includes AndroidX Media3 [1.5.0](https://github.com/androidx/media/blob/1.5.0/constants.gradle#L17)
* [v3.14.5](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver/releases/tag/v3.14.5)
  - is the most recent release that supports API 23 (Android 6.0, Marshmallow)
    * includes AndroidX Media3 [1.10.1](https://github.com/androidx/media/blob/1.10.1/constants.gradle#L17)

- - - -

#### Credits:

* [AirPlay-Receiver-on-Android](https://github.com/gpfduoduo/AirPlay-Receiver-on-Android)
  - __brilliant__
  - what I like:
    * quality of code is excellent
    * implements much of the foundation for what I've described
      - [media player](https://github.com/yixia/VitamioBundle) used to render video URLs
      - _HttpCore_ web server that implements all _AirPlay_ video APIs
      - _jmDNS_ Bonjour registration
  - what I dislike:
    * all libraries are several years old
    * doesn't use _ExoPlayer_
    * the repo includes a lot of unused code
      - needs a little housekeeping

- - - -

#### Legal:

* copyright: [Warren Bank](https://github.com/warren-bank)
* license: [GPL-2.0](https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt)
