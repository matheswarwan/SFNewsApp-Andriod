# SFNewsApp

SFNewsApp is a Kotlin Android app configured for Firebase Cloud Messaging and Salesforce Marketing Cloud MobilePush.

Android application ID: `com.fcs.sfnewsapp`

## Configuration

1. Place the real Firebase file downloaded for package `com.fcs.sfnewsapp` at `firebase/google-services.json`.
2. The Gradle build syncs `firebase/google-services.json` into `app/google-services.json` before the Google Services task runs. These files are ignored because they contain environment-specific config.
3. Confirm the SFMC settings in `app/src/main/java/com/fcs/sfnewsapp/MyApp.kt`:
   - App ID
   - Access Token
   - FCM Sender ID
   - App Endpoint
   - MID, if your SFMC business unit requires one
4. Build and run the app:

```bash
./gradlew assembleDebug
```

Use JDK 17 for Android Gradle Plugin 8.x builds. The project includes the Gradle wrapper, so no separate Gradle install is required.

## Deep Link Test

Use this command after installing the debug app on a device or emulator:

```bash
adb shell am start -a android.intent.action.VIEW -d "sfnews://app/product/weather" com.fcs.sfnewsapp
```

The app opens the matching product page and displays the incoming URI.

## Firebase

The Google Services plugin is enabled and Firebase Analytics plus Firebase Messaging are included through the Firebase BOM. The real Firebase config belongs at:

```text
firebase/google-services.json
```

The app module uses `app/google-services.json` as the build-time copy target for the Google Services plugin. This file is ignored; use `app/google-services.json.example` as a template.

## Salesforce Marketing Cloud

The app initializes MobilePush from `MyApp`, creates the default notification channel `marketing_default`, persists an install-time UUID under `contact_key`, sets SFMC identity profile attributes, adds the `general` tag, forwards refreshed FCM tokens, and lets SFMC handle Marketing Cloud push messages. SFMC notification taps route through OpenDirect or custom keys `deeplink`, `deepLink`, and `url`. Device Info displays contact key, FCM token, SFMC system token, SFMC device ID, and SFMC contact key for testing.

This project uses `com.salesforce.marketingcloud:marketingcloudsdk:8.2.0` because Salesforce versions after 8.2.0 are compiled with Android API 35 or 36, and versions 10 and 11 require minSdk 26. Version 8.2.0 is the newest release in the Salesforce changelog that is compiled with Android API 34, matching this project's required `compileSdk 34` and `minSdk 24`.
