package com.driftdetector.app.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.driftdetector.app.R
import com.driftdetector.app.presentation.screen.AIAssistantScreen
import com.driftdetector.app.presentation.screen.DriftDashboardScreen
import com.driftdetector.app.presentation.screen.InstantDriftFixScreen
import com.driftdetector.app.presentation.screen.ModelManagementScreen
import com.driftdetector.app.presentation.screen.ModelUploadScreen
import com.driftdetector.app.presentation.screen.PatchManagementScreen
import com.driftdetector.app.presentation.screen.SettingsScreen
import com.driftdetector.app.presentation.screen.ThemeMode
import com.driftdetector.app.presentation.theme.DriftDetectorTheme
import com.driftdetector.app.presentation.viewmodel.SettingsViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.driftdetector.app.core.monitoring.ModelMonitoringService
import com.driftdetector.app.core.util.PermissionHelper
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private val monitoringService: ModelMonitoringService by inject()

    // Permission launchers
    private lateinit var storagePermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var manageStorageLauncher: ActivityResultLauncher<android.content.Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("ACTIVITY", "=== MainActivity.onCreate() START ===")
        Timber.d("MainActivity onCreate called")

        try {
            super.onCreate(savedInstanceState)
            Log.d("ACTIVITY", "✓ super.onCreate() completed")

            // Initialize permission launchers
            initializePermissionLaunchers()

            // Start continuous model monitoring
            monitoringService.startMonitoring()
            Log.d("ACTIVITY", "✓ Monitoring service started")

            // Check and request storage permissions
            checkStoragePermissions()

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

    /**
     * Initialize permission request launchers
     */
    private fun initializePermissionLaunchers() {
        // Standard storage permissions launcher
        storagePermissionLauncher =
            PermissionHelper.createStoragePermissionLauncher(this) { granted ->
                if (granted) {
                    Timber.i("✅ Storage permissions granted")
                } else {
                    Timber.w("⚠️ Storage permissions denied - file access may be limited")
                    // Show explanation to user
                    showPermissionDeniedMessage()
                }
            }

        // MANAGE_EXTERNAL_STORAGE permission launcher (Android 11+)
        manageStorageLauncher = PermissionHelper.createManageStorageLauncher(this) { granted ->
            if (granted) {
                Timber.i("✅ Full storage access granted")
            } else {
                Timber.w("⚠️ Full storage access denied - requesting standard permissions")
                // Fallback to standard permissions
                PermissionHelper.requestStoragePermissions(storagePermissionLauncher)
            }
        }
    }

    /**
     * Check if storage permissions are granted, request if not
     */
    private fun checkStoragePermissions() {
        if (!PermissionHelper.hasStoragePermissions(this)) {
            Timber.d("📋 Storage permissions not granted, requesting...")

            // For Android 11+ (API 30+), try to request MANAGE_EXTERNAL_STORAGE first
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // Show rationale first (optional)
                Timber.d(PermissionHelper.getPermissionRationale())

                // Request MANAGE_EXTERNAL_STORAGE for full access
                PermissionHelper.requestManageStoragePermission(this, manageStorageLauncher)
            } else {
                // For older versions, request standard permissions
                PermissionHelper.requestStoragePermissions(storagePermissionLauncher)
            }
        } else {
            Timber.i("✅ Storage permissions already granted")
        }
    }

    /**
     * Show message when permissions are denied
     */
    private fun showPermissionDeniedMessage() {
        Timber.w(
            """
            ⚠️ Storage access denied
            
            ${PermissionHelper.getPermissionRationale()}
            
            ${PermissionHelper.getManualPermissionInstructions(this)}
        """.trimIndent()
        )
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
        monitoringService.shutdown()
        Timber.d("MainActivity onDestroy - monitoring service shutdown")
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
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // App Logo - Using the actual launcher icon with circular clip
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                            contentDescription = "DriftGuard Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        // App Title
                        Text(
                            "DriftGuardAI",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            // Hide bottom bar on PatchBot screen
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
            // Show FAB on all screens except PatchBot
            if (currentRoute != Screen.AIAssistant.route) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Screen.AIAssistant.route)
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Psychology, contentDescription = "PatchBot")
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
                DriftDashboardScreen(
                    onNavigateToInstantDriftFix = {
                        navController.navigate(Screen.InstantDriftFix.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Models.route) {
                ModelManagementScreen(
                    onNavigateToUpload = {
                        navController.navigate(Screen.ModelUpload.route)
                    },
                    onNavigateToInstantDriftFix = {
                        navController.navigate(Screen.InstantDriftFix.route)
                    }
                )
            }
            composable(Screen.ModelUpload.route) {
                ModelUploadScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToDashboard = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToPatches = {
                        navController.navigate(Screen.Patches.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
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
            composable(Screen.InstantDriftFix.route) {
                InstantDriftFixScreen(
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
    object ModelUpload : Screen("model_upload", "Upload", Icons.Filled.CloudUpload)
    object Patches : Screen("patches", "Patches", Icons.Filled.Build)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    object AIAssistant : Screen("ai_assistant", "PatchBot", Icons.Filled.Psychology)
    object InstantDriftFix : Screen("instant_drift_fix", "Instant Drift Fix", Icons.Filled.Speed)
}
