# W′ Race — Developer Handoff

**Project**: Karoo-Wprime-Race
**Current version**: 1.3 (versionCode 4)
**Repo**: https://github.com/j4m1eb/Karoo-Wprime-Race
**Owner / primary user**: j4m1eb (also the rider testing it in actual races)
**Platform**: Hammerhead Karoo 2 / Karoo 3 cycling computer (Android-based)
**Stack**: Kotlin, Jetpack Compose, Glance for RemoteViews, Karoo Extensions SDK

---

## 1. What This App Does

A Karoo cycling computer extension that tracks a rider's **W′ balance** (anaerobic capacity) in real time and displays it against a **target pacing curve** for time trials and criterium races.

W′ depletes when riding above Critical Power (CP) and recovers when below. The app uses the Karoo's live power data to maintain a running W′ balance, then compares it to where the rider *should* be at this point in the race based on either a linear curve (TT) or a step function with phase floors (Crit).

---

## 2. The Five Data Fields

Each is a `KarooExtension` `DataType` registered in `extension_info.xml`. All five must appear there or they won't show in the Karoo's field picker.

| Field | typeId | Purpose |
|---|---|---|
| W′ TT | `wprime-tt` | Actual W′ vs linear target curve (100% → 0% over duration) |
| W′ Crit | `wprime-crit` | Actual W′ vs step-function phase floor |
| W′ Usable | `wprime-crit-usable` | W′ headroom above current phase floor (% or kJ) |
| W′ Time to Floor | `wprime-crit-time` | Seconds at current 3s power until W′ hits the phase floor |
| W′ Time to Empty | `wprime-crit-zero` | Seconds at current 3s power until W′ hits 0% (going pop) |

Each field renders in single-width or double-width layouts via Glance.

---

## 3. Architecture

### Module structure
- **`app/`** — Android application module: extension entry point, data types, settings UI, Glance views
- **`lib/`** — Shared library module: W′ models, math, domain types

### Key packages (in `app/`)
- `com.j4m1eb.wprimerace.extension` — DataType implementations, the `KarooExtension` subclass
- `com.j4m1eb.wprimerace.settings` — `WPrimeRaceSettings` (DataStore wrapper), `WPrimeRaceConfig` (data class)
- `com.j4m1eb.wprimerace.screens` — Compose UI for settings (`MainScreen`, `CritCurveEditor`)
- `com.j4m1eb.wprimerace.ui` — Glance views for the data fields (`WPrimeRaceView`)

### Dependency injection
Koin. `WPrimeRaceExtension` injects `KarooSystemService` and `WPrimeRaceSettings`. Modules wired up in the Application class.

### Settings persistence
Android DataStore Preferences. Each value has a `Preferences.Key<T>` and gets read via a `Flow<WPrimeRaceConfig>` that emits whenever any backing key changes. Settings update live — fields receive the new config within one refresh cycle.

### Power input
Subscribed via `karooSystem.streamDataFlow(DataType.Type.SMOOTHED_3S_AVERAGE_POWER)`. The 3s smoothing means the field is responsive but not jittery on every pedal stroke.

### Elapsed time
Subscribed via `streamDataFlow(DataType.Type.ELAPSED_TIME)`. **Critical gotcha**: Karoo emits this in **milliseconds**, not seconds. The codebase divides by 1000.0 — don't undo this.

There's an `elapsedOffset` pattern: each DataType records the first elapsed value it sees, then subtracts it from subsequent readings. This anchors pacing to recording start, so a long warm-up before pressing record doesn't corrupt the curve.

---

## 4. Domain Model

### `WPrimeRaceConfig` (the master settings object)
```kotlin
data class WPrimeRaceConfig(
    val cpWatts: Int,
    val wPrimeKj: Double,
    val ttDurationMin: Int,
    val critDurationMin: Int,
    val showArrow: Boolean,
    val model: WPrimeModel,                   // enum
    val critCurve: List<CritCurvePoint>,      // 4 interior breakpoints
    val ttShowKj: Boolean,
    val critShowKj: Boolean,
    val usableShowKj: Boolean,
)
```

### `CritCurvePoint`
```kotlin
data class CritCurvePoint(val racePct: Double, val wPrimePct: Double)
```

Default curve: `(43, 70), (71, 50), (97, 30), (99, 10)`. Start (0%, 100%) and finish (100%, 0%) are implicit and not editable.

### `WPrimeModel` (enum)
- `SKIBA_DIFFERENTIAL` — default, fastest, continuous recovery from W′ deficit
- `SKIBA_2012` — original Skiba formulation, simpler
- `BARTRAM` — recovery rate scales with power below CP
- `CHORLEY_BIEXP` — dual-exponential (fast/slow recovery components), most physiologically accurate

