# Game Login Reminder

An Android app that reminds you to log in to your games every day, tracks in-game event deadlines, and shows your login streaks.

## Features

- **Daily login reminders**: add any game and set a reminder time. Tap "Đã đăng nhập" once you've logged in and that day's reminder is skipped.
- **Event tracking**: add in-game events with an end date and time. You get notified 24 hours and 2 hours before they end.
- **Achievements**: current streak, best streak, total login days, and a monthly calendar for each game.
- **Offline**: all data is stored locally on your device. No account and no internet needed.

## Download

Get the latest APK from the [Releases](../../releases) page, then allow installs from unknown sources when Android asks.

Requires Android 8.0 (API 26) or higher.

## Notes

- The app interface is in Vietnamese.
- On some devices (Xiaomi, Oppo, Samsung...), turn off battery optimization for the app so notifications arrive on time.
- Allow notifications, and exact alarms on Android 12+.

## Build from source

1. Open the project in Android Studio and let Gradle sync.
2. Run it on a device or emulator, or use Build > Build APK(s).

The repo also includes a GitHub Actions workflow that builds a debug APK (Actions tab > Build APK > Run workflow).

## Tech stack

Kotlin, Jetpack Compose (Material 3), AlarmManager, local storage via SharedPreferences.
