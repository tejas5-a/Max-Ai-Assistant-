package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.data.repository.AutomationRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.TaskRepository
import com.example.ui.components.NeumorphicTopAppBar
import com.example.ui.screens.automation.AutomationScreen
import com.example.ui.screens.automation.AutomationViewModel
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.chat.ChatViewModel
import com.example.ui.screens.create.CreateScreen
import com.example.ui.screens.create.CreateViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.productivity.ProductivityScreen
import com.example.ui.screens.productivity.ProductivityViewModel
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.search.SearchViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.voice.LiveVoiceScreen
import com.example.ui.screens.voice.LiveVoiceViewModel
import com.example.ui.theme.MaxAiTheme
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val preferencesRepository = PreferencesRepository(context)
    val chatRepository = ChatRepository(db.chatDao(), preferencesRepository)
    val taskRepository = TaskRepository(db.taskDao())
    val automationRepository = AutomationRepository(context, db.automationDao())

    val settingsState by preferencesRepository.settingsFlow.collectAsState(initial = null)
    val themeMode = settingsState?.themeMode ?: com.example.ui.theme.ThemeMode.SYSTEM

    MaxAiTheme(themeMode = themeMode) {
        val navController = rememberNavController()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val isOnboardingCompleted = settingsState?.isOnboardingCompleted == true
        val startDestination = if (isOnboardingCompleted) Screen.Home.route else Screen.Onboarding.route
        val isImmersiveScreen = currentRoute == Screen.Onboarding.route || currentRoute == Screen.LiveVoice.route

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Text(
                        text = "MAX AI Destinations",
                        modifier = Modifier.padding(16.dp),
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                    )

                    Screen.bottomNavItems.forEach { screen ->
                        NavigationDrawerItem(
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == screen.route) screen.activeIcon else screen.inactiveIcon,
                                    contentDescription = screen.title
                                )
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    NavigationDrawerItem(
                        label = { Text(Screen.Settings.title) },
                        selected = currentRoute == Screen.Settings.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.Settings.route)
                        },
                        icon = { Icon(imageVector = Screen.Settings.activeIcon, contentDescription = "Settings") },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    if (!isImmersiveScreen) {
                        NeumorphicTopAppBar(
                            isOpenRouterEnabled = settingsState?.isOpenRouterEnabled ?: true,
                            isPollinationsEnabled = settingsState?.isPollinationsEnabled ?: true,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onOpenSettings = { navController.navigate(Screen.Settings.route) }
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.padding(if (isImmersiveScreen) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
                ) {
                    composable(Screen.Onboarding.route) {
                        OnboardingScreen(
                            onContinueAsGuest = {
                                scope.launch { preferencesRepository.setOnboardingCompleted(true) }
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            },
                            onOpenSettings = {
                                scope.launch { preferencesRepository.setOnboardingCompleted(true) }
                                navController.navigate(Screen.Settings.route)
                            }
                        )
                    }

                    composable(Screen.Home.route) {
                        val homeViewModel = viewModel<HomeViewModel> {
                            HomeViewModel(preferencesRepository, taskRepository, automationRepository)
                        }
                        HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToLiveVoice = { navController.navigate(Screen.LiveVoice.route) },
                            onNavigateToChat = { prompt ->
                                navController.navigate(Screen.Chat.route)
                            },
                            onNavigateToCreate = { navController.navigate(Screen.Create.route) },
                            onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                            onNavigateToProductivity = { navController.navigate(Screen.Productivity.route) },
                            onNavigateToAutomation = { navController.navigate(Screen.Automation.route) },
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                        )
                    }

                    composable(Screen.LiveVoice.route) {
                        val liveVoiceViewModel = viewModel<LiveVoiceViewModel> {
                            LiveVoiceViewModel(context, preferencesRepository, taskRepository, automationRepository)
                        }
                        LiveVoiceScreen(
                            viewModel = liveVoiceViewModel,
                            onClose = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Chat.route) {
                        val chatViewModel = viewModel<ChatViewModel> {
                            ChatViewModel(context, chatRepository, preferencesRepository)
                        }
                        ChatScreen(
                            viewModel = chatViewModel,
                            onNavigateToLiveVoice = { navController.navigate(Screen.LiveVoice.route) },
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                        )
                    }

                    composable(Screen.Create.route) {
                        val createViewModel = viewModel<CreateViewModel> {
                            CreateViewModel(context, preferencesRepository)
                        }
                        CreateScreen(
                            viewModel = createViewModel,
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                        )
                    }

                    composable(Screen.Search.route) {
                        val searchViewModel = viewModel<SearchViewModel> {
                            SearchViewModel(preferencesRepository)
                        }
                        SearchScreen(viewModel = searchViewModel)
                    }

                    composable(Screen.Productivity.route) {
                        val productivityViewModel = viewModel<ProductivityViewModel> {
                            ProductivityViewModel(taskRepository)
                        }
                        ProductivityScreen(viewModel = productivityViewModel)
                    }

                    composable(Screen.Automation.route) {
                        val automationViewModel = viewModel<AutomationViewModel> {
                            AutomationViewModel(automationRepository)
                        }
                        AutomationScreen(
                            viewModel = automationViewModel,
                            onNavigateToBuilder = { navController.navigate(Screen.AutomationBuilder.route) },
                            onNavigateToHistory = { navController.navigate(Screen.ExecutionHistory.route) }
                        )
                    }

                    composable(Screen.AutomationBuilder.route) {
                        val automationViewModel = viewModel<AutomationViewModel> {
                            AutomationViewModel(automationRepository)
                        }
                        com.example.ui.screens.automation.AutomationBuilderScreen(
                            viewModel = automationViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.ExecutionHistory.route) {
                        val automationViewModel = viewModel<AutomationViewModel> {
                            AutomationViewModel(automationRepository)
                        }
                        com.example.ui.screens.history.ExecutionHistoryScreen(
                            viewModel = automationViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Providers.route) {
                        val settingsViewModel = viewModel<SettingsViewModel> {
                            SettingsViewModel(preferencesRepository, chatRepository, taskRepository)
                        }
                        com.example.ui.screens.providers.ProvidersScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Models.route) {
                        val settingsViewModel = viewModel<SettingsViewModel> {
                            SettingsViewModel(preferencesRepository, chatRepository, taskRepository)
                        }
                        com.example.ui.screens.models.ModelsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Settings.route) {
                        val settingsViewModel = viewModel<SettingsViewModel> {
                            SettingsViewModel(preferencesRepository, chatRepository, taskRepository)
                        }
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateToProviders = { navController.navigate(Screen.Providers.route) },
                            onNavigateToModels = { navController.navigate(Screen.Models.route) }
                        )
                    }
                }
            }
        }
    }
}