All four implementations live in the `lib/` module and conform to a common interface: given previous W′ balance, current power, CP, W′ total, and dt, return new W′ balance.

---

## 5. The Step Function (Critical Design Decision)

**This was the major rewrite of v1.2.** Originally the crit target was linearly interpolated between breakpoints, which produced a smoothly declining target. The user (an actual racer) found this useless: "don't go below 98% W′ at minute 5" is not actionable in a real crit where every cover and surge bites into W′.

The redesign: **the crit target is a step function that holds constant across each phase and jumps down at the boundary.**

```kotlin
// in WPrimeCritFloor.kt
fun critPhaseFloorFraction(
    elapsedSec: Double,
    raceDurationSec: Double,
    curve: List<CritCurvePoint>,
): Double {
    if (raceDurationSec <= 0.0) return curve.firstOrNull()?.wPrimePct?.div(100.0) ?: 1.0
    val progressPct = (elapsedSec / raceDurationSec).coerceIn(0.0, 1.0) * 100.0
    for (point in curve) {
        if (progressPct < point.racePct) return point.wPrimePct / 100.0
    }
    return 0.0
}
```

This is the **single source of truth for crit phase floors**. All three crit fields call it:
- `WPrimeCritDataType` uses it as the target percentage
- `WPrimeCritUsableDataType` subtracts it from current W′ balance to compute headroom
- `WPrimeCritTimeDataType` uses it as the floor in `timeToFloorSec(...)`

This consistency is deliberate — previously each field had its own hardcoded breakpoints (31/62/92), which was a maintenance nightmare and inconsistent for the rider.

The TT field uses a different function: simple linear `100 × (1 − elapsed / duration)`.

---

## 6. UI Layouts

### Single-width
```
T: 97%       ← target for this moment
↘  89%       ← actual W′ + trend arrow
```

### Double-width
```
         W′ BALANCE
T: 97%     ↘    89%
```

### Trend arrow
13 rotation positions (−90° to +90°) proportional to power vs CP. SVG path data lives in `strings.xml` as `arrow_path_data`. Renders via `RemoteViews` (Glance can't easily animate, so it's static-per-update).

### Double-width scaling gotcha
`dwScale = 0.72f` applied to the actual W′ value text in `WPrimeRaceView.kt`. Without it, "100%" clips to "10" because the field width can't fit three digits at the default scale. **Don't remove this without testing on actual hardware.**

---

## 7. Settings Screen (`MainScreen.kt`)

Sections:
1. **W′ Parameters**: CP (watts), W′ (kJ)
2. **Time Trial**: TT Duration (minutes)
3. **Criterium**: Crit Duration (minutes), then `CritCurveEditor`
4. **Display**: trend arrow toggle, kJ/% toggles per field, model selector

### `CritCurveEditor` composable
- 4 rows, each with two `OutlinedTextField`s: Race % and W′ floor %
- Each row also displays the dynamic minute equivalent: `"%.0f min".format(racePct / 100.0 * critDurationMin)`
- The minute label updates live as the user changes either Race % or Crit Duration above it
- "Reset to Default" button (filled `Button`, not `OutlinedButton` — user explicitly noted the outlined version "doesn't look like a button")
- Reset uses `WPrimeRaceConfig.DEFAULT_CRIT_CURVE`

### Save button behaviour
Turns green briefly on save to confirm. Validation: numeric input only; `enter_valid_number` string shown on parse failure.

---

## 8. Build & Deploy

### Local build
Java needs to be the JBR shipped with Android Studio:
```
/Applications/Android Studio.app/Contents/jbr/Contents/Home
```
ADB lives at:
```
/Users/jamiebishop/Library/Android/sdk/platform-tools/adb
```

```bash
./gradlew clean assembleRelease
adb install app/build/outputs/apk/release/app-release.apk
```

If install fails with **signature mismatch**, the previously installed APK was signed with a different key (e.g. one from GitHub Actions release vs local debug). Fix:
```bash
adb uninstall com.j4m1eb.wprimerace
adb install app/build/outputs/apk/release/app-release.apk
```

If you hit **duplicate dex class** errors, run `./gradlew clean` first.

### Release build
Requires environment variables:
- `KEYSTORE_PATH`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Falls back to debug signing if `KEYSTORE_PATH` is not set, so local builds work without secrets but won't be installable over a release-signed APK.

### `manifest.json` generation
Custom Gradle task `generateManifest` (in `app/build.gradle.kts`) produces a `manifest.json` in the project root, used by the Karoo companion app for auto-update detection. Wired to run as part of `assemble`. Don't manually edit `manifest.json` — it's regenerated.

### GitHub Actions release
`.github/workflows/android.yml` triggers on tag push. Uses `ncipollo/release-action@v1` with:
- `makeLatest: true` — important, otherwise the companion app's auto-update doesn't pick it up
- `allowUpdates: true` — for re-running a tag if needed

