# Deployment Instructions

## Prerequisites

- JDK 17
- Android Studio or Android SDK with API 34 installed
- Firebase project configured for Android package `com.fcs.sfnewsapp`
- Salesforce Marketing Cloud MobilePush app configuration

## Firebase Setup

1. Download `google-services.json` from Firebase for package `com.fcs.sfnewsapp`.
2. Place it at:

```text
firebase/google-services.json
```

3. Keep `app/google-services.json` in place as the app-module file required by the Google Services Gradle plugin. During Gradle builds, `firebase/google-services.json` is copied into `app/google-services.json` if present.

## SFMC Setup

Update SFMC values in `app/src/main/java/com/fcs/sfnewsapp/MyApp.kt`:

- App ID
- Access Token
- FCM sender ID
- App endpoint
- MID if required; blank means `setMid` is skipped

The Device Info page displays values needed for SFMC testing:

- Contact key
- FCM token
- SFMC system token
- SFMC device ID
- SFMC contact key

If the FCM token already existed before SFMC startup, `MyApp` syncs the current token to SFMC after successful initialization.

## Build

```bash
./gradlew assembleDebug
```

## Tests

```bash
./gradlew testDebugUnitTest
```

Use JDK 17. Java 26 fails in this environment with `Unsupported class file major version 70`.

## Deep Link Test

Install the app, then run:

```bash
adb shell am start -a android.intent.action.VIEW -d "sfnews://app" com.fcs.sfnewsapp
```

The app logs and displays the incoming URI.

Use a routed page for direct testing:

```bash
adb shell am start -a android.intent.action.VIEW -d "sfnews://app/product/weather" com.fcs.sfnewsapp
```

Current supported deep links:

- `sfnews://app`
- `sfnews://app/device-info`
- `sfnews://app/update-details`
- `sfnews://app/product/breaking`
- `sfnews://app/product/markets`
- `sfnews://app/product/sports`
- `sfnews://app/product/weather`

## SFMC Deep Link Push

Use one of these values in SFMC:

```text
sfnews://app/product/breaking
sfnews://app/product/markets
sfnews://app/product/sports
sfnews://app/product/weather
sfnews://app/device-info
sfnews://app/update-details
```

Preferred setup:

- Enable OpenDirect for the MobilePush app in SFMC.
- Put the deep link value in the push message OpenDirect field.
- Alternatively, enable Custom Keys and send key `deeplink` with the deep link value.

The app handles custom keys `deeplink`, `deepLink`, and `url`, then falls back to the SFMC OpenDirect URL.

## Push Test Notes

- Confirm the device has notification permission on Android 13+. The app requests this permission on launch.
- If SFMC says the device is not opted in, open the app, allow notification permission, then tap **Opt in**.
- The **Opt in** button enables SFMC push and sends contact key, the `general` tag, and any entered user details.
- The **Opt out** button disables SFMC push.
- Confirm the FCM sender ID in SFMC matches Firebase.
- Confirm SFMC registration is visible before sending MobilePush notifications.
- For non-SFMC FCM payloads, include `data.deeplink`, `data.deepLink`, or `data.url` to trigger the fallback deep link open behavior.
