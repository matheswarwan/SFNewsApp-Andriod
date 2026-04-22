# SFNewsApp Android

SFNewsApp is a Kotlin Android sample app for Firebase Cloud Messaging and Salesforce Marketing Cloud MobilePush.

Package/application ID:

```text
com.fcs.sfnewsapp
```

## What This App Includes

- Firebase Cloud Messaging setup.
- Salesforce Marketing Cloud MobilePush setup.
- Runtime notification permission request for Android 13+.
- SFMC device registration, opt in, and opt out controls.
- Contact key registration and update flow.
- User profile/detail updates for name, email, phone, and address.
- Device Info page with copy buttons for Contact Key, FCM token, SFMC System Token, SFMC Device ID, and SFMC Contact Key.
- Deep link routing for SFMC OpenDirect/custom-key push notifications.
- Product tile pages for testing routed deeplinks.

## Prerequisites

- Android Studio.
- JDK 17.
- Android SDK API 34.
- Firebase Android app configured for `com.fcs.sfnewsapp`.
- SFMC MobilePush app configured for Android and Firebase Cloud Messaging.

Use JDK 17 for Android Gradle Plugin 8.x builds.

## Firebase Setup

1. Download `google-services.json` from Firebase for package `com.fcs.sfnewsapp`.
2. Place it here:

```text
firebase/google-services.json
```

3. The Gradle build copies it into:

```text
app/google-services.json
```

Both real config files are ignored by Git because they are environment-specific. Use this committed template for reference:

```text
app/google-services.json.example
```

## SFMC Setup

Copy the example config:

```bash
cp sfmc.properties.example sfmc.properties
```

Edit `sfmc.properties`:

```properties
sfmcAppId=YOUR_SFMC_APP_ID
sfmcAccessToken=YOUR_SFMC_ACCESS_TOKEN
fcmSenderId=YOUR_FCM_SENDER_ID
sfmcServerUrl=https://YOUR_SUBDOMAIN.device.marketingcloudapis.com/
sfmcMid=
```

Use values from SFMC MobilePush:

- App ID
- Access Token
- App Endpoint
- FCM Sender ID
- MID if your business unit requires it

If `SFMC_MID` is blank, the app skips `setMid(...)`.

`sfmc.properties` is ignored by Git. Gradle injects these values into `BuildConfig` during local builds. If this file is missing or still has placeholder values, SFMC initialization is skipped and Device Info will keep showing `Pending SFMC registration...`.

## Build and Run

Open the project folder in Android Studio:

```text
SFNewsAppAndriod
```

Then:

1. Let Gradle sync.
2. Select an emulator or device.
3. Run the `app` configuration.
4. Allow notification permission when prompted.

Terminal build:

```bash
./gradlew assembleDebug
```

Unit test task:

```bash
./gradlew testDebugUnitTest
```

## App Screens

Home includes tiles for:

- Device Info
- Update Details
- Breaking News
- Markets
- Sports
- Weather

Device Info includes:

- Contact Key
- FCM Token
- SFMC System Token
- SFMC Device ID
- SFMC Contact Key
- Copy buttons
- Opt in
- Opt out

Update Details includes:

- Name
- Email
- Phone
- Address
- Contact key update

## Opt In and Device Registration

To make the device eligible for SFMC push:

1. Run the app.
2. Allow notification permission.
3. Open **Device Info**.
4. Tap **Opt in**.
5. Wait for the SFMC identifiers to populate.
6. Confirm the device appears in SFMC MobilePush Audiences.

The Opt in button:

- Calls `enablePush()`.
- Commits contact key to SFMC registration.
- Adds the `general` tag.
- Sends entered user details if present.
- Calls `sdk.identity.setProfileId(...)`.

The Opt out button calls `disablePush()`.

If SFMC says `Device is not opted in`, verify:

- Notification permission is allowed.
- You tapped **Opt in** after granting permission.
- FCM Sender ID matches Firebase.
- SFMC registration has completed.
- The device appears in MobilePush Audiences.

## User Details and Contact Key

Use **Update Details** to send:

- `name`
- `email`
- `phone`
- `address`

The app updates both SFMC registration attributes and SFMC identity attributes.

When changing contact key, the app:

- Persists the new contact key locally.
- Calls `registrationManager.edit().setContactKey(...).commit()`.
- Calls `sdk.identity.setProfileId(...)`.
- Adds the `general` tag.

## Deep Link Routes

Supported routes:

```text
sfnews://app
sfnews://app/device-info
sfnews://app/update-details
sfnews://app/product/breaking
sfnews://app/product/markets
sfnews://app/product/sports
sfnews://app/product/weather
```

Test with adb:

```bash
adb shell am start -a android.intent.action.VIEW -d "sfnews://app/product/weather" com.fcs.sfnewsapp
```

## SFMC Deep Link Push

Preferred setup in SFMC:

1. Enable OpenDirect for the MobilePush app.
2. Create or edit a push message.
3. Set the OpenDirect URL to a supported route, for example:

```text
sfnews://app/product/weather
```

4. Send the push.
5. Tap the notification.
6. The app opens the matching page.

Alternative setup:

Use Custom Keys:

```text
deeplink = sfnews://app/product/weather
```

The app checks these custom keys:

```text
deeplink
deepLink
url
```

Then it falls back to the SFMC OpenDirect URL.

## Important Files

- `app/src/main/java/com/fcs/sfnewsapp/MyApp.kt`: SFMC initialization, notification channel, token sync, OpenDirect/custom-key notification tap handling.
- `app/src/main/java/com/fcs/sfnewsapp/MyFirebaseMessagingService.kt`: FCM token refresh and message handling.
- `app/src/main/java/com/fcs/sfnewsapp/MainActivity.kt`: UI, routes, opt in/out, user updates, copy controls.
- `app/src/main/java/com/fcs/sfnewsapp/AppIdentity.kt`: Contact key persistence.
- `docs/Push-notification-setup-guide.md`: Step-by-step enterprise implementation guide.
- `docs/deployment-instructions.md`: Build, deploy, and test notes.
- `docs/design.md`: Current app design notes.
- `docs/claude-context.md`: Project context for future AI/dev sessions.

## Security Notes

This repository is public. Do not commit:

- Real `google-services.json`.
- SFMC Access Token.
- Environment-specific SFMC endpoints.
- Production secrets.

Keep real config local or inject it through your enterprise build pipeline.

## SDK Version Note

This project uses:

```text
com.salesforce.marketingcloud:marketingcloudsdk:8.2.0
```

That version matches the requested `compileSdk 34` and `minSdk 24` constraints. Check Salesforce release notes before upgrading because newer SDKs may require newer compileSdk or minSdk values.
