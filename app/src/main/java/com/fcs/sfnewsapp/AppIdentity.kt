package com.fcs.sfnewsapp

import android.content.Context
import java.util.UUID

object AppIdentity {
    private const val PREFS_NAME = "sfnewsapp_prefs"
    private const val CONTACT_KEY = "contact_key"

    fun getOrCreateContactKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(CONTACT_KEY, null)?.let { return it }

        val contactKey = UUID.randomUUID().toString()
        prefs.edit().putString(CONTACT_KEY, contactKey).apply()
        return contactKey
    }

    fun setContactKey(context: Context, contactKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(CONTACT_KEY, contactKey)
            .apply()
    }
}
