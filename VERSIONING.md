# AawaraRide versioning

## Version naming convention

AawaraRide uses:

```text
0.1.<milestone>
0.1.<milestone>-f<fix_number>
```

Examples are `0.1.m1`, `0.1.m2a`, and `0.1.m2a-f1`.

The Android `versionName` must exactly match the milestone or fix identifier.
The identifier is product-facing and is used in APK filenames.

## Milestone identification rules

Every new product milestone receives a new base identifier. Sub-milestones use
lowercase suffixes, such as `m2a` and `m2b`, when the product scope defines
separate milestone stages.

## Fix-cycle increment rules

Fixes within a milestone append `-f1`, `-f2`, and so on. Never reuse an
identifier and never reset a fix counter within the same milestone. Before
building, inspect this file and continue from the latest release-history entry.

## Android versionCode

`versionCode` is an integer and must increase monotonically for every
distributed build, including fix builds. The current `0.1.m3b` build uses `versionCode 6`; future distributed builds must
continue increasing this value.

## APK naming

Distributed APKs use:

```text
AawaraRide-<versionName>.apk
```

For example: `AawaraRide-0.1.m2a-f1.apk`. Debug builds may retain an additional
build-type suffix in Gradle's intermediate output, but the release artifact
must use the versioned name above.

## Release history

### 0.1.m3b-f1 — versionCode 7

- Migrated all application-owned Java source and the unit test to Kotlin.
- Preserved the existing JSON-backed ride store, manifest, permissions,
  calculations, call handling, announcements, and XML UI.
- Build and device validation are pending in the current environment because
  Java/JAVA_HOME is unavailable.

### 0.1.m3b — versionCode 6

- Added persistent completed ride history and idempotent finalisation.
- Added incremental route-point retention through the existing local
  SharedPreferences JSON store; no second persistence framework was introduced.
- Added Ride History and Ride Details screens with duration formatting,
  coordinates, speed metrics, and route-point availability.
- Added DAO-style accessors for completed rides and ordered route points.
- Build and lint status: `./gradlew clean lintDebug assembleDebug` passed.
- Automated test status: duration-formatting unit tests are included; no physical
  device tests were executed in this environment.
- Physical-device GPS, background, screen-off, and mock-location checks remain
  pending in this environment.
- Known limitation: the project does not currently use Room, so these records
  remain in the existing JSON-backed local store rather than a SQL database.

### 0.1.m3-f1 — versionCode 5

- Allowed valid mock-provider locations through the normal foreground tracking
  pipeline; mock origin is not treated as an invalid location.
- Added location timestamp, accuracy, jump, duplicate, and speed consistency
  validation, including Android speed-accuracy metadata when available.
- Reduced stationary GPS drift and prevented one high-speed sample from setting
  the top speed.
- Expired current speed when a ride is completed and retained the existing
  fifteen-second freshness window for live speed display.
- Formatted total, moving, idle, history, and summary durations as `MM:SS` or
  `HH:MM:SS` while retaining numeric seconds internally.
- Source checks: VS Code diagnostics passed for touched Java files. Gradle
  packaging was attempted but could not run because Java and `JAVA_HOME` are
  unavailable in this environment.
- Physical-device checks, external mock-location app checks, background and
  screen-off checks, and Milestone 1 call-announcement regression checks remain
  pending until a configured Android build/device environment is available.
- Known limitation: physical GPS accuracy and external mock-location behavior
  have not been verified on a device in this fix cycle.

### 0.1.m3 — versionCode 4

- Added foreground GPS tracking that waits for the first valid fix before
  starting a ride.
- Added filtered distance, speed, moving-time, idle-time, and ride summaries.
- Added incremental local persistence and unfinished-ride recovery.
- Preserved existing caller announcements and voice-command implementation.
- Known limitation: route history is stored locally but does not yet have a
  dedicated history screen.

### 0.1.m2a-f1 — versionCode 3

- Fixed microphone permission gating while preserving caller announcements when
  voice permission is denied.
- Fixed call-screening-first and phone-state-first event ordering for voice
  recognition startup.
- Added lifecycle diagnostics and stale-recognition callback protection.
- Declared microphone foreground-service support for Android 14+.
- Known limitation: recognition errors are logged but are not retried in this
  fix cycle.
- Known limitation: Android and OEM policy may still deny arbitrary
  `TelecomManager` answer/end operations.

### 0.1.m2a — versionCode 2

- Added scoped voice recognition during an incoming call.
- Added supported command matching for accept and reject phrases.
- Added guarded `TelecomManager` answer/end attempts.
- Added microphone and answer-call permissions.
- Preserved native phone and headset controls as the fallback when Android
  denies arbitrary call control.
- Known limitation: reliable answer/reject control may require default-dialer
  or InCall role privileges.

### 0.1.m1-pre-versioning — versionCode 1

- Rollback baseline for the pre-versioning Milestone 1 APK.
- Included Riding Mode, caller identification, TTS announcements, repeat
  settings, foreground monitoring, and optional call-screening role support.
