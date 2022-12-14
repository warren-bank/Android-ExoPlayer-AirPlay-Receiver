#### origin for pre-built native extension binaries:

* [Just Player](https://github.com/moneytoo/Player/tree/v0.132/app/libs)
  - release: [v0.132](https://github.com/moneytoo/Player/releases/tag/v0.132) (using [AndroidX Media3 1.0.0-beta03](https://github.com/androidx/media/releases/tag/1.0.0-beta03))
  - extensions:
    * [_av1_](https://github.com/moneytoo/Player/raw/v0.132/app/libs/lib-decoder-av1-release.aar)
    * [_ffmpeg_](https://github.com/moneytoo/Player/raw/v0.132/app/libs/lib-decoder-ffmpeg-release.aar)
  - ABIs:
    * armeabi-v7a
    * arm64-v8a
    * x86
    * x86_64

#### _ffmpeg_ build options

```
ENABLED_DECODERS=(vorbis opus flac alac pcm_mulaw pcm_alaw mp3 amrnb amrwb aac ac3 eac3 dca mlp truehd)
```
