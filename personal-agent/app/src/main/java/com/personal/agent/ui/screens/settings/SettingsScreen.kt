package com.personal.agent.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.personal.agent.ai.ChatRepository
import com.personal.agent.ai.ProviderConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val repo = remember { ChatRepository(ctx) }
    val all = remember { ProviderConfig.defaults() }
    var config by remember { mutableStateOf(all.first()) }
    var apiKey by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var savedMsg by remember { mutableStateOf<String?>(null) }
    val snacks = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        config = repo.currentConfig(all)
        apiKey = repo.keys.getApiKey(config.id)
    }
    LaunchedEffect(savedMsg) { savedMsg?.let { snacks.showSnackbar(it); savedMsg = null } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings · Provider") }) },
        snackbarHost = { SnackbarHost(snacks) }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Keys are stored only in EncryptedSharedPreferences. Never hardcoded, never logged.")
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = config.label, onValueChange = {}, readOnly = true,
                    label = { Text("Provider") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    all.forEach { c ->
                        DropdownMenuItem(text = { Text(c.label) }, onClick = {
                            expanded = false
                            scope.launch {
                                config = c.copy(model = c.model, baseUrl = c.baseUrl)
                                apiKey = repo.keys.getApiKey(c.id)
                            }
                        })
                    }
                }
            }
            OutlinedTextField(value = config.baseUrl, onValueChange = { config = config.copy(baseUrl = it) }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = config.model, onValueChange = { config = config.copy(model = it) }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = apiKey, onValueChange = { apiKey = it }, label = { Text("API key") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Button(onClick = {
                scope.launch {
                    repo.saveConfig(config)
                    if (apiKey.isBlank()) repo.keys.clearApiKey(config.id)
                    else repo.keys.setApiKey(config.id, apiKey.trim())
                    savedMsg = "Saved securely."
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save securely") }
            Button(onClick = {
                scope.launch { repo.keys.clearApiKey(config.id); apiKey = ""; savedMsg = "Key removed." }
            }, modifier = Modifier.fillMaxWidth()) { Text("Remove key") }
            Text("Phase 1 scope: Chat + Projects + Settings. Agent modes, Terminal, Browser, Builds arrive in later phases — no fake buttons added.")
        }
    }
}
