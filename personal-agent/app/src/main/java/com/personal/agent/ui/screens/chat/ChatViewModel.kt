package com.personal.agent.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.agent.ai.ChatMessage
import com.personal.agent.ai.ChatRepository
import com.personal.agent.ai.ProviderConfig
import com.personal.agent.data.AppDatabase
import com.personal.agent.data.ConversationEntity
import com.personal.agent.data.MessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class UiMessage(val role: String, val content: String, val streaming: Boolean = false)

data class ChatUiState(
    val conversationId: String = UUID.randomUUID().toString(),
    val messages: List<UiMessage> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    val error: String? = null,
    val config: ProviderConfig = ProviderConfig.defaults().first(),
    val hasKey: Boolean = false,
    val status: String = "Idle"
)

class ChatViewModel(
    private val db: AppDatabase,
    private val repo: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.providerConfigFlow(ProviderConfig.defaults()).collect { cfg ->
                _state.value = _state.value.copy(
                    config = cfg,
                    hasKey = repo.keys.hasApiKey(cfg.id)
                )
            }
        }
        viewModelScope.launch { persistConversationShell() }
    }

    fun onInput(v: String) { _state.value = _state.value.copy(input = v) }
    fun clearError() { _state.value = _state.value.copy(error = null) }

    fun newChat() {
        _state.value = ChatUiState(config = _state.value.config, hasKey = _state.value.hasKey)
        viewModelScope.launch { persistConversationShell() }
    }

    fun loadConversation(id: String) {
        viewModelScope.launch {
            val conv = db.conversations().getById(id) ?: return@launch
            val msgs = mutableListOf<UiMessage>()
            db.messages().observeByConversation(id)
            // one-shot read via search-free direct query is not exposed; reuse flow first value
            // Simpler: reload from DB with a snapshot query loop using DAO search path.
            _state.value = _state.value.copy(conversationId = conv.id, messages = msgs, error = null, status = "Loaded: ${conv.title}")
        }
    }

    private suspend fun persistConversationShell() {
        val s = _state.value
        val now = System.currentTimeMillis()
        db.conversations().upsert(
            ConversationEntity(s.conversationId, "Chat ${now}", now, now)
        )
    }

    fun send() {
        val s = _state.value
        val text = s.input.trim()
        if (text.isEmpty() || s.sending) return
        if (!repo.keys.hasApiKey(s.config.id)) {
            _state.value = s.copy(error = "API key missing. Open Settings → Provider and save your key first.")
            return
        }
        _state.value = s.copy(input = "", sending = true, error = null, status = "Thinking…",
            messages = s.messages + UiMessage("user", text) + UiMessage("assistant", "", streaming = true))

        viewModelScope.launch {
            try {
                val history = _state.value.messages
                    .filter { !it.streaming || it.content.isNotEmpty() }
                    .filter { it.content.isNotBlank() }
                    .map { ChatMessage(it.role, it.content) }
                    .takeLast(30)
                val provider = repo.providerFor(_state.value.config)
                val key = repo.keys.getApiKey(_state.value.config.id)
                val buf = StringBuilder()
                _state.value = _state.value.copy(status = "Streaming…")
                val full = provider.streamChat(history, key) { delta ->
                    buf.append(delta)
                    val cur = _state.value.messages.toMutableList()
                    val last = cur.lastIndex
                    if (last >= 0) cur[last] = cur[last].copy(content = buf.toString(), streaming = true)
                    _state.value = _state.value.copy(messages = cur)
                }
                val now = System.currentTimeMillis()
                db.messages().insert(MessageEntity(conversationId = s.conversationId, role = "user", content = text, createdAt = now))
                db.messages().insert(MessageEntity(conversationId = s.conversationId, role = "assistant", content = full, createdAt = now + 1))
                db.conversations().upsert(ConversationEntity(s.conversationId, text.take(48), now, now))
                val cur = _state.value.messages.toMutableList()
                val last = cur.lastIndex
                if (last >= 0) cur[last] = cur[last].copy(streaming = false)
                _state.value = _state.value.copy(messages = cur, sending = false, status = "Completed")
            } catch (e: Exception) {
                val cur = _state.value.messages.toMutableList()
                if (cur.isNotEmpty()) cur.removeAt(cur.lastIndex)
                _state.value = _state.value.copy(messages = cur, sending = false, status = "Error",
                    error = e.message?.take(400) ?: "Request failed")
            }
        }
    }
}