To cut a release:
```bash
git tag v1.2-release
git push origin v1.2-release
```
Then verify the release on GitHub points the `app-release.apk` asset to `https://github.com/j4m1eb/Karoo-Wprime-Race/releases/latest/download/app-release.apk`.

### Karoo version display caching gotcha
After a successful version bump install, the Karoo's UI may still show the old version. ADB `dumpsys package com.j4m1eb.wprimerace | grep version` will confirm what's actually installed. Force-stop the app on the Karoo, or reboot, to refresh the displayed version.

---

## 9. Recent Major Changes (v1.0 → v1.2)

In rough order:

1. **Fixed elapsed time unit bug** — Karoo emits ELAPSED_TIME in ms; was being treated as seconds, causing W′ to deplete in 5 seconds
2. **Added `elapsedOffset`** — anchors pacing to recording start, not Karoo system uptime
3. **Crit target redesigned from linear interpolation → step function** (the fundamental design change above)
4. **Unified all crit fields** to read from `config.critCurve` rather than each having its own hardcoded breakpoints
5. **Added "Time to Empty" field** — like Time to Floor but always uses 0% as the floor; useful for "how long until I completely blow up" answers
6. **Added customisable pacing curve UI** — 4 editable interior breakpoints with dynamic minute display and Reset to Default
7. **Bumped to v1.2** (versionCode 3, versionName "1.2"); set `makeLatest: true` in release action

---

## 10. TT / Crit Split Config

**Resolved in v1.3:** TT and Crit now have separate CP/W′/duration settings in one app.

### Why it matters

The user's measured CP from intervals.icu is around 239–256W (depending on fitting window). For **crit racing** that's fine — crits are characterised by repeated above-CP surges and the absolute CP value matters less than W′ capacity.

For **TT racing** the user is doing the **"hack"**: rather than using physiological CP, they're setting CP = (target average power − a small delta) and W′ = a "pacing budget" that represents how many joules of overage they can tolerate before they run out. This converts the field from "physiological W′ tracker" into "pacing budget tracker" — same maths, repurposed inputs.

Example for tomorrow's race (25mi out-and-back, rolling, net downhill out + cross-tail / net uphill back + cross-head):
- TT target: 245W for 65min
- TT settings the user is going to use: **CP=240, W′=12kJ, duration=65min**

These TT values are wrong for crits. The crit values are wrong for TT. Before v1.3 the user had to manually swap before each race, which was brittle.

### Options considered

**Option A — single app, separate config blocks (implemented in v1.3)**
```kotlin
data class WPrimeRaceConfig(
    val model: WPrimeModel,
    val showArrow: Boolean,
    val tt: TtConfig,
    val crit: CritConfig,
)
data class TtConfig(val cp: Int, val wPrimeKj: Double, val durationMin: Int)
data class CritConfig(val cp: Int, val wPrimeKj: Double, val durationMin: Int, val curve: List<CritCurvePoint>)
```
Each DataType pulls from `config.tt` or `config.crit`. Settings UI splits into two clearly labelled sections. ~2 hours of work, low risk.

**Option B — split into two apps**
"W Prime TT" and "W Prime Crit" as separate APKs. Cleaner conceptually but doubles maintenance cost and duplicates code (or requires extracting a deeper shared library). Not recommended unless TT and Crit divergence grows further.

### Current implementation
- `WPrimeRaceConfig` contains `tt: TtConfig` and `crit: CritConfig`.
- `WPrimeTTDataType` uses only `config.tt`.
- All crit fields use only `config.crit`.
- Old single CP/W′ DataStore keys are treated as migration seeds for both blocks until the user saves the new split settings.
- Possible v1.5 follow-up: named "Profiles" feature (save sets of values per race type, e.g. "Summer TT 25mi", "Tuesday Crit 60min").

---

## 11. Domain Knowledge a New Dev Needs

### W′ and CP basics
- **CP (Critical Power)**: the power you can theoretically hold indefinitely. In practice, ~30–60min sustainable.
- **W′ (W-prime)**: the finite work above CP, in kilojoules. Once depleted, you're done.
- Above CP: W′ depletes at rate `(power − CP)` joules per second.
- Below CP: W′ recovers, rate depends on the model and `(CP − power)`.
- Typical values for trained cyclists: CP 200–350W, W′ 10–25 kJ.

### Why the four models?
There's no single accepted W′ recovery model. They differ mainly in how recovery rate scales:
- Skiba 2012 / Differential: time-constant–based recovery, well validated, default choice
- Bartram: linear in power below CP, simpler but less accurate at extremes
- Chorley Bi-Exp: two-component recovery (fast / slow), best-fit for physiology but more parameters

If a user reports "the field doesn't match how I felt", first thing to check is which model is selected.

