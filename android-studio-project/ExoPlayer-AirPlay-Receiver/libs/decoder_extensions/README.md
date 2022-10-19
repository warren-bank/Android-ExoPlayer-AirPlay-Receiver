#### origin for pre-built native extension binaries:

* [Just Player](https://github.com/moneytoo/Player/tree/v0.123/app/libs)
  - release: [v0.123](https://github.com/moneytoo/Player/releases/tag/v0.123) (using [ExoPlayer 2.18.1](https://github.com/google/ExoPlayer/releases/tag/r2.18.1))
  - extensions:
    * [_av1_](https://github.com/moneytoo/Player/raw/v0.123/app/libs/extension-av1-release.aar)
    * [_ffmpeg_](https://github.com/moneytoo/Player/raw/v0.123/app/libs/extension-ffmpeg-release.aar)
  - ABIs:
    * armeabi-v7a
    * arm64-v8a
    * x86
    * x86_64

#### _ffmpeg_ build options

```
ENABLED_DECODERS=(vorbis opus flac alac pcm_mulaw pcm_alaw mp3 amrnb amrwb aac ac3 eac3 dca mlp truehd)
```
