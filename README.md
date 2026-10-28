# DOTDAY

A habit tracker with a "Pop Press" comic look — halftone dots, thick panel borders, Anton/Work Sans type. One Kotlin Multiplatform codebase, native on **Android, iOS, and Desktop (JVM)**. Local-first: no backend, no accounts, no network calls — everything lives in one JSON blob on-device.

Port of the single-file prototype `DOTDAY.html`; `Domain.kt` mirrors its logic near line-for-line.

## What it does (features)

- **Today tab** — the day's due habits as a checklist; tap to log/unlog, with a confetti "burst" animation and streak copy on completion.
- **Habits tab** — create/edit/archive/delete habits: name, category, color, custom weekday schedule (`sched()` renders "Mon/Wed/Fri" etc.), start date.
- **Insights tab** — completion `rate()` over a date range, `currentStreak()` / `longestStreak()` per habit, week-by-week navigation (`weekOf`, `weekOff`).
- **Settings tab** — theme (`auto` / `dark` / `light`), week-start day (Mon/Sun), "calm mode" (reduced motion/intensity), demo-data seeding (`loadDemo`), wipe all data, **export backup** (JSON — share sheet on Android/iOS, `~/dotday-backup.json` on Desktop).
- **Toasts, confirm dialogs, and burst/confetti overlays** are app-wide, driven by `UiState` rather than per-screen.
- Responsive nav: bottom bar on narrow widths (≤600dp), top nav bar otherwise — same screens, same state.

## Layout

```
composeApp/src/commonMain/kotlin/com/dotday/app/
  domain/Domain.kt      pure logic: Habit, isDue, currentStreak, longestStreak, rate, weekOf, Copy (all user-facing strings)
  data/AppStore.kt       single source of truth — habits/logs/settings as Compose state, persisted via multiplatform-settings
  ui/UiState.kt          transient UI-only state: active tab, editing/dialog state, toasts, bursts
  ui/Theme.kt            DotColors, DotFonts, light/dark palettes, hex()
  ui/Components.kt       comic-panel primitives: Panel, DButton, Chip, DField, Check, SelectField, T/H2/Meta, halftone()
  ui/TodayScreen.kt ui/HabitsScreen.kt ui/InsightsScreen.kt ui/SettingsScreen.kt
  App.kt                 header, nav switch, toast host, confirm dialog, burst overlay
  platform/Backup.kt     expect fun rememberBackupExporter() — per-platform actuals for export
composeApp/src/{android,desktop,ios}Main/...   platform entry points + Backup actuals
composeApp/src/commonMain/composeResources/font  Anton + Work Sans (OFL)
iosApp/iosApp/*.swift   SwiftUI host shell (not Gradle-built; wire up via Xcode)
```

## Run

Requires JDK 17. Generate the wrapper once if missing: `gradle wrapper`.

- Desktop: `./gradlew :composeApp:run`
- Android: `./gradlew :composeApp:installDebug`
- iOS: create an Xcode project named `iosApp` (or via the KMP wizard), replace its Swift files with `iosApp/iosApp/*.swift`, add a Run Script build phase: `cd "$SRCROOT/.." && ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`.
- Full build: `./gradlew build`. No test suite exists yet.

## Where to touch things for a better UI / an addon

Everything visual funnels through `ui/Components.kt` and `ui/Theme.kt` — change those two files and every screen picks it up, since `TodayScreen`/`HabitsScreen`/`InsightsScreen`/`SettingsScreen` are all built from the same primitives (`Panel`, `DButton`, `Chip`, `T`/`H2`, `halftone()`). Don't restyle screens one-by-one; restyle the primitive.

- **New colors/typography** → `ui/Theme.kt` (`DotColors`, `DotFonts`, `hex()`). Add a palette variant here, not inline `Color(...)` calls in screens.
- **New reusable widget** (e.g. a progress ring, a tag picker) → add it to `ui/Components.kt` alongside the existing primitives, follow the same `comic()`-border / halftone-dot visual language already there.
- **New screen/tab** → add to the `Tab` enum in `ui/UiState.kt`, add a `when (ui.tab)` branch in `App.kt`, add the screen file under `ui/`. Nav (bottom bar vs. top bar) is handled once in `App.kt`, not per-screen.
- **New transient UI state** (dialogs, selection, animation flags) → `ui/UiState.kt`, not screen-local `remember {}`, so it survives tab switches and toasts/confirms can reach it from anywhere.
- **New persisted data/settings** → add a field to `Persisted` and `AppStore` in `data/AppStore.kt`; every mutator must call `save()` itself (there's no separate persistence layer).
- **New copy/microcopy** → the `Copy` object in `domain/Domain.kt`, to keep the Pop Press voice in one place.
- **New platform-specific capability** (share sheet, file picker, notifications, widgets) → follow the `expect`/`actual` pattern used by `platform/Backup.kt` rather than branching on platform inside common code.

## Notable deliberate differences from the original HTML

- `<input type=date>` → plain text field expecting `YYYY-MM-DD`.
- `<select>` → inline tappable option list; `<datalist>` → category chips.
- Dark mode follows the system theme unless overridden in Settings.
