package com.personal.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.personal.agent.ai.ChatRepository
import com.personal.agent.ui.navigation.Routes
import com.personal.agent.ui.screens.chat.ChatScreen
import com.personal.agent.ui.screens.chat.ChatViewModel
import com.personal.agent.ui.screens.projects.ProjectsScreen
import com.personal.agent.ui.screens.settings.SettingsScreen
import com.personal.agent.ui.theme.PersonalAgentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PersonalAgentTheme { AppShell() } }
    }
}

@Composable
fun AppShell() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: Routes.CHAT
    val ctx = LocalContext.current
    val app = ctx.applicationContext as PersonalAgentApp
    val repo = remember { ChatRepository(ctx) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = route == Routes.CHAT, onClick = { nav.navigate(Routes.CHAT) },
                    icon = { Icon(Icons.Filled.Chat, null) }, label = { Text("Chat") }
                )
                NavigationBarItem(
                    selected = route == Routes.PROJECTS, onClick = { nav.navigate(Routes.PROJECTS) },
                    icon = { Icon(Icons.Filled.Folder, null) }, label = { Text("Projects") }
                )
                NavigationBarItem(
                    selected = route == Routes.SETTINGS, onClick = { nav.navigate(Routes.SETTINGS) },
                    icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("Settings") }
                )
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = Routes.CHAT, modifier = Modifier.padding(pad)) {
            composable(Routes.CHAT) {
                val vm: ChatViewModel = viewModel(factory = ChatVmFactory(app, repo))
                ChatScreen(vm)
            }
            composable(Routes.PROJECTS) { ProjectsScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
        }
    }
}

class ChatVmFactory(
    private val app: PersonalAgentApp,
    private val repo: ChatRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return com.personal.agent.ui.screens.chat.ChatViewModel(app.db, repo) as T
    }
}
