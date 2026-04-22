# Push Notification Setup Guide

This guide explains how to add Firebase Cloud Messaging and Salesforce Marketing Cloud MobilePush to an Android enterprise app.

## 1. Prerequisites

- Android app package name registered in Firebase.
- Firebase Android app with `google-services.json`.
- SFMC MobilePush app with:
  - App ID
  - Access Token
  - App Endpoint
  - FCM Sender ID
  - MID if your business unit requires it
- Android minSdk/compileSdk compatible with the selected SFMC SDK version.
- Android 13+ notification permission flow.

Reference docs:

- Firebase Android setup: `https://firebase.google.com/docs/android/setup`
- Firebase Cloud Messaging Android client: `https://firebase.google.com/docs/cloud-messaging/android/client`
- SFMC MobilePush Android SDK: `https://developer.salesforce.com/docs/marketing/mobilepush/guide/android-sdk.html`
- SFMC OpenDirect: `https://help.salesforce.com/s/articleView?id=mktg.mc_mp_opendirect.htm&type=5`
- SFMC Custom Keys: `https://help.salesforce.com/s/articleView?id=mktg.mc_mp_custom_keys.htm&type=5`
- SFMC URL/custom key handling: `https://developer.salesforce.com/docs/marketing/mobilepush/guide/handle-urls-custom-keys.html`

## 2. Add Gradle Plugins

Root `build.gradle`:

```groovy
plugins {
    id "com.android.application" version "8.5.2" apply false
    id "org.jetbrains.kotlin.android" version "1.9.24" apply false
    id "com.google.gms.google-services" version "4.4.4" apply false
}
```

`settings.gradle` repositories:

```groovy
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://salesforce-marketingcloud.github.io/MarketingCloudSDK-Android/repository")
        }
    }
}
```

## 3. Add App Dependencies

`app/build.gradle`:

```groovy
plugins {
    id "com.android.application"
    id "org.jetbrains.kotlin.android"
    id "com.google.gms.google-services"
}

dependencies {
    implementation platform("com.google.firebase:firebase-bom:34.12.0")
    implementation "com.google.firebase:firebase-analytics"
    implementation "com.google.firebase:firebase-messaging"
    implementation "com.salesforce.marketingcloud:marketingcloudsdk:8.2.0"
}
```

Use a newer SFMC SDK only after checking its minSdk and compileSdk requirements.

## 4. Add Firebase Config

Download `google-services.json` from Firebase for the enterprise app package.

Place it where the Google Services plugin expects it:

```text
app/google-services.json
```

This sample project stores the source copy at:

```text
firebase/google-services.json
```

and copies it into the app module during Gradle processing.

## 5. Manifest Setup

Add permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Register the app class:

```xml
<application
    android:name=".MyApp"
    ...>
</application>
```

Register the Firebase messaging service:

```xml
<service
    android:name=".MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

Register deep links:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="sfnews" android:host="app" />
</intent-filter>
```

## 6. Initialize SFMC

Initialize SFMC in `Application.onCreate`.

Core setup:

```kotlin
SFMCSdk.configure(
    applicationContext,
    SFMCSdkModuleConfig.build {
        val builder = MarketingCloudConfig.builder()
            .setApplicationId(SFMC_APP_ID)
            .setAccessToken(SFMC_ACCESS_TOKEN)
            .setSenderId(FCM_SENDER_ID)
            .setMarketingCloudServerUrl(SFMC_SERVER_URL)
            .setAnalyticsEnabled(true)
            .setNotificationCustomizationOptions(notificationOptions())

        if (SFMC_MID.isNotBlank()) {
            builder.setMid(SFMC_MID)
        }

        pushModuleConfig = builder.build(this@MyApp)
    },
) { status ->
    if (status.status == InitializationStatus.SUCCESS) {
        setInstallTimeIdentity()
        syncCurrentFcmTokenToMarketingCloud()
    }
}
```

Create a notification channel before push display:

```kotlin
val channel = NotificationChannel(
    "marketing_default",
    "Marketing Notifications",
    NotificationManager.IMPORTANCE_DEFAULT,
)
getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
```

## 7. Handle Notification Taps and Deep Links

Use `NotificationLaunchIntentProvider` so SFMC notification taps open an app route.

```kotlin
NotificationCustomizationOptions.create(
    R.drawable.ic_notification,
    NotificationManager.NotificationLaunchIntentProvider { context, message ->
        PendingIntent.getActivity(
            context,
            message.notificationId(),
            Intent(
                Intent.ACTION_VIEW,
                resolveNotificationUri(message.url(), message.customKeys()),
                context,
                MainActivity::class.java,
            ).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    },
    NotificationManager.NotificationChannelIdProvider { _, _ ->
        "marketing_default"
    },
)
```

Resolve OpenDirect/custom keys:

```kotlin
private fun resolveNotificationUri(openDirectUrl: String?, customKeys: Map<String, String>): Uri {
    val deeplink = customKeys["deeplink"]
        ?: customKeys["deepLink"]
        ?: customKeys["url"]
        ?: openDirectUrl
        ?: "sfnews://app"
    return Uri.parse(deeplink)
}
```

Supported sample routes:

