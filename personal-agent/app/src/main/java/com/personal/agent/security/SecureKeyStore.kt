package com.personal.agent.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Secure storage for API keys. Never hardcoded, never in plain prefs or memory logs. */
class SecureKeyStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "provider_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getApiKey(providerId: String): String = prefs.getString("key_$providerId", "") ?: ""
    fun setApiKey(providerId: String, key: String) { prefs.edit().putString("key_$providerId", key).apply() }
    fun clearApiKey(providerId: String) { prefs.edit().remove("key_$providerId").apply() }
    fun hasApiKey(providerId: String): Boolean = getApiKey(providerId).isNotBlank()
}
