package com.driftdetector.app.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.driftdetector.app.presentation.screen.AIAssistantScreen
import com.driftdetector.app.presentation.screen.DriftDashboardScreen
import com.driftdetector.app.presentation.screen.ModelManagementScreen
import com.driftdetector.app.presentation.screen.PatchManagementScreen
import com.driftdetector.app.presentation.screen.SettingsScreen
import com.driftdetector.app.presentation.screen.ThemeMode
import com.driftdetector.app.presentation.theme.DriftDetectorTheme
import com.driftdetector.app.presentation.viewmodel.SettingsViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("ACTIVITY", "=== MainActivity.onCreate() START ===")
        Timber.d("MainActivity onCreate called")

        try {
            super.onCreate(savedInstanceState)
            Log.d("ACTIVITY", "✓ super.onCreate() completed")

            setContent {
                Log.d("ACTIVITY", "✓ setContent block entered")

                // Get theme settings
                val settingsViewModel: SettingsViewModel = koinViewModel()
                val settingsState by settingsViewModel.uiState.collectAsState()

                val darkTheme = when (settingsState.themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.AUTO -> isSystemInDarkTheme()
                }

                // Log theme changes
                Timber.d("🎨 MainActivity: Theme mode = ${settingsState.themeMode}, darkTheme = $darkTheme")

                DriftDetectorTheme(darkTheme = darkTheme) {
                    Log.d("ACTIVITY", "✓ DriftDetectorTheme applied")
                    DriftDetectorApp()
                }
            }
            Log.d("ACTIVITY", "✓ setContent completed")
        } catch (e: Exception) {
            Log.e("ACTIVITY", "✗ MainActivity.onCreate() FAILED", e)
            Timber.e(e, "MainActivity onCreate failed")
            throw e
        }

        Log.d("ACTIVITY", "=== MainActivity.onCreate() COMPLETE ===")
    }

    override fun onStart() {
        super.onStart()
        Log.d("ACTIVITY", "MainActivity.onStart()")
        Timber.d("MainActivity onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("ACTIVITY", "MainActivity.onResume()")
        Timber.d("MainActivity onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d("ACTIVITY", "MainActivity.onPause()")
        Timber.d("MainActivity onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("ACTIVITY", "MainActivity.onStop()")
        Timber.d("MainActivity onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ACTIVITY", "MainActivity.onDestroy()")
        Timber.d("MainActivity onDestroy")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriftDetectorApp() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Dashboard,
        Screen.Models,
        Screen.Patches,
        Screen.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DriftGuardAI") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            // Hide bottom bar on AI Assistant screen
            if (currentRoute != Screen.AIAssistant.route) {
                NavigationBar {
                    val currentDestination = navBackStackEntry?.destination

                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == screen.route
                            } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // Show FAB on all screens except AI Assistant
            if (currentRoute != Screen.AIAssistant.route) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Screen.AIAssistant.route)
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Psychology, contentDescription = "AI Assistant")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DriftDashboardScreen()
            }
            composable(Screen.Models.route) {
                ModelManagementScreen()
            }
            composable(Screen.Patches.route) {
                PatchManagementScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(Screen.AIAssistant.route) {
                AIAssistantScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

sealed class Screen(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Dashboard)
    object Models : Screen("models", "Models", Icons.Filled.Memory)
    object Patches : Screen("patches", "Patches", Icons.Filled.Build)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    object AIAssistant : Screen("ai_assistant", "AI Assistant", Icons.Filled.Psychology)
}
