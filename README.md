# VdC-Music

A fast music player for Android with first-class Android Auto support for local music and jellyfin.

📖 [Read the story behind Mellow](https://blog.marathonlabs.io/blog/dogfooding-emu-building-mellow/)

<p align="center">
  <img src="docs/screenshot-album.png" width="240" alt="Album detail" />

</p>

<p align="center">
  <img src="docs/screenshot-tablet.png" alt="Tablet layout" />
</p>

## Features
*local library and jellyfin support
*android auto
*lyrics support and downloader
*in app downloader
*metadata editor
*10 band equalizer
*speed changer
*playlist support

## Tech stack

Kotlin · Jetpack Compose · Material 3 · Media3 (ExoPlayer)
Hilt · Room · Coil · WorkManager · jellyfin-sdk-kotlin

## Build from source

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Android Auto

Since VdC-Music is installed outside the Play Store, you need to enable unknown sources in Android Auto:

1. Open the **Android Auto** app on your phone
2. Tap **Settings** → scroll to **Version** → tap it **10 times** to enable developer mode
3. Tap the **⋮** overflow menu (top-right) → **Developer settings**
4. Enable **Unknown sources**

## License

Apache License 2.0. See [LICENSE](LICENSE).
