# AAOS OEM Media Center

Privileged Automotive OS media hub: discovers installed `MediaLibraryService` sources, browses their libraries, and drives transport controls via Media3 `MediaBrowser`.

This app does **not** play audio itself. It is a remote control with a file browser. The source app (Spotify, radio, or the bundled sample) owns ExoPlayer, URIs, DRM, and playlists.

```
[Media Center UI] --MediaBrowser--> [MediaLibraryService] --ExoPlayer--> speaker
         ▲                                    │
         └──────── metadata / play state ─────┘
```

The hub sends a **media id**. The source looks up the real URI and plays it.

## Contents

- [Modules](#modules)
- [Build](#build)
- [Emulator (normal install)](#emulator-normal-install)
- [Privileged install](#privileged-install-oem--userdebug-emulator)
- [Unit tests](#unit-tests)
- [Smoke checklist](#smoke-checklist)
- [Architecture](#architecture)
- [Dependencies](#dependencies)
- [How source discovery works](#how-source-discovery-works)
- [How connection works](#how-connection-works)
- [How browsing works](#how-browsing-works)
- [How playback control works](#how-playback-control-works)
- [Now Playing sync](#now-playing-sync)
- [AAOS-specific pieces](#aaos-specific-pieces)
- [Implement this yourself](#implement-this-yourself)
- [Key files](#key-files)

## Modules

| Module | Package | Role |
|---|---|---|
| `:app` | `com.oem.mediacenter` | Hub UI. Discovers sources, browses, sends play/pause/next/seek. **No ExoPlayer.** |
| `:samplemedia` | `com.oem.samplemedia` | Fake media app. Owns `ExoPlayer`, exposes `MediaLibraryService`, serves two demo MP3 streams. |

Install both on an emulator. The hub has nothing to control without at least one library service.

## Build

```bash
export ANDROID_HOME=/path/to/Android/sdk
./gradlew :app:assembleDebug
```

## Emulator (normal install)

1. Boot an **Automotive** AVD (API 30–34+).
2. Install the bundled sample source and the media center:

```bash
./gradlew :samplemedia:assembleDebug :app:assembleDebug
adb install -r samplemedia/build/outputs/apk/debug/samplemedia-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

3. Launch **Media Center**, confirm **Sample Media Source** appears, browse Demo Tracks, play, and use transport controls.

Without privileged install, discovery/control of other apps may be limited.

## Privileged install (OEM / userdebug emulator)

`MEDIA_CONTENT_CONTROL` is signature|privileged. For full car-wide control:

```bash
adb root
adb remount
adb shell mkdir -p /system/priv-app/MediaCenter
adb push app/build/outputs/apk/debug/app-debug.apk /system/priv-app/MediaCenter/MediaCenter.apk
adb push app/src/main/res/xml/privapp_permissions_mediacenter.xml \
  /etc/permissions/privapp-permissions-mediacenter.xml
adb reboot
```

Template whitelist: [`app/src/main/res/xml/privapp_permissions_mediacenter.xml`](app/src/main/res/xml/privapp_permissions_mediacenter.xml).

## Unit tests

```bash
./gradlew :app:testDebugUnitTest
```

## Smoke checklist

- [ ] Sources lists sample media app(s)
- [ ] Browse shows root children; folders navigate
- [ ] Playable item starts playback in the source app
- [ ] Play/pause/next/previous/seek update Now Playing
- [ ] Switching sources releases the previous session
- [ ] Force-stopping the source shows disconnect UI (no crash)

---

## Architecture

```
UI (Compose screens)
        │
        ▼
MediaCenterViewModel     ← sources list, browse tree, now playing
        │
        ▼
MediaCenterRepository    ← thin facade
        │
        ├── SourceDiscovery          → SessionToken.getAllServiceTokens()
        └── ActiveSessionManager     → one MediaBrowser connection
                    │
                    ├── getLibraryRoot / getChildren     (browse)
                    ├── setMediaItem / play / pause      (control)
                    └── CarMediaSourceSync               (tell AAOS which source)
```

Wiring is a manual container in `AppContainer`, not Hilt. `MediaCenterApp` creates it; `MainActivity` passes `repository` into the ViewModel factory.

Screens:

1. **Sources** — grid of discovered media apps
2. **Browse** — folder/track tree for the connected source
3. **Now Playing** — title, seek bar, play/pause/next/previous
4. **MiniPlayer** — overlay on Sources/Browse while something is playing

## Dependencies

### Media Center (`:app`)

| Dependency | Why |
|---|---|
| Compose BOM + Material3 + icons | UI |
| Navigation Compose | `sources` → `browse/{package}` → `nowplaying` |
| Lifecycle ViewModel / runtime-compose | ViewModel + `collectAsStateWithLifecycle` |
| `media3-session` + `media3-common` 1.5.0 | `SessionToken`, `MediaBrowser`, `MediaItem`, `Player` commands |
| `kotlinx-coroutines-guava` | Media3 returns Guava `ListenableFuture`; `.await()` makes them suspend |
| `android.car.jar` (`compileOnly`) | Optional AAOS `CarMediaManager`. Reflection at runtime so the app still builds on a phone SDK |
| JUnit + coroutines-test | Discovery filters and browse mapping |

There is **no** `media3-exoplayer` in `:app`. That is intentional.

### Sample source (`:samplemedia`)

| Dependency | Why |
|---|---|
| `media3-exoplayer` | Plays the HTTP MP3s |
| `media3-session` | `MediaLibraryService` + `MediaLibrarySession` so the hub can connect |

### Manifest pieces

The hub is automotive, requests `MEDIA_CONTENT_CONTROL`, and uses `<queries>` so package visibility on Android 11+ can see other media services:

```xml
<queries>
    <intent>
        <action android:name="androidx.media3.session.MediaLibraryService" />
    </intent>
    <intent>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent>
    <intent>
        <action android:name="android.media.browse.MediaBrowserService" />
    </intent>
</queries>
```

The sample source **exports** a service with those same actions. That is how discovery finds it.

## How source discovery works

Do not hard-code package names. Ask Media3 for every installed session service.

### 1. Ask the system

`SourceDiscovery.discover()` calls:

```kotlin
SessionToken.getAllServiceTokens(context)
```

That scans installed packages for services advertising:

- `androidx.media3.session.MediaLibraryService`
- `androidx.media3.session.MediaSessionService`
- (legacy) `android.media.browse.MediaBrowserService`

`<queries>` in the hub manifest is required on Android 11+ or the list is empty.

### 2. Keep only services you can browse

| Token type | Kept? |
|---|---|
| `TYPE_LIBRARY_SERVICE` | Yes — browsable tree |
| `TYPE_SESSION_SERVICE` | Yes — can connect as a controller |
| `TYPE_SESSION` | No — already-running session, not a bindable service |

### 3. Map to a UI row

Each token becomes a `MediaSource`:

- `packageName` — e.g. `com.oem.samplemedia`
- `serviceName` — e.g. `com.oem.samplemedia.SampleLibraryService`
- `label` — from `PackageManager.getApplicationLabel()`
- `token` — needed later to connect

The ViewModel calls `discoverSources()` on launch and shows `SourcesScreen`. `tokenProvider` is injectable so unit tests do not need a real `PackageManager`.

## How connection works

Tapping a source calls `viewModel.connect(source)` and navigates to Browse.

`ActiveSessionManager.connect()`:

1. Releases any previous `MediaBrowser` (switching sources must not leak sessions).
2. Sets state to `Connecting`.
3. Builds the client:

```kotlin
val future = MediaBrowser.Builder(context, source.token)
    .setListener(object : MediaBrowser.Listener {})
    .buildAsync()
val browser = future.await()
```

`MediaBrowser` is a **remote controller**. After `await()`, you are bound to the other app’s `MediaLibraryService`.

4. Attaches a `Player.Listener` so metadata and play state flow back.
5. Sets `Connected(packageName)`.
6. Optionally tells the car which source is active (`CarMediaSourceSync`).
7. Starts a 500 ms ticker to refresh seek-bar position.

If connect fails, state is `Failed(message)`. If the source is force-stopped, the ticker sees `!browser.isConnected` and sets `Disconnected`.

**Rule:** hold exactly one `MediaBrowser`. Connect → use it → `release()` before connecting another.

## How browsing works

A Media3 library is a tree of `MediaItem`s, not a REST API.

```
root  (Sample Library)
 └── folder_tracks  (Demo Tracks)     ← browsable folder
      ├── track_1                     ← playable
      └── track_2                     ← playable
```

### Hub side

1. `browser.getLibraryRoot()` → root `mediaId`
2. `browser.getChildren(parentId, page = 0, pageSize = 50)` → list
3. Map each `MediaItem` to `BrowseNode`:
   - `isBrowsable` → folder, tap to go deeper
   - `isPlayable` → track, tap to play
4. ViewModel keeps `pathIds` / `pathTitles` as a breadcrumb stack so Back pops one folder instead of leaving the screen.

### Source side (`SampleLibraryService`)

Implement the other end of the same protocol:

| Callback | Return |
|---|---|
| `onGetLibraryRoot()` | Root item |
| `onGetChildren("root")` | `[Demo Tracks folder]` |
| `onGetChildren("folder_tracks")` | `[track_1, track_2]` |
| `onGetItem(mediaId)` | One track |
| `onSetMediaItems()` | Replace stub items with full items that have URIs |

That last callback matters. The hub only sends a `MediaItem` with an id (`track_1`). The source looks up the HTTPS URI and gives ExoPlayer something it can play.

## How playback control works

The hub never has an `ExoPlayer`. Commands go through the same `MediaBrowser`, which implements Media3’s `Player` interface over Binder.

### Start a track

```kotlin
val item = MediaItem.Builder().setMediaId(node.mediaId).build()
browser.setMediaItem(item)
browser.prepare()
browser.play()
```

Sequence:

1. Hub: `setMediaItem(id only)` → `prepare()` → `play()`
2. Source: `onSetMediaItems` fills in the HTTPS URI
3. Source ExoPlayer starts playing
4. Source `MediaLibrarySession` publishes metadata + playback state
5. Hub `Player.Listener` + 500 ms ticker update `NowPlayingState`

### Transport buttons

| UI action | API |
|---|---|
| Play / Pause | `if (browser.isPlaying) pause() else play()` |
| Next | `seekToNextMediaItem()` |
| Previous | `seekToPreviousMediaItem()` |
| Seek bar | `seekTo(positionMs)` |

Enable buttons from `browser.availableCommands`, do not guess:

- `COMMAND_PLAY_PAUSE`
- `COMMAND_SEEK_TO_NEXT` or `COMMAND_SEEK_TO_NEXT_MEDIA_ITEM`
- `COMMAND_SEEK_TO_PREVIOUS` or `COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM`
- `COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM`

A live radio stream often has no seek. A single-track demo may have no next. The UI greys those out.

Now Playing and MiniPlayer call the same ViewModel methods. One session, two views.

## Now Playing sync

`NowPlayingState` is a snapshot: title, artist, playing, position, duration, which buttons work.

It is refreshed:

1. On every `Player.Listener.onEvents` (play/pause, track change, commands change)
2. Every 500 ms while connected (so the seek bar moves)

Compose collects that `StateFlow`. The UI does not poll.

## AAOS-specific pieces

**`CarMediaSourceSync`** uses reflection to call `CarMediaManager.setMediaSource(component, PLAYBACK)`. That tells the car this is the active media app so steering-wheel keys and the system media widget stay in sync. If `android.car` is missing, it logs and continues.

**`MEDIA_CONTENT_CONTROL`** is `signature|privileged`. A normal `adb install` of the hub may only fully control apps that allow it. OEM install is `priv-app` plus the whitelist XML. For learning on an emulator, the sample source is enough without privilege.

**`distractionOptimized=true`** marks the Activity as safe to show while driving.

**`TouchMin = 64.dp`** is a large hit target for car screens.

## Implement this yourself

Do it in this order. Each step is testable before the next.

1. **Sample source first**  
   A `MediaLibraryService` with `ExoPlayer` and a hard-coded tree (root → folder → 2 tracks with public MP3 URLs). Export the service with the Media3 library action. Install it.

2. **Discovery**  
   `SessionToken.getAllServiceTokens(context)` → filter library/session types → show labels. Confirm the sample appears.

3. **Connect**  
   `MediaBrowser.Builder(context, token).buildAsync().await()`. Log `browser.isConnected`. Release on switch.

4. **Browse**  
   `getLibraryRoot()` then `getChildren(id)`. Render folders vs tracks from `isBrowsable` / `isPlayable`. Keep a path stack for Back.

5. **Play**  
   `setMediaItem(MediaItem with mediaId)` → `prepare()` → `play()`. In the source, implement `onSetMediaItems` to attach the real URI.

6. **Controls + now playing**  
   `play()` / `pause()` / `seekToNextMediaItem()` / `seekTo()`. Listen with `Player.Listener`. Gate buttons on `availableCommands`. Tick position every ~500 ms.

7. **Polish**  
   Mini player, reconnect last package via `SavedStateHandle`, disconnect UI, optional `CarMediaManager`.

## Key files

The three files that are the whole engine:

| File | Responsibility |
|---|---|
| `app/.../discovery/SourceDiscovery.kt` | Find sources |
| `app/.../session/ActiveSessionManager.kt` | Connect, browse, play, pause, seek |
| `samplemedia/.../SampleLibraryService.kt` | What a source must implement |

Everything else is Compose around those three.
