package com.fcs.sfnewsapp

import android.app.PendingIntent
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.salesforce.marketingcloud.MarketingCloudConfig
import com.salesforce.marketingcloud.MarketingCloudSdk
import com.salesforce.marketingcloud.notifications.NotificationCustomizationOptions
import com.salesforce.marketingcloud.notifications.NotificationManager as SfmcNotificationManager
import com.salesforce.marketingcloud.sfmcsdk.InitializationStatus
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdkModuleConfig
import java.util.Locale

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createMarketingNotificationChannel()
        if (hasMarketingCloudConfig()) {
            initializeMarketingCloud()
        } else {
            Log.w(TAG, "SFMC initialization skipped. Replace placeholder SFMC values in MyApp.kt.")
        }
    }

    private fun initializeMarketingCloud() {
        SFMCSdk.configure(
            applicationContext,
            SFMCSdkModuleConfig.build {
                val marketingCloudConfigBuilder = MarketingCloudConfig.builder()
                    .setApplicationId(SFMC_APP_ID)
                    .setAccessToken(SFMC_ACCESS_TOKEN)
                    .setSenderId(FCM_SENDER_ID)
                    .setMarketingCloudServerUrl(SFMC_SERVER_URL)
                    .setAnalyticsEnabled(true)
                    .setNotificationCustomizationOptions(
                        NotificationCustomizationOptions.create(
                            R.drawable.ic_notification,
                            SfmcNotificationManager.NotificationLaunchIntentProvider { context, message ->
                                PendingIntent.getActivity(
                                    context,
                                    message.url?.hashCode() ?: 0,
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        resolveNotificationUri(message.url, message.customKeys),
                                        context,
                                        MainActivity::class.java,
                                    ).apply {
                                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    },
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                                )
                            },
                            SfmcNotificationManager.NotificationChannelIdProvider { _, _ ->
                                MARKETING_CHANNEL_ID
                            },
                        ),
                    )

                if (SFMC_MID.isNotBlank()) {
                    marketingCloudConfigBuilder.setMid(SFMC_MID)
                }

                pushModuleConfig = marketingCloudConfigBuilder.build(this@MyApp)
            },
        ) { status ->
            Log.d(TAG, "SFMC initialization status: $status")
            if (status.status == InitializationStatus.SUCCESS) {
                setInstallTimeIdentity()
                syncCurrentFcmTokenToMarketingCloud()
            } else {
                Log.w(TAG, "SFMC initialization was not usable: $status")
            }
        }
    }

    private fun hasMarketingCloudConfig(): Boolean {
        return listOf(
            SFMC_APP_ID,
            SFMC_ACCESS_TOKEN,
            FCM_SENDER_ID,
            SFMC_SERVER_URL,
            SFMC_MID,
        ).none { it.startsWith("YOUR_") || it.contains("YOUR_") }
    }

    private fun createMarketingNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            MARKETING_CHANNEL_ID,
            "Marketing Notifications",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun setInstallTimeIdentity() {
        val contactKey = AppIdentity.getOrCreateContactKey(this)

        SFMCSdk.requestSdk { sdk ->
            sdk.identity.setProfileId(contactKey)
            sdk.identity.setProfileAttribute("locale", Locale.getDefault().toString())
            sdk.identity.setProfileAttribute("platform", "android")
            sdk.identity.setProfileAttribute("appVersion", BuildConfig.VERSION_NAME)
            sdk.mp { pushModule ->
                pushModule.registrationManager.edit()
                    .addTag("general")
                    .commit()
            }
        }
    }

    private fun syncCurrentFcmTokenToMarketingCloud() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                MarketingCloudSdk.requestSdk { sdk ->
                    sdk.pushMessageManager.setPushToken(token)
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Unable to sync current FCM token to SFMC", error)
            }
    }

    private fun resolveNotificationUri(
        openDirectUrl: String?,
        customKeys: Map<String, String>,
    ): Uri {
        val deeplink = customKeys["deeplink"]
            ?: customKeys["deepLink"]
            ?: customKeys["url"]
            ?: openDirectUrl
            ?: DEFAULT_DEEP_LINK
        return Uri.parse(deeplink)
    }

    private companion object {
        const val TAG = "MyApp"
        const val MARKETING_CHANNEL_ID = "marketing_default"

        const val SFMC_APP_ID = "YOUR_SFMC_APP_ID"
        const val SFMC_ACCESS_TOKEN = "YOUR_SFMC_ACCESS_TOKEN"
        const val FCM_SENDER_ID = "YOUR_FCM_SENDER_ID"
        const val SFMC_SERVER_URL = "https://YOUR_SUBDOMAIN.device.marketingcloudapis.com/"
        const val SFMC_MID = ""
        const val DEFAULT_DEEP_LINK = "sfnews://app"
    }
}
