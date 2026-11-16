# AawaraRide

Current release: **0.1.m3-f1**.

Milestone-based release policy, Android version codes, APK naming, and release
history are documented in [VERSIONING.md](VERSIONING.md).

Android Java application for announcing incoming cellular callers through the
phone's available audio route while preserving native call handling.

## Build

The local development setup uses:

- OpenJDK 17
- Android SDK platform 35
- Android SDK build-tools 35.0.0
- Android platform-tools 37.0.1
- Gradle 8.9 through the included wrapper
- VS Code extensions `vscjava.vscode-java-pack`,
  `vscjava.vscode-gradle`, and `adelphes.android-dev-ext`

The project uses Gradle Kotlin DSL, Java 11 source compatibility, minimum API
29, and no runtime network dependency. In a new terminal, set the SDK path:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Then run:

```text
./gradlew assembleDebug
```

The versioned debug APK is written under
`app/build/outputs/apk/debug/` with the `AawaraRide-0.1.m3-f1` archive prefix.

The expected fix-build artifact is `AawaraRide-0.1.m3-f1.apk`. This workspace
does not currently contain a built APK because Java is not configured in the
build environment. Physical GPS and mock-location testing is still required;
the release notes distinguish those checks from source validation.

## Run

1. Install the debug APK on a physical Android device.
2. Grant phone-state, microphone, contacts, and notification permissions when prompted.
3. Open Settings and verify announcement defaults: initial enabled, repeat
   enabled, five-second delay, maximum repeat count one.
4. Enable Riding Mode before placing a test call.
5. Disable Riding Mode to stop monitoring and cancel pending announcements.

On Android 10+, enabling Riding Mode offers the optional call-screening role.
Granting it improves caller-number availability without making AawaraRide the
default dialer. If it is declined, the app uses the phone-state fallback and
Android/OEM restrictions may hide the incoming number.

## Samsung Galaxy S23 Ultra test checklist

- Grant phone, contacts, and notification permissions.
- Enable Riding Mode and grant the optional call-screening role.
- Test a saved contact, an ordinary unknown number, and a restricted caller ID.
- Test normal, vibrate, silent, and silent-ringtone modes.
- Test with a Bluetooth helmet connected and disconnected.
- With Riding Mode enabled, test “Accept call”, “Pick up”, “Answer call”,
  “Reject call”, “Decline call”, and “End call”.
- Answer and reject using the native phone UI and headset controls.
- Lock the screen, place another call, then disable Riding Mode and confirm
  pending speech stops.

Milestone 3 also records rides after the first valid GPS fix, including distance,
duration, moving/idle time, and speed metrics. Ride data is stored locally for
recovery and is shown on the main screen after completion.

Voice recognition is not continuous: it is created only after an incoming call
announcement begins and is destroyed when the call ends or a command session
finishes. Android may deny `TelecomManager` answer/end actions to an ordinary
application. In that case AawaraRide leaves the call ringing and the native
phone/headset controls remain authoritative; it does not become the default
dialer automatically. Android's speech recognizer may use a system recognition
service, so users should review the device's speech-recognition privacy
settings. Bluetooth microphone routing is delegated to Android's active audio
device policy and cannot be guaranteed for every headset profile.

Call detection and the public API trade-offs are documented in
[TECHNICAL-FEASIBILITY.md](TECHNICAL-FEASIBILITY.md).
