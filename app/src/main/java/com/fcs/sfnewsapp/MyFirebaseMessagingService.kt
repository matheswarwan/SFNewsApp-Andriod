package com.fcs.sfnewsapp

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.salesforce.marketingcloud.MarketingCloudSdk
import com.salesforce.marketingcloud.messages.push.PushMessageManager

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Received new FCM token")

        MarketingCloudSdk.requestSdk { sdk ->
            sdk.pushMessageManager.setPushToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

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

    private fun openDeepLink(remoteMessage: RemoteMessage) {
        val uri = remoteMessage.data["deeplink"]
            ?: remoteMessage.data["deepLink"]
            ?: remoteMessage.data["url"]
            ?: return
        Log.d(TAG, "Opening notification deep link: $uri")

        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
        )
    }

    private companion object {
        const val TAG = "MyFirebaseMsgService"
    }
}
