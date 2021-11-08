#### origin for pre-built native extension binaries:

* [Just Player](https://github.com/moneytoo/Player/tree/v0.67/app/libs)
  - release: 0.67 (using ExoPlayer 2.16.0)
  - extensions:
    * [_av1_](https://github.com/moneytoo/Player/raw/v0.67/app/libs/extension-av1-release.aar)
    * [_ffmpeg_](https://github.com/moneytoo/Player/raw/v0.67/app/libs/extension-ffmpeg-release.aar)
  - ABIs:
    * armeabi-v7a
    * arm64-v8a
    * x86
    * x86_64

#### _ffmpeg_ build options

```
ENABLED_DECODERS=(vorbis opus flac alac pcm_mulaw pcm_alaw mp3 amrnb amrwb aac ac3 eac3 dca mlp truehd)
```
