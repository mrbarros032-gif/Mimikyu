# Mimikyu App: Frozen Features Registry

> **STRICT COMPLIANCE RULE**:
> All features and implementations listed in this registry are **frozen and production-locked**.
> Under NO circumstances should any listed feature be modified, refactored, re-architected, or deleted unless the user **explicitly names the specific feature and instructs its modification in the prompt**.
> Any modification must ONLY touch the isolated lines requested by the user, leaving every other feature intact.

---

## 1. Authentication & Onboarding
- **GetStartedScreen** (`feature/home/ui/GetStartedScreen.kt`):
  - Upper background video section (`0.62f` screen height) looping muted (`volume = 0f`).
  - Seamless vertical gradient overlay with AMOLED black bleeding upward from behind the text over the bottom half of the video.
  - Headline: "Music without borders", subtitle, and capsule "Get started" button.
- **LoginBottomSheet** (`core/ui/components/LoginBottomSheet.kt`):
  - Bottom sheet for user authentication with two official sign-in paths:
    1. **Google Login** (`GoogleLoginButton` with official Google OAuth/Credential Manager).
    2. **Email & Password** (`EmailLoginContent` with sign-in and sign-up toggle).
  - NO test/developer bypass buttons (e.g., hardcoded "Continue as Nishkarsh" is permanently eliminated).

---

## 2. Audio Playback & Player Controls
- **Audio Playback Engine** (`core/player/`):
  - ExoPlayer Media3 audio pipeline with gapless playback, cache prefetching, and crossfade support.
  - Background audio playback Service and media notification integration.
- **Full Player & Mini Player** (`feature/player/ui/`):
  - Mini player pinned above bottom navigation with play/pause, like heart toggle, and swipe/tap to open full screen.
  - Full-screen player with album cover carousel, dynamic background gradient extracted from artwork (`ColorExtraction.kt`), waveform progress seek bar, playback controls, shuffle, repeat, sleep timer, and share sheet.

---

## 3. Lyrics System (Apple Music Style & TTML)
- **Accompanist Lyrics View** (`core/lyrics/AccompanistLyricsAdapter.kt`, `FullScreenLyricsList.kt`):
  - Apple Music-style dynamic synced lyrics with syllable-by-syllable karaoke fluid glow.
  - Real-time lyrics scrolling with vocal pause breathing dots (`...`) calibrated to a **3.5s (3,500ms)** gap threshold.
- **Multi-Tier Lyrics Fetching** (`core/lyrics/ExternalLyricsService.kt`):
  - Tier 0: Local disk TTML cache (`lyrics_ttml/{videoId}.ttml`).
  - Tier 1: Supabase/Zoho Cloud TTML storage and BetterLyrics word-synced TTML API.
  - Tier 2: Word-by-word KRC / YRC decoding.
  - Tier 3: LRCLIB line-synced fallback.
  - Strict language integrity: Foreign (CJK/Han) characters and translations are strictly stripped/rejected for Western/English tracks.

---

## 4. Home Screen & Discovery
- **Home Content** (`feature/home/ui/HomeContent.kt`):
  - Top category filter chips: `All`, `Liked Songs`, `Discover`, `Trending`.
  - **Daily Artist Hero Banner** (`DailyArtistHeroSection.kt`): Horizontal pager showcasing curated artist highlights (Michael Jackson, Kanye West, Tame Impala, The Weeknd, BTS) with top/bottom ambient dark gradients.
  - **Curated Mimikyu-Style Shelves** (`MimikyuHomeShelves.kt`):
    - "Your top mixes" (On Repeat, Daily Mixes, Chill/Energetic mixes).
    - "Recommended Stations".
    - "Your favourite artists": Circular artist cards strictly deduplicated by normalized name (`DailyMixGenerator.kt`).
    - "Featured Playlists for You" and "Top Albums for You".

---

## 5. Search & Discovery
- **Search Screen** (`feature/search/ui/SearchScreen.kt`):
  - Top search input bar with voice/song identifier access.
  - Discovery grid with genre and vibe tiles when query is empty.
  - Multi-tab search results: `Songs`, `Albums`, `Artists`, `Playlists`.
  - Pagination with infinite scroll and stable list keys (`stableKey`).

---

## 6. Library & Local Music
- **Your Library** (`feature/library/ui/LibraryScreen.kt`):
  - User playlists, saved albums, followed artists, and MV tabs.
  - Dedicated "Local Music" row linking to device storage scanner (`LocalMusicScreen.kt`).
  - Custom playlist creation and playlist ordering management.
  - Empty states and signed-out view with sign-in call to action.

---

## 7. Artist Profiles
- **Artist Screen** (`feature/artist/ui/ArtistScreen.kt`):
  - High-res artist cover banner resolved from YouTube Music and Last.fm (`ArtistPortraitProvider.kt`).
  - Top Songs section strictly deduplicated by song title (`distinctBy { it.name.lowercase().trim() }`), preventing duplicate tracks (e.g. multiple "Dracula" tracks).
  - Albums, Singles, Music Videos, and Artist Biography.
  - **Mimikyu Monthly Listeners Integration** (`MimikyuArtistStatsRepository.kt`, `ArtistActionBar.kt`, `ArtistAboutTab.kt`):
    - Authenticated Mimikyu monthly listeners and global rankings ingested from official Kworb/Mimikyu catalog (top 2,400 global artists).
    - Header action bar displays live Mimikyu-format listener count (`{count} monthly listeners`).
    - About tab displays verified "Mimikyu Monthly" card with exact active monthly listeners count, Last.fm scrobbler totals, and Audiometa fallback.

