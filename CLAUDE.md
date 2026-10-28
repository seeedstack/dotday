# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

DotDay is a Kotlin Multiplatform / Compose Multiplatform habit tracker, targeting Android, iOS, and Desktop (JVM) from one shared codebase. It is a direct port of a single-file JS/HTML prototype (`DOTDAY.html`, a "Pop Press" comic-styled habit tracker) — the domain logic in `Domain.kt` mirrors that JS almost line-for-line, so when behavior seems odd, assume it's an intentional port rather than a bug.

## Commands

- Desktop run: `./gradlew :composeApp:run`
- Android debug install: `./gradlew :composeApp:installDebug`
- Build everything: `./gradlew build`
- iOS: no Xcode project is checked in. Create one named `iosApp` (or via the KMP wizard), replace its Swift files with `iosApp/iosApp/*.swift`, and add a Run Script build phase: `cd "$SRCROOT/.." && ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`.
- There is no test suite in the repo currently — don't assume one exists.
- Requires JDK 17. If `gradlew` is missing permissions or the wrapper jar is absent, regenerate with `gradle wrapper`.

## Architecture

Everything lives under `composeApp/src/`, split by Kotlin Multiplatform source sets:

- `commonMain/kotlin/com/dotday/app/domain/Domain.kt` — pure functions/data: `Habit`, due-date logic (`isDue`), streak math (`currentStreak`, `longestStreak`), completion rates (`rate`), and the `Copy` object (all user-facing microcopy/strings live here, Pop Press comic voice). Changing copy or streak/rate semantics happens here, not in the UI files.
- `commonMain/kotlin/com/dotday/app/data/AppStore.kt` — single source of truth for app state (`habits`, `logs`, settings) as Compose `mutableStateOf` properties, persisted as one JSON blob via `multiplatform-settings` (`Settings` → SharedPreferences/NSUserDefaults/java.util.prefs). Every mutation method (`toggle`, `upsert`, `delete`, `toggleArchive`, etc.) calls `save()` itself — there's no separate persistence layer to keep in sync.
- `commonMain/kotlin/com/dotday/app/ui/UiState.kt` — transient UI-only state (active tab, editing/dialog state, toasts, confetti "burst" animations) kept separate from `AppStore`'s persisted state.
- `commonMain/kotlin/com/dotday/app/ui/*.kt` — `TodayScreen`, `HabitsScreen`, `InsightsScreen`, `SettingsScreen` (one per bottom-nav tab) plus `Theme.kt` (design tokens, light/dark palettes) and `Components.kt` (comic-panel borders, buttons, halftone-dot background — the shared "Pop Press" visual language).
- `commonMain/kotlin/com/dotday/app/App.kt` — top-level composable: header/greeting, nav (bottom bar when width ≤ 600dp, otherwise a top nav), toast host, confirm dialog, and the burst/confetti overlay. Screen switching is a `when (ui.tab)` over the four screens.
- `commonMain/kotlin/com/dotday/app/platform/Backup.kt` — `expect fun rememberBackupExporter()`; platform `actual`s (`Backup.android.kt`, `.desktop.kt`, `.ios.kt`) implement "Export backup" differently per platform (share sheet on Android/iOS, writes `~/dotday-backup.json` on Desktop). Any new platform-specific capability should follow this expect/actual pattern rather than branching inside common code.
- `iosApp/iosApp/*.swift` — the SwiftUI host shell that embeds the Compose Multiplatform framework; not built by Gradle, only referenced by the Xcode project the user creates locally.

## Notable deliberate differences from the original HTML

- `<input type=date>` → plain text field expecting `YYYY-MM-DD`.
- `<select>` → inline tappable option list; `<datalist>` → category chips.
- Dark mode follows the system theme unless overridden in Settings (`AppStore.theme`: `"auto" | "dark" | "light"`).
- Local-first, single-user: all state is the one JSON blob in `AppStore`; there is no backend/network layer.