### Karoo Extensions SDK quirks
- Hammerhead's [karoo-ext](https://github.com/hammerheadnav/karoo-ext) is the SDK
- Field types must be pre-registered in `extension_info.xml`. Forgetting to add a `<DataType>` there is the #1 reason a new field doesn't appear in the picker.
- `streamDataFlow` is the modern API; older examples use callback-based subscriptions
- Glance views run in a separate process (RemoteViews) — don't share singletons across the boundary. Pass everything through DataStore.
- The Karoo runs Android 10+ on Karoo 2 and Android 12+ on Karoo 3. Min SDK is 23 to be safe.

### Compatibility floor
- Karoo software version 1.524+ (set in README requirements)

---

## 12. Files of Interest

| File | Why it matters |
|---|---|
| `app/build.gradle.kts` | Version, signing config, manifest generation task |
| `app/src/main/res/xml/extension_info.xml` | **MUST contain a `<DataType>` entry for every field** |
| `app/src/main/res/values/strings.xml` | All field titles/descriptions; arrow SVG path |
| `app/src/main/kotlin/.../extension/WPrimeRaceExtension.kt` | Entry point; lists all DataTypes |
| `app/src/main/kotlin/.../extension/WPrimeCritFloor.kt` | The step function — the heart of the crit logic |
| `app/src/main/kotlin/.../extension/WPrime*DataType.kt` | One per field; each subscribes to power + elapsed and emits a render state |
| `app/src/main/kotlin/.../settings/WPrimeRaceSettings.kt` | DataStore-backed config flow; default values |
| `app/src/main/kotlin/.../screens/MainScreen.kt` | Settings UI; contains `CritCurveEditor` |
| `app/src/main/kotlin/.../ui/WPrimeRaceView.kt` | Glance view rendering; `dwScale` clipping fix lives here |
| `lib/src/main/kotlin/.../models/` | The four W′ models |
| `.github/workflows/android.yml` | Release pipeline |
| `manifest.json` | **Generated** — don't hand-edit |
| `README.md` | User-facing docs; reflects v1.2 |

---

## 13. Known Issues / Open Items

| Item | Severity | Notes |
|---|---|---|
| Named race profiles not yet available | Medium | Possible v1.5 follow-up. |
| Karoo UI version display can lag after install | Low | Requires force-stop or reboot to refresh; cosmetic |
| No "race profiles" — settings must be manually changed per event | Medium | Possible v1.5 |
| The Crit step function's last interior point is at 99% (default) — there's an edge case if a user sets it to 100% | Low | Validate input; don't allow ≥100% |
| Models other than Skiba Differential are less battle-tested with real Karoo data | Low | Default is fine; user feedback will catch issues |
| Trend arrow has 13 fixed positions — not perfectly smooth | Low | Acceptable, animation isn't a requirement |

---

## 14. Testing Notes

There are no automated tests in the repo currently. Validation has been:

1. **ADB install on physical Karoo** — primary testing path
2. **Real-world racing** — the user has been testing live in TTs and crits
3. **Math sanity checks** — the step function and TT linear function are simple enough to verify by hand

If you add tests, the highest-value place is the `lib/` module — the W′ models and `critPhaseFloorFraction` are pure functions and trivial to unit-test. The Glance views and DataType subscriptions are harder to test in isolation; consider integration tests against the Karoo emulator (`scansDevices="false"` so it should run in any Android environment with the Karoo SDK present).

---

## 15. Communication / Workflow Notes for the User

- The user (j4m1eb) is the *primary* tester. Releases happen quickly — sometimes in response to a single race day's findings.
- They're a real racer who'll test in actual events, not in lab conditions. Expect feedback like "it ticked down too slow at minute 50" rather than "the regression test failed".
- Version bumps go to GitHub release immediately. Auto-update on the companion app pulls the latest tag with `makeLatest: true`.
- They appreciate concise, decisive recommendations over options paralysis. Pick a number, justify it briefly, ship.
- Pre-race conversations often pivot into pacing strategy / W′ math discussions — engage with these, they're shaping product direction.

---

## 16. Quick Start for a New Dev

1. Clone repo, open in Android Studio
2. Confirm JBR is set as project SDK (Android Studio → Settings → Build Tools → Gradle → JDK location)
3. `./gradlew clean assembleRelease` to confirm build
4. Plug Karoo in via USB, enable developer options, ADB install
5. On Karoo: open W Prime Race app, set CP, W′, durations, save
6. Add fields to a ride profile data page
7. Start a recording (without riding) and confirm the field reads 100% target / 100% actual at t=0
8. Read this doc end-to-end before changing anything in the crit logic — `critPhaseFloorFraction` is the load-bearing wall

---

## 17. Contact

GitHub: https://github.com/j4m1eb/Karoo-Wprime-Race
Owner: j4m1eb (Jamie)