```text
sfnews://app
sfnews://app/device-info
sfnews://app/update-details
sfnews://app/product/breaking
sfnews://app/product/markets
sfnews://app/product/sports
sfnews://app/product/weather
```

## 8. Device Identity and Contact Key

Persist a contact key on first launch:

```kotlin
val contactKey = UUID.randomUUID().toString()
prefs.edit().putString("contact_key", contactKey).apply()
```

Send the contact key to SFMC:

```kotlin
SFMCSdk.requestSdk { sdk ->
    sdk.identity.setProfileId(contactKey)
    sdk.mp { push ->
        push.registrationManager.edit()
            .setContactKey(contactKey)
            .addTag("general")
            .commit()
    }
}
```

When the user changes contact key:

```kotlin
SFMCSdk.requestSdk { sdk ->
    sdk.mp { push ->
        push.registrationManager.edit()
            .setContactKey(newContactKey)
            .addTag("general")
            .commit()
    }
    sdk.identity.setProfileId(newContactKey)
}
```

## 9. FCM Token Handling

Forward token refreshes to SFMC:

```kotlin
override fun onNewToken(token: String) {
    MarketingCloudSdk.requestSdk { sdk ->
        sdk.pushMessageManager.setPushToken(token)
    }
}
```

Also sync the current token after SFMC initialization:

```kotlin
FirebaseMessaging.getInstance().token
    .addOnSuccessListener { token ->
        MarketingCloudSdk.requestSdk { sdk ->
            sdk.pushMessageManager.setPushToken(token)
        }
    }
```

This avoids waiting for a future token refresh before SFMC receives the token.

## 10. Opt In and Opt Out

Android 13+ requires runtime notification permission.

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
) {
    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_POST_NOTIFICATIONS)
}
```

Opt in:

```kotlin
SFMCSdk.requestSdk { sdk ->
    sdk.mp { push ->
        push.pushMessageManager.enablePush()
        push.registrationManager.edit()
            .setContactKey(contactKey)
            .addTag("general")
            .commit()
    }
    sdk.identity.setProfileId(contactKey)
}
```

Opt out:

```kotlin
SFMCSdk.requestSdk { sdk ->
    sdk.mp { push ->
        push.pushMessageManager.disablePush()
    }
}
```

If SFMC shows “Device is not opted in,” verify:

- Notification permission is allowed on the device.
- The app called `enablePush()`.
- SFMC registration has completed.
- FCM Sender ID matches Firebase.
- The device appears in MobilePush Audiences.

## 11. Update User Details

Send registration attributes:

```kotlin
SFMCSdk.requestSdk { sdk ->
    sdk.mp { push ->
        push.registrationManager.edit()
            .setAttribute("name", name)
            .setAttribute("email", email)
            .setAttribute("phone", phone)
            .setAttribute("address", address)
            .commit()
    }

    sdk.identity.setProfileAttribute("name", name)
    sdk.identity.setProfileAttribute("email", email)
    sdk.identity.setProfileAttribute("phone", phone)
    sdk.identity.setProfileAttribute("address", address)
}
```

Use attribute names that match the enterprise SFMC data model.

## 12. Receive Push Messages

Let SFMC handle Marketing Cloud messages first:

```kotlin
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    if (PushMessageManager.isMarketingCloudPush(remoteMessage)) {
        MarketingCloudSdk.requestSdk { sdk ->
            val handled = sdk.pushMessageManager.handleMessage(remoteMessage)
            if (!handled) {
                openDeepLink(remoteMessage)
            }
        }
        return
    }

    openDeepLink(remoteMessage)
}
```

Fallback custom data handling:

```kotlin
val uri = remoteMessage.data["deeplink"]
    ?: remoteMessage.data["deepLink"]
    ?: remoteMessage.data["url"]
    ?: return
```

## 13. Send Deep Link Push from SFMC

Preferred:

1. Enable OpenDirect for MobilePush.
2. Set the OpenDirect URL to one of:

```text
sfnews://app/product/breaking
sfnews://app/product/markets
sfnews://app/product/sports
sfnews://app/product/weather
sfnews://app/device-info
sfnews://app/update-details
```

Alternative:

1. Enable Custom Keys.
2. Add:

```text
deeplink = sfnews://app/product/weather
```

The app checks custom keys first, then OpenDirect.

## 14. Testing Checklist

1. Install the app.
2. Allow notification permission.
3. Open Device Info.
4. Tap Opt in.
5. Confirm Contact Key, FCM Token, SFMC System Token, Device ID, and SFMC Contact Key are visible.
6. Send a basic SFMC test push.
7. Send an SFMC deep link push with OpenDirect:

```text
sfnews://app/product/weather
```

8. Tap the push and confirm the Weather page opens.

## 15. Enterprise Implementation Notes

- Store secrets/config outside source control when possible.
- Put local SFMC values in ignored `sfmc.properties` and inject them through `BuildConfig`.
- Use environment-specific Firebase and SFMC configs.
- Align contact key strategy with the identity provider.
- Use stable SFMC attribute names.
- Keep opt-in state visible in QA builds.
- Log SFMC initialization status in debug builds.
- Do not show raw tokens in production UI.
- Validate OpenDirect routes before launch.
