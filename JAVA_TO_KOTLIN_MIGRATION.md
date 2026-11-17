# Java-to-Kotlin Migration

## Status

The application source migration is complete in the working tree. All
application-owned Java sources and the Java unit test were replaced with Kotlin
sources. No product behavior, Android component declaration, JSON key, or
SharedPreferences file/key was intentionally changed.

## Initial inventory

- Application Java sources: 16
- Java unit tests: 1
- Kotlin sources before migration: 0
- Android activities: `MainActivity`, `SettingsActivity`,
  `RideHistoryActivity`, `RideDetailsActivity`
- Foreground service: `RidingModeService`
- Call-screening service: `AawaraCallScreeningService`
- Persistence: `RideStore`/`RideDao`, using SharedPreferences JSON
- Location processing: `RideTracker`, `RideRecord`, `RidePoint`
- Audio and telephony: `AnnouncementManager`, `CallerIdentificationManager`,
  `CallActionManager`, `VoiceCommandManager`
- Room, workers, content providers, and static broadcast receivers: none

## Migration checklist

- [x] Core models and duration formatting
- [x] JSON persistence and DAO-style accessors
- [x] GPS tracking and ride lifecycle
- [x] Settings repository
- [x] Text-to-Speech and audio focus
- [x] Caller lookup and telephony actions
- [x] Voice command manager
- [x] Foreground service and call-screening service
- [x] Activities and unit test
- [x] Java source removal
- [x] Kotlin Gradle plugin configuration
- [ ] Automated build and lint validation
- [ ] Physical-device and APK installation validation

## Files migrated

| Java source | Kotlin source |
| --- | --- |
| `com.aawararide.audio.AnnouncementManager` | `AnnouncementManager.kt` |
| `com.aawararide.contacts.CallerIdentificationManager` | `CallerIdentificationManager.kt` |
| `com.aawararide.ride.DurationFormatter` | `DurationFormatter.kt` |
| `com.aawararide.ride.RideDao` | `RideDao.kt` |
| `com.aawararide.ride.RidePoint` | `RidePoint.kt` |
| `com.aawararide.ride.RideRecord` | `RideRecord.kt` |
| `com.aawararide.ride.RideStore` | `RideStore.kt` |
| `com.aawararide.ride.RideTracker` | `RideTracker.kt` |
| `com.aawararide.settings.AnnouncementSettingsRepository` | `AnnouncementSettingsRepository.kt` |
| `com.aawararide.service.RidingModeService` | `RidingModeService.kt` |
| `com.aawararide.telephony.AawaraCallScreeningService` | `AawaraCallScreeningService.kt` |
| `com.aawararide.telephony.CallActionManager` | `CallActionManager.kt` |
| `com.aawararide.ui.MainActivity` | `MainActivity.kt` |
| `com.aawararide.ui.SettingsActivity` | `SettingsActivity.kt` |
| `com.aawararide.ui.RideHistoryActivity` | `RideHistoryActivity.kt` |
| `com.aawararide.ui.RideDetailsActivity` | `RideDetailsActivity.kt` |
| `com.aawararide.voice.VoiceCommandManager` | `VoiceCommandManager.kt` |
| `DurationFormatterTest` | `DurationFormatterTest.kt` |

## Build and dependency changes

The Android module now applies `org.jetbrains.kotlin.android` version
`2.0.21` and targets JVM 11. Android SDK, application ID, min/target/compile
SDK values, build variants, and existing AndroidX/JUnit dependencies remain
unchanged. No runtime dependency was added.

## Compatibility

The SharedPreferences files and keys and every ride JSON key remain unchanged.
`RideRecord.fromJson` still accepts missing fields and sorts route points by
timestamp, so existing active rides, completed rides, and route points remain
readable. Runtime compatibility with stored fixtures and an installed APK
requires the pending Android build/device validation.

## Validation results

The requested baseline command was:

```text
gradlew.bat test lintDebug assembleDebug
```

It could not start because the environment has neither `JAVA_HOME` nor a
`java` executable. Consequently, post-migration tests, lint, APK packaging,
installation, and physical-device checks are pending rather than claimed.

## Known limitations

Existing voice-command, Android/OEM call-control, Bluetooth routing, and
physical GPS limitations remain unchanged. The migration does not claim
physical-device behavior until an Android SDK/JDK-equipped environment is
available.
