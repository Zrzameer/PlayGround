package com.personal.agent.ai

import kotlinx.serialization.Serializable

enum class ProviderType { OPENAI_COMPATIBLE, GEMINI, ANTHROPIC, OPENROUTER }

@Serializable
data class ProviderConfig(
    val id: String,
    val label: String,
    val type: ProviderType,
    val baseUrl: String,
    val model: String
) {
    companion object {
        fun defaults(): List<ProviderConfig> = listOf(
            ProviderConfig("openai", "OpenAI", ProviderType.OPENAI_COMPATIBLE, "https://api.openai.com/v1", "gpt-4o-mini"),
            ProviderConfig("openrouter", "OpenRouter", ProviderType.OPENROUTER, "https://openrouter.ai/api/v1", "openai/gpt-4o-mini"),
            ProviderConfig("gemini", "Gemini (OpenAI-compat)", ProviderType.GEMINI, "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-2.0-flash"),
            ProviderConfig("anthropic", "Anthropic (OpenAI-compat)", ProviderType.ANTHROPIC, "https://api.anthropic.com/v1", "claude-3-5-sonnet-latest"),
            ProviderConfig("custom", "Custom (OpenAI-compat)", ProviderType.OPENAI_COMPATIBLE, "https://example.com/v1", "model")
        )
    }
}

data class ChatMessage(val role: String, val content: String)

interface AiProvider {
    val config: ProviderConfig
    suspend fun streamChat(
        messages: List<ChatMessage>,
        apiKey: String,
        onDelta: (String) -> Unit
    ): String
}
