# Virtual Volume Rocker

A tiny Android app that replaces broken or missing hardware volume buttons with a thin, nearly transparent bar on the edge of the screen. Slide up on the bar to turn the volume up, slide down to turn it down, and the normal system volume panel appears just like it does with real buttons.

## Features

- **Edge slider:** a thin line on the left or right edge of the screen. Each step of finger travel changes the volume by one notch, with a light vibration.
- **Tap to show the panel:** a quick tap on the bar opens the system volume panel without changing the volume.
- **Same behaviour as real buttons:** it changes media volume while something is playing, call volume during a call, and ringer volume otherwise. You can also set it to always control media volume.
- **Customizable:** you can change the edge, position, length, touch width, visibility (down to fully invisible) and slide sensitivity. Changes apply straight away.
- **Always available:** it runs as an accessibility service, so it doesn't need the "draw over other apps" permission, keeps running in the background, and also works on the lock screen.
- **Private:** it doesn't read anything on your screen, needs no internet permission and collects no data.

## Install

1. Download the latest APK from the [Actions](../../actions) tab. Open the most recent successful **Build APK** run, find **Artifacts**, download **vvrock-debug-apk** and extract `app-debug.apk` from the zip.
2. Install it on your phone. You'll need to allow installing from unknown sources, and tap **Install anyway** if Play Protect warns you.
3. Open **Virtual Volume Rocker**, tap **Open Accessibility settings**, and turn on **Virtual Volume Rocker**.
   - **Android 13 and newer:** if the switch is greyed out, go to **App info → ⋮ → Allow restricted settings** and try again.
   - **Xiaomi, Samsung, Oppo, Vivo and similar phones:** set the app's battery setting to **Unrestricted** so the phone doesn't turn the service off.

## Build from source

You need JDK 17 and the Android SDK, or just Android Studio.

- **Android Studio:** open the project folder, then go to **Build → Build APK(s)**.
- **Command line:** run `gradle assembleDebug` with Gradle 8.11.1.
- **GitHub Actions:** every push builds a debug APK automatically, using [`.github/workflows/build.yml`](.github/workflows/build.yml).

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

It needs Android 8.0 (API 26) or newer and targets API 35.

## How it works

| File | Purpose |
| --- | --- |
| `EdgeVolumeService.kt` | Accessibility service that places the bar on screen and changes the volume through `AudioManager` |
| `EdgeBarView.kt` | Draws the bar and turns finger movement into volume steps |
| `MainActivity.kt` | Settings screen |
| `Prefs.kt` | Setting keys and default values |

---

Made with ❤️ by [Khalid Utsob](https://khalidutsob.com)
