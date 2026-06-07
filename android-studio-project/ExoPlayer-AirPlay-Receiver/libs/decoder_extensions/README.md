#### origin for pre-built native extension binaries:

* [Just Player](https://github.com/moneytoo/Player/tree/v0.212/app/libs)
  - release: [v0.212](https://github.com/moneytoo/Player/releases/tag/v0.212) (using [AndroidX Media3 1.10.1](https://github.com/androidx/media/releases/tag/1.10.1))
  - extensions:
    * [_av1_](https://github.com/moneytoo/Player/raw/v0.212/app/libs/lib-decoder-av1-release.aar) from [source](https://github.com/androidx/media/tree/1.10.1/libraries/decoder_av1)
    * [_ffmpeg_](https://github.com/moneytoo/Player/raw/v0.212/app/libs/lib-decoder-ffmpeg-release.aar) from [source](https://github.com/androidx/media/tree/1.10.1/libraries/decoder_ffmpeg)
    * [_iamf_](https://github.com/moneytoo/Player/raw/v0.212/app/libs/lib-decoder-iamf-release.aar) from [source](https://github.com/androidx/media/tree/1.10.1/libraries/decoder_iamf)
    * [_mpegh_](https://github.com/moneytoo/Player/raw/v0.212/app/libs/lib-decoder-mpegh-release.aar) from [source](https://github.com/androidx/media/tree/1.10.1/libraries/decoder_mpegh)
  - ABIs:
    * armeabi-v7a
    * arm64-v8a
    * x86
    * x86_64

#### _ffmpeg_ build options

```
ENABLED_DECODERS=(vorbis opus flac alac pcm_mulaw pcm_alaw mp3 amrnb amrwb aac ac3 eac3 dca mlp truehd)
```
