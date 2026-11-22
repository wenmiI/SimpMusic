# SimpMusic for iOS

The iOS app is the same Compose Multiplatform app as Android and Desktop. This folder only holds the
shell that hosts it: a SwiftUI entry point (`iosApp/iOSApp.swift`), `Info.plist`, the asset catalog
and an [XcodeGen](https://github.com/yonaskolb/XcodeGen) spec (`project.yml`). Everything else lives
in `composeApp` (UI, `iosMain`) and `core` (data, player, sync).

## Building

You need a Mac. Kotlin/Native compiles the iOS code on any host, but only Xcode can link and sign
the app.

1. JDK 21, Xcode 16 or newer, and `brew install xcodegen`.
2. `git submodule update --init` at the repository root, if `core/` is empty.
3. `cd iosApp && xcodegen generate`, then open `SimpMusic.xcodeproj`.
4. Pick your team under Signing & Capabilities and run.

The first build runs `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode` from Xcode's
"Compile Kotlin framework" phase and takes several minutes. It builds the FOSS flavour; set
`SIMPMUSIC_FULL_BUILD=true` in the scheme's environment for a Full build (Last.fm, Discord OAuth),
which wants the same keys in `local.properties` as the other platforms.

## Without a Mac

`.github/workflows/ios.yml` builds an unsigned `SimpMusic-unsigned.ipa` on GitHub's macOS runners,
on every push to the `ios` branch or by hand from the Actions tab. Install it with
[SideStore](https://sidestore.io) or [AltStore](https://altstore.io), which sign it with your own
Apple ID as they install it. With a free Apple ID the signature lasts 7 days and the app has to be
refreshed; a paid developer account lasts a year.

## What is different on iOS

Playback is AVPlayer (`core/media/media-ios`), driven by the same handler as Desktop
(`JvmMediaPlayerHandlerImpl`, now in `core/data/src/nonAndroidMain`). The lock screen and Control
Center come from `SystemMediaControls.ios.kt`, background audio from the `audio` background mode.

Streams come from `LocalStreamResolver`: PipePipe's TV-client requests with every cipher solved on
the device. There is no remote fallback behind it, and the requests go out signed out, so:

- **Audio is AAC 128 kbps** (itag 140). AVPlayer cannot play Opus, and Premium's 256 kbps formats
  are only served to a signed-in web session.
- **Video is 360p** (itag 18, the one stream that carries picture and sound together; AVPlayer cannot
  join YouTube's separate video and audio streams).

Not on iOS: crossfade, the equalizer, delay/reverb, 8D, the SoundCloud waveform, pitch shift (speed
works), song recognition, Google Cast (the cast button is AirPlay's route picker instead), picture in
picture, the in-app device volume row, widgets, and the QR pairing (Login sync, Library sync). Their
settings are hidden on iOS (`hasAudioEngine()` in SettingScreen.kt, the pitch row in the speed sheet,
the pairing rows under Devices); the SoundCloud style draws a flat line where its waveform would be.
**Sync account works**, so an iPhone shares the library, history and downloads with the other
devices through your host.

Backups are written to the app's Documents folder, which the Files app shows as
On My iPhone › SimpMusic; restore reads any backup picked in Files, including Android and Desktop
ones. Downloads are `.m4a` files under `Documents/downloads`.

## Status

The Kotlin side compiles for `iosArm64` and `iosSimulatorArm64`, and the stream resolver was checked
against YouTube on the JVM (`LocalStreamResolverLiveTest`). The app has **not** yet been linked or
run on a device: that needs Xcode. Expect the first CI run or the first Xcode build to need fixes.
