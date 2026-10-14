# Time Management App

A small Android app for making time for study, work, exercise, walking, cooking,
reading, hobbies, games, breaks, and activities you add yourself.

The **Focus** tab has activity timers, daily tasks, music, and progress. When a
timed session ends, confetti and points celebrate it; after a focus session, you
can choose a five-minute tap-the-star game or start a five-minute break timer.
The **Tools** tab has a stopwatch, a one-time alarm reminder, and a clock that
uses the time zone configured on the device.

Copyright © 2026 Jaineil Rana.

Source code: [GitHub](https://github.com/jaineil32624/time-management-app).

The included soft-focus ambient loop is original sound generated for this app;
it does not use third-party music. SomaFM internet radio is streamed from
[SomaFM](https://somafm.com/listen/) and the broadcast tracks remain the
property of their artists and labels. Spotify and YouTube Music buttons open
activity-matched searches in those services; they do not copy or rehost songs.
Music selected from your device remains the property of its respective
copyright holder.

## Run it

Open this folder in Android Studio and let it sync Gradle. Choose an emulator or
Android phone, then press **Run**. You can also build the debug APK on Windows
with `.\gradlew.bat assembleDebug`.

The alarm asks for notification permission the first time you set it. It rings
using the phone's alarm volume until you stop it; check that volume and the
device's alarm sound settings if it is too quiet. Android can delay reminders
when battery saving is enabled, and exact timing depends on the device's alarm
settings.

## Prepare a Play Store release

The app bundle can be built with `.\gradlew.bat bundleRelease`. Before publishing:

1. Choose a globally unique application ID in `app/build.gradle.kts`.
2. Create and safely back up your own Play upload key. Do not commit the key or
   its passwords to the project.
3. Set these environment variables in your local build session:
   `ANDROID_UPLOAD_STORE_FILE`, `ANDROID_UPLOAD_STORE_PASSWORD`,
   `ANDROID_UPLOAD_KEY_ALIAS`, and `ANDROID_UPLOAD_KEY_PASSWORD`.
4. Run `.\gradlew.bat bundleRelease` again. The signed bundle is written to
   `app/build/outputs/bundle/release/app-release.aab`.
5. Complete the Play Console listing, content rating, data-safety form, and
   testing requirements before submitting the app.

If the upload-key environment variables are not set, Gradle builds an unsigned
bundle for local testing; Google Play will not accept that bundle for upload.
