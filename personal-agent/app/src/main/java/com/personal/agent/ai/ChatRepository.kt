package com.personal.agent.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.personal.agent.security.SecureKeyStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.prefs by preferencesDataStore("agent_prefs")
private val KEY_PROVIDER = stringPreferencesKey("provider_id")
private val KEY_MODEL = stringPreferencesKey("model")
private val KEY_BASEURL = stringPreferencesKey("base_url")

class ChatRepository(private val context: Context) {
    val keys = SecureKeyStore(context)

    fun providerConfigFlow(all: List<ProviderConfig>): Flow<ProviderConfig> =
        context.prefs.data.map { p ->
            val id = p[KEY_PROVIDER] ?: "openai"
            val base = all.firstOrNull { it.id == id } ?: all.first()
            base.copy(
                model = p[KEY_MODEL] ?: base.model,
                baseUrl = p[KEY_BASEURL] ?: base.baseUrl
            )
        }

    suspend fun currentConfig(all: List<ProviderConfig>): ProviderConfig =
        providerConfigFlow(all).first()

    suspend fun saveConfig(c: ProviderConfig) {
        context.prefs.edit { e ->
            e[KEY_PROVIDER] = c.id
            e[KEY_MODEL] = c.model
            e[KEY_BASEURL] = c.baseUrl
        }
    }

    fun providerFor(config: ProviderConfig): AiProvider = OpenAiCompatibleProvider(config)
}
