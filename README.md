# AawaraRide

Milestone 1 Android Java application for announcing incoming cellular callers
through the phone's available audio route while preserving native call handling.

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

The debug APK is written to
`app/build/outputs/apk/debug/app-debug.apk`.

## Run

1. Install the debug APK on a physical Android device.
2. Grant phone-state, contacts, and notification permissions when prompted.
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
- Answer and reject using the native phone UI and headset controls.
- Lock the screen, place another call, then disable Riding Mode and confirm
  pending speech stops.

Call detection and the public API trade-offs are documented in
[TECHNICAL-FEASIBILITY.md](TECHNICAL-FEASIBILITY.md).
