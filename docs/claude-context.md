# Claude Context

## Project

SFNewsApp is a greenfield Android app in Kotlin using a single `app` module.

## Current Configuration

- Application ID and namespace: `com.fcs.sfnewsapp`
- minSdk: 24
- targetSdk: 34
- compileSdk: 34
- Android Gradle Plugin: 8.5.2
- Kotlin Android plugin: 1.9.24
- Google Services Gradle plugin: 4.4.4
- Firebase BOM: 34.12.0
- Firebase dependencies: Analytics and Messaging
- SFMC MobilePush dependency: `com.salesforce.marketingcloud:marketingcloudsdk:8.2.0`

## Important Files

- Push implementation guide: `docs/Push-notification-setup-guide.md`
- Main activity: `app/src/main/java/com/fcs/sfnewsapp/MainActivity.kt`
- Application class: `app/src/main/java/com/fcs/sfnewsapp/MyApp.kt`
- FCM service: `app/src/main/java/com/fcs/sfnewsapp/MyFirebaseMessagingService.kt`
- Manifest: `app/src/main/AndroidManifest.xml`
- Notification icon: `app/src/main/res/drawable/ic_notification.xml`
- Root Firebase config location requested by user: `firebase/google-services.json` ignored in Git
- App-module Google Services file used by plugin: `app/google-services.json` ignored in Git

## Firebase Config Handling

The Google Services Gradle plugin expects `google-services.json` in the app module. Real Firebase configs are kept in `firebase/google-services.json` locally and copied to `app/google-services.json` before Google Services processing when the root Firebase file exists. Both real config paths are ignored in Git; `app/google-services.json.example` is the committed template.

## SFMC Notes

SFMC SDK `8.2.0` is intentionally used because the requested project constraints are `compileSdk 34` and `minSdk 24`. Newer Salesforce versions listed after 8.2.0 are compiled with Android API 35 or 36, and SDK versions 10/11 raise the minimum Android API to 26.

`MyApp` creates notification channel `marketing_default`. SFMC values are placeholders in Git and must be replaced locally with App ID, Access Token, FCM Sender ID, app endpoint, and MID if required. MID is skipped when blank. After SFMC initializes successfully, it persists `contact_key`, sets identity profile fields, adds the `general` tag, and syncs the current Firebase token to SFMC. `MyFirebaseMessagingService` forwards future token updates and lets SFMC handle Marketing Cloud messages before falling back to custom keys `deeplink`, `deepLink`, or `url`.

`MainActivity` is a simple in-app router with Home, Device Info, Update Details, and product tile pages. Device Info shows SFMC/Firebase test identifiers: app contact key, FCM token, SFMC system token, SFMC device ID, and SFMC contact key. It has copy buttons for all identifiers and for each individual value.

On Android 13+, `MainActivity` requests `POST_NOTIFICATIONS` at runtime so push notifications can display.

`MainActivity` includes Opt in and Opt out buttons. Opt in requests notification permission if needed, enables SFMC push, commits contact key, adds the `general` tag, and sends any entered name/email/phone/address attributes. Opt out disables SFMC push. The screen also has forms to update name, email, phone, address, and contact key. Updating contact key persists it locally, calls `registrationManager.edit().setContactKey(...).commit()`, and calls `sdk.identity.setProfileId(...)`.

Deep links route inside `MainActivity`:

- `sfnews://app` opens Home
- `sfnews://app/device-info` opens Device Info
- `sfnews://app/update-details` opens Update Details
- `sfnews://app/product/breaking` opens Breaking News
- `sfnews://app/product/markets` opens Markets
- `sfnews://app/product/sports` opens Sports
- `sfnews://app/product/weather` opens Weather

SFMC notification taps use `NotificationLaunchIntentProvider`. It checks custom keys `deeplink`, `deepLink`, or `url`, then falls back to the SFMC OpenDirect URL. If none are present, it opens `sfnews://app`.

## Local Verification State

XML and JSON static validation passed. Full Gradle tests could not run in this environment because only Java 26 is installed and AGP/Gradle failed with `Unsupported class file major version 70`. Use JDK 17.
