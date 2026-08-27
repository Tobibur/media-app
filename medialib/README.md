# medialib

Headless Media3 client. No Compose, no ExoPlayer. Discover sources, browse a
source's library, and send transport commands. The source app plays the audio.

## Add the module

```kotlin
// settings.gradle.kts
include(":medialib")

// app/build.gradle.kts
implementation(project(":medialib"))
```

The consuming app must still declare `MEDIA_CONTENT_CONTROL` (and install as
priv-app on AAOS) if it needs car-wide control. `<queries>` for Media3 services
are merged from this library's manifest.

## Usage

```kotlin
val hub = MediaHub.create(context)

// Source list
val sources = hub.sources.list()

// Connect one source (releases any previous connection)
hub.session.connect(sources.first())

// Browse
val root = hub.library.root().getOrThrow()
val children = hub.library.children(root.mediaId).getOrThrow()

// Play controller
hub.playback.play(children.first { it.isPlayable })
hub.playback.togglePlayPause()
hub.playback.skipNext()
hub.playback.skipPrevious()
hub.playback.seekTo(30_000L)

// Observe
hub.session.state        // StateFlow<ConnectionState>
hub.playback.nowPlaying  // StateFlow<NowPlayingState>

hub.release()
```

Gate transport buttons on `NowPlayingState.canPlayPause` / `canSkipNext` /
`canSkipPrevious` / `canSeek`. Do not assume every source supports seek or skip.

## Public API

| Surface | Type | Role |
|---|---|---|
| `hub.sources` | `SourceCatalog` | `list()` installed library/session services |
| `hub.session` | `SessionController` | `connect` / `disconnect` / `state` |
| `hub.library` | `LibraryBrowser` | `root()` / `children(parentId)` |
| `hub.playback` | `PlaybackController` | play item, play/pause/skip/seek, `nowPlaying` |

Media3 types (`SessionToken`, `MediaBrowser`) stay inside `internal` classes.
Consumers never construct them.

## What this is not

- Not a player. No `ExoPlayer`, no URIs, no playlists.
- Not a UI toolkit. Screens live in `:app` or any other consumer.
