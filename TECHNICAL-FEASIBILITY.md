# AawaraRide Milestone 1: call detection feasibility

## Selected approach

AawaraRide uses a foreground service and a dynamically registered receiver for
`TelephonyManager.ACTION_PHONE_STATE_CHANGED` as a fallback. On newer Android
devices, the app also optionally requests the `ROLE_CALL_SCREENING` role. A
user who grants that role lets the public `CallScreeningService` receive the
incoming call handle without making AawaraRide the default dialer. The service
always responds with “allow” and never blocks, silences, answers, or rejects a
call.

The receiver is registered only while Riding Mode is active. The service
deduplicates repeated `RINGING` events and cancels pending TTS when the call
leaves the ringing state.

## API comparison

| API | Caller identity without default dialer | Decision |
|---|---|---|
| Phone-state broadcast | Provides phone state; the number extra may be withheld from non-default apps on modern Android/OEM builds | Fallback |
| `PhoneStateListener` | Provides state and is deprecated from API 31; number availability varies by version and OEM | Compatibility reference only |
| `TelephonyCallback` | Reliable state callbacks from API 31, but the callback has no caller number | Not sufficient alone for caller identification |
| `CallScreeningService` | Provides incoming call details without default-dialer status, but requires the user-granted call-screening role | **Preferred when granted** |

Android and OEM restrictions mean the incoming number may be absent for
private calls, restricted caller-ID policies, or some device builds. In that
case the app announces a private number. If the number is present but no
matching contact exists, it announces an unknown number. Contacts remain
on-device and are never uploaded.

## Audio routing limitation

Text-to-speech is configured as notification speech. Android's audio policy
chooses the active output route; a connected Bluetooth helmet that exposes a
media or notification output can receive the announcement, but an app cannot
guarantee routing to every Bluetooth intercom implementation without taking
over call audio. AawaraRide intentionally does not request call audio or
modify the native call route. Bluetooth disconnection therefore degrades to
the system's available audio output.

## Device compatibility

The implementation uses public Android APIs and has no Samsung- or Xiaomi-
specific code. On Samsung Galaxy S23 Ultra and Xiaomi Redmi Note 11, users
must allow phone, contacts, and (API 33+) notification permissions and exempt
the app from aggressive battery optimization if the manufacturer stops the
foreground service. That battery-management step is device-settings guidance,
not an undocumented API dependency.
