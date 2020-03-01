#### [ExoPlayer AirPlay Receiver](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver)
##### (less formally named: _"ExoAirPlayer"_)

Android app to run on a set-top box and play video URLs "cast" to it with a stateless HTTP API (based on AirPlay).

- - - -

#### Background:

* I use Chromecasts _a lot_
  - they are incredibly adaptable
    * though their protocol is [proprietary and locked down](https://blog.oakbits.com/google-cast-protocol-receiver-authentication.html)
  - though the [Google Cast SDK for Android](https://developers.google.com/cast/docs/android_sender) is nearly ubiquitous, I very rarely cast video from apps
  - I find much better video content to stream on websites, and wrote some tools to identify and cast these URLs
    * [Chrome extension](https://github.com/warren-bank/crx-webcast-reloaded) to use with desktop web browsers
    * [Android app](https://github.com/warren-bank/Android-WebCast) to use with mobile devices
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
    * to add support for: video queue, next, previous, mute, set volume

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

  # file path for test image:
  image_path='/path/to/image.jpg'

  # URL for test image:
  image_page='https://commons.wikimedia.org/wiki/File:Android_robot.svg'
  image_url='https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/Android_robot.svg/654px-Android_robot.svg.png'

  # URLs for test videos:
  videos_page='https://players.akamai.com/hls/'
  video_url_1='https://multiplatform-f.akamaihd.net/i/multi/will/bunny/big_buck_bunny_,640x360_400,640x360_700,640x360_1000,950x540_1500,.f4v.csmil/master.m3u8'
  video_url_2='https://multiplatform-f.akamaihd.net/i/multi/april11/hdworld/hdworld_,512x288_450_b,640x360_700_b,768x432_1000_b,1024x576_1400_m,.mp4.csmil/master.m3u8'
  video_url_3='https://multiplatform-f.akamaihd.net/i/multi/april11/cctv/cctv_,512x288_450_b,640x360_700_b,768x432_1000_b,1024x576_1400_m,.mp4.csmil/master.m3u8'

  # URLs for test video text captions:
  captions_page='https://demos.flowplayer.com/basics/subtitle.html'
  caption_url_1='https://d12zt1n3pd4xhr.cloudfront.net/fp/subtitles-demo.vtt'
  caption_url_2='https://d12zt1n3pd4xhr.cloudfront.net/fp/subtitles-demo.vtt'
  caption_url_3='https://d12zt1n3pd4xhr.cloudfront.net/fp/subtitles-demo.vtt'
```

* display image from local file system:
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
* play video #1 (add text captions, set 'Referer' request header, seek to beginning):
  ```bash
    curl --silent -X POST \
      -H "Content-Type: text/parameters" \
      --data-binary "Content-Location: ${video_url_1}\nCaption-Location: ${caption_url_1}\nReferer: ${videos_page}\nStart-Position: 0" \
      "http://${airplay_ip}/play"
  ```
* seek to `30 seconds` within currently playing video:
  ```bash
    curl --silent -X POST -d "" \
      "http://${airplay_ip}/scrub?position=30.0"
  ```
* pause the currently playing video:
  ```bash
    curl --silent -X POST -d "" \
      "http://${airplay_ip}/rate?value=0.0"
  ```
* resume playback of the currently paused video:
  ```bash
    curl --silent -X POST -d "" \
      "http://${airplay_ip}/rate?value=1.0"
  ```
* increase speed of playback to 10x:
  ```bash
    curl --silent -X POST -d "" \
      "http://${airplay_ip}/rate?value=10.0"
  ```
* stop playback:
  ```bash
    curl --silent -X POST -d "" \
      "http://${airplay_ip}/stop"
  ```

__extended APIs:__

* add video #2 to end of queue (add text captions, set 'Referer' request header, seek to 50%):
  ```bash
    # note: position < 1 is a percent of the total video length
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
* skip forward to next video in queue:
  ```bash
    curl --silent -X POST -d "" \
      "http://${airplay_ip}/next"
  ```
* skip backward to previous video in queue:
  ```bash
    curl --silent -X POST -d "" \
      "http://${airplay_ip}/previous"
  ```
* mute audio:
  ```bash
    curl --silent -X POST -d "" \
      "http://${airplay_ip}/volume?value=0.0"
  ```
* set audio volume to 50%:
  ```bash
    curl --silent -X POST -d "" \
      "http://${airplay_ip}/volume?value=0.5"
  ```
* set audio volume to 100%:
  ```bash
    curl --silent -X POST -d "" \
      "http://${airplay_ip}/volume?value=1.0"
  ```
* turn text captions off:
  ```bash
    curl --silent -X POST -d "" \
      "http://${airplay_ip}/captions?toggle=0"
  ```
* turn text captions on:
  ```bash
    curl --silent -X POST -d "" \
      "http://${airplay_ip}/captions?toggle=1"
  ```

#### Usage (high level):

* [single-page application (SPA)](http://webcast-reloaded.surge.sh/airplay_sender.html) that can run in any web browser, and be used to:
  - send commands to a running instance of [ExoPlayer AirPlay Receiver](https://github.com/warren-bank/Android-ExoPlayer-AirPlay-Receiver)
    * "cast" video URLs to its playlist
    * control all aspects of playback

* [Chrome extension](https://github.com/warren-bank/crx-webcast-reloaded) that can run in any Chromium-based desktop web browser, and be used to:
  - intercept the URL of (nearly) all videos on any website
  - display these video URLs as a list of links
    * clicking on any link will transfer the URL of the video (as well as the URL of the referer webpage) to the [SPA](http://webcast-reloaded.surge.sh/airplay_sender.html) (above)
      - more precisely, the link to the SPA is displayed as a small [AirPlay icon](https://github.com/warren-bank/crx-webcast-reloaded/raw/v0.6.0/chrome_extension/data/airplay.png)
      - the other links transfer the video URL to other tools
        * webpage to watch the video in an HTML5 player with the ability to "cast" the video to a Chromecast
        * a running instance of [HLS-Proxy](https://github.com/warren-bank/HLS-Proxy)

* [Android app](https://github.com/tutikka/DroidPlay) that is open-source, and can be used to:
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
    * implements 99% of what I've described
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
