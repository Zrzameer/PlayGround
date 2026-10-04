package com.personal.agent.ui.screens.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.personal.agent.projects.ProjectInfo
import com.personal.agent.projects.ProjectManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen() {
    val ctx = LocalContext.current
    val pm = remember { ProjectManager(ctx) }
    var projects by remember { mutableStateOf<List<ProjectInfo>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun refresh() { scope.launch { projects = pm.list() } }
    LaunchedEffect(Unit) { refresh() }

    Scaffold(topBar = { TopAppBar(title = { Text("Projects") }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.weight(1f), placeholder = { Text("New project name") }, singleLine = true)
                Button(onClick = { scope.launch { if (name.isNotBlank()) { pm.create(name); name = ""; projects = pm.list() } } }) { Text("Create") }
            }
            Text("Isolated workspaces under app-private storage. Phase 1: local create/delete. Git/ZIP import + scanner arrive in Phase 3.")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(projects) { p ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text(p.name)
                            Text(p.path, maxLines = 2)
                            Text("${p.fileCount} files")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { scope.launch { pm.delete(p.id); projects = pm.list() } }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}
