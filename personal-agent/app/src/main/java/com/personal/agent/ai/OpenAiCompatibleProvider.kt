package com.personal.agent.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Real OpenAI-compatible chat-completions client with SSE streaming.
 * Works with OpenAI, OpenRouter, Gemini-openai endpoint, and any /chat/completions server.
 */
class OpenAiCompatibleProvider(
    override val config: ProviderConfig,
    private val client: OkHttpClient = defaultClient()
) : AiProvider {

    override suspend fun streamChat(
        messages: List<ChatMessage>,
        apiKey: String,
        onDelta: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "API key missing for ${config.label}" }
        val json = Json { ignoreUnknownKeys = true }
        val bodyJson = buildJsonObject {
            put("model", config.model)
            put("stream", true)
            putJsonArray("messages") {
                messages.forEach { m ->
                    add(buildJsonObject {
                        put("role", m.role)
                        put("content", m.content)
                    })
                }
            }
        }.toString()

        val req = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        val full = StringBuilder()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val err = resp.body?.string()?.take(500) ?: "HTTP ${resp.code}"
                throw IllegalStateException("Provider error (${resp.code}): $err")
            }
            val source = resp.body?.source() ?: throw IllegalStateException("Empty response body")
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break
                if (data.isEmpty()) continue
                try {
                    val el = json.parseToJsonElement(data).jsonObject
                    val delta = el["choices"]?.jsonArray?.getOrNull(0)
                        ?.jsonObject?.get("delta")?.jsonObject
                        ?.get("content")?.jsonPrimitive?.content
                        ?: el["choices"]?.jsonArray?.getOrNull(0)
                            ?.jsonObject?.get("message")?.jsonObject
                            ?.get("content")?.jsonPrimitive?.content
                    if (!delta.isNullOrEmpty()) {
                        full.append(delta)
                        withContext(Dispatchers.Main) { onDelta(delta) }
                    }
                } catch (_: Exception) { /* skip malformed SSE chunk */ }
            }
        }
        full.toString()
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
