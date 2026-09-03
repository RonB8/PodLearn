# PodLingo — Claude Code Rules

Android app: Kotlin + Jetpack Compose + Material3, MVVM + Hilt.
Modules: `:app` and `:core-logic`. `core-logic` is pure Kotlin/JVM and must never depend on Android/Room.

## Critical Rules

- Never `git commit` unless explicitly asked. Consent never carries over.
- Do not guess on ambiguous/destructive UX decisions — ask first.
- Verify UI changes on the physical device when possible: build → install → launch → test. If unable, say so.
- No comments unless they explain a non-obvious WHY, workaround, or invariant.
- Prefer existing/shared components over duplicating code.

## Architecture

- ViewModels use `@HiltViewModel`.
- Repositories/managers are `@Singleton` + `@Inject constructor`.
- Manual Hilt providers only where already required, mainly `di/DatabaseModule.kt` and `di/NetworkModule.kt`.
- Tunable constants belong in `config/AppDefaults.kt`, not inline.
- Navigation: `ui/navigation/PodLingoNavHost.kt` + `Routes.kt`.
- UI is organized by feature under `ui/`.

## Database

- Room DB is currently version 5.
- Every schema change requires an explicit `MIGRATION_x_y` and version bump.
- Never use destructive migration fallback.

## Localization / RTL

- Never hardcode UI strings in Composables.
- All static/parameterized UI text goes through `ui/strings/AppStrings.kt`, implemented by both English and Hebrew.
- Hebrew uses app-wide RTL automatically.
- Keep these LTR: player controls/seek bar, mini-player, transcript/translation overlay, and Theme/Language segmented-button rows.
- Do not manually reposition UI for RTL.

## Storage / Playback

- Audio files: `filesDir/episodes/<episode-id>.mp3`.
- Storage limit is configurable via `SettingsRepository` / `AppDefaults`.
- `EpisodeStorageManager` evicts old audio only; transcripts must remain.
- If audio is missing but transcript is `READY`, `TranscriptRepository.preprocess()` must re-download audio without retranscribing, and download failure must preserve `READY`.

## Vocabulary Quiz

- Quiz logic lives in `ui/player/VocabQuizBuilder.kt` and must be reused/injected rather than duplicated.
- Quiz dialog: `ui/vocabulary/VocabQuizDialog.kt`.
- Async pause/resume flows in `PlayerViewModel` must use `resumeIfNotQuizzing()`, never directly `playerController.resume()`.

## Build / Test

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

.\gradlew.bat :app:compileDebugKotlin --console=plain
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :core-logic:test :app:testDebugUnitTest
