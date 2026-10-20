package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    object Onboarding : Screen("onboarding", "Welcome", Icons.Filled.AutoAwesome, Icons.Filled.AutoAwesome)
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object LiveVoice : Screen("live_voice", "Live Voice", Icons.Filled.Mic, Icons.Outlined.Mic)
    object Chat : Screen("chat", "AI Chat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline)
    object Create : Screen("create", "Image Gen", Icons.Filled.Image, Icons.Outlined.Image)
    object Search : Screen("search", "Web Search", Icons.Filled.Search, Icons.Outlined.Search)
    object Automation : Screen("automation", "Automations", Icons.Filled.Schedule, Icons.Outlined.Schedule)
    object AutomationBuilder : Screen("automation_builder", "Builder", Icons.Filled.Schedule, Icons.Outlined.Schedule)
    object Productivity : Screen("productivity", "Tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircleOutline)
    object ExecutionHistory : Screen("execution_history", "History", Icons.Filled.History, Icons.Outlined.History)
    object Providers : Screen("providers", "Providers", Icons.Filled.Tune, Icons.Outlined.Tune)
    object Models : Screen("models", "Models", Icons.Filled.Psychology, Icons.Outlined.Psychology)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)

    companion object {
        val bottomNavItems = listOf(Home, LiveVoice, Chat, Create, Search, Productivity, Automation)
    }
}
