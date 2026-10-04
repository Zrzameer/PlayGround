package com.personal.agent.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val snacks = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(state.error) {
        state.error?.let { snacks.showSnackbar(it); vm.clearError() }
    }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Agent · ${state.config.label}") },
                actions = { OutlinedButton(onClick = { vm.newChat() }) { Text("New") } }
            )
        },
        snackbarHost = { SnackbarHost(snacks) },
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(8.dp)) {
                Text(
                    if (!state.hasKey) "No API key — set it in Settings."
                    else "Status: ${state.status} · ${state.config.model}",
                    style = MaterialTheme.typography.labelSmall
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.input,
                        onValueChange = vm::onInput,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask anything…") },
                        maxLines = 4
                    )
                    Button(onClick = { vm.send() }, enabled = !state.sending) {
                        Text(if (state.sending) "…" else "Send")
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(pad).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.messages) { m ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            if (m.role == "user") "You" else "Agent${if (m.streaming) " · typing…" else ""}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(m.content.ifEmpty { "…" })
                        if (m.role == "assistant" && m.content.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { copyText(ctx, m.content); scope.launch { snacks.showSnackbar("Copied") } }) { Text("Copy") }
                                OutlinedButton(onClick = { shareText(ctx, m.content) }) { Text("Share") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun copyText(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("agent", text))
}

private fun shareText(ctx: Context, text: String) {
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
    }, "Share response"))
}
