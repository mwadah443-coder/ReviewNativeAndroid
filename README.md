# Review — Native Android

This is the native Kotlin/Jetpack Compose rebuild of the Review app. The previous WebView/HTML/CSS/JavaScript UI has been replaced by native Android UI and local Kotlin state.

## Open
Open `ReviewNativeAndroid` in Android Studio with JDK 17. Sync Gradle and run the `app` configuration.

## Features
- Dashboard and daily review queue
- Spaced-review scheduling with Again/Hard/Good/Easy ratings
- Tasks, search, trash/restore, task editor
- Folders
- Goals
- Self-development tasks
- Calendar
- Analytics and memory metrics
- Native Review Agent chat with local app-context analysis
- Smart flashcard generation from pasted lesson text
- Local notification scheduling and OneSignal integration hook
- JSON backup/export and import
- Dark green visual theme and Arabic RTL UI

Set `ONESIGNAL_APP_ID` in `gradle.properties` if you use OneSignal.
