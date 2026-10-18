# Mimikyu Project Source of Truth

## 1. Project Identity
- **App Name:** Mimikyu
- **Package ID / Application ID:** `com.lin0721.linmusic`
- **Main Application Class:** `MimikyuApplication.kt`
- **Root Composable / UI Entry:** `MimikyuApp.kt` & `MainActivity.kt`
- **Theme Engine:** `MimikyuTheme`

## 2. Core Mimikyu Architecture & Modules (`app/src/main/java/com/lin0721/linmusic/`)
- **`MimikyuApplication.kt`**: Koin DI initialization, Coil pre-warming, and Firebase App Check.
- **`MainActivity.kt`**: Edge-to-edge window insets, deep-link handling for user profiles, and desktop lyric service triggers.
- **`MimikyuApp.kt`**: Root Material 3 Scaffold, bottom navigation bar, and miniplayer dock.
- **`MimikyuNavHost.kt`**: Animated navigation routing engine between features.
- **`core/lyrics/`**: Syllable-by-syllable TTML lyrics cascade engine.
- **`core/songlike/`**: Cloud Firestore reactive liked songs repository.
- **`core/auth/`**: Firebase Auth & Google Credential Manager integration.

## 3. STRICT RULE: DO NOT CONFUSE WITH MELODIA
- "Melodia" was the historical base template name. 
- **DO NOT** use any class names, strings, package paths, or file references containing `Melodia` (such as `MelodiaApp.kt` or `MelodiaApplication.kt`). All entry points must use the `Mimikyu` prefix.
