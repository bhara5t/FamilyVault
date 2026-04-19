package com.example.familyvault.security

import android.content.Context

object PinStorage {

    private const val PREF_NAME = "vault_prefs"
    private const val KEY_PIN = "user_pin"

    fun savePin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    fun getPin(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PIN, null)
    }

    fun isPinSet(context: Context): Boolean {
        return getPin(context) != null
    }
}