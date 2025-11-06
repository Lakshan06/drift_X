package com.driftdetector.app.core.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Helper class to manage storage permissions across different Android versions
 */
object PermissionHelper {

    /**
     * Get required storage permissions based on Android version
     */
    fun getRequiredStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) - Use granular media permissions
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 (API 30-32) - Use READ_EXTERNAL_STORAGE
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            // Android 10 and below (API 29 and below)
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * Check if all required storage permissions are granted
     */
    fun hasStoragePermissions(context: Context): Boolean {
        // For Android 11+, check if we have MANAGE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val hasManageStorage = Environment.isExternalStorageManager()
            if (hasManageStorage) {
                Timber.d("✓ Has MANAGE_EXTERNAL_STORAGE permission")
                return true
            }
        }

        // Check standard permissions
        val permissions = getRequiredStoragePermissions()
        val allGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            Timber.d("✓ All storage permissions granted: ${permissions.joinToString()}")
        } else {
            Timber.w("⚠️ Missing storage permissions")
        }

        return allGranted
    }

    /**
     * Check if we should show permission rationale
     */
    fun shouldShowPermissionRationale(activity: ComponentActivity, permission: String): Boolean {
        return activity.shouldShowRequestPermissionRationale(permission)
    }

    /**
     * Create a permission launcher for storage permissions
     */
    fun createStoragePermissionLauncher(
        activity: ComponentActivity,
        onResult: (Boolean) -> Unit
    ): ActivityResultLauncher<Array<String>> {
        return activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }

            if (allGranted) {
                Timber.i("✅ All storage permissions granted")
                onResult(true)
            } else {
                Timber.w("❌ Storage permissions denied")
                onResult(false)
            }
        }
    }

    /**
     * Create a launcher for MANAGE_EXTERNAL_STORAGE permission (Android 11+)
     */
    fun createManageStorageLauncher(
        activity: ComponentActivity,
        onResult: (Boolean) -> Unit
    ): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                false
            }

            if (granted) {
                Timber.i("✅ MANAGE_EXTERNAL_STORAGE permission granted")
            } else {
                Timber.w("❌ MANAGE_EXTERNAL_STORAGE permission denied")
            }
            onResult(granted)
        }
    }

    /**
     * Request storage permissions
     */
    fun requestStoragePermissions(
        launcher: ActivityResultLauncher<Array<String>>
    ) {
        val permissions = getRequiredStoragePermissions()
        Timber.d("📋 Requesting storage permissions: ${permissions.joinToString()}")
        launcher.launch(permissions)
    }

    /**
     * Request MANAGE_EXTERNAL_STORAGE permission (Android 11+)
     * This opens device settings where user must manually grant permission
     */
    fun requestManageStoragePermission(
        activity: ComponentActivity,
        launcher: ActivityResultLauncher<Intent>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                Timber.d("📋 Opening settings for MANAGE_EXTERNAL_STORAGE permission")
                launcher.launch(intent)
            } catch (e: Exception) {
                Timber.e(e, "Failed to open settings for MANAGE_EXTERNAL_STORAGE")
                // Fallback: Open general storage settings
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                launcher.launch(intent)
            }
        }
    }

    /**
     * Get user-friendly explanation for why we need storage permissions
     */
    fun getPermissionRationale(): String {
        return """
            🗄️ Storage Access Required
            
            DriftGuardAI needs storage access to:
            • 📁 Import ML models (.tflite, .onnx files)
            • 📊 Load datasets (CSV, JSON, Parquet)
            • ☁️ Access files from Google Drive/Downloads
            • 💾 Export drift reports and patches
            
            Your files remain private and secure on your device.
        """.trimIndent()
    }

    /**
     * Get instructions for manually enabling storage permissions
     */
    fun getManualPermissionInstructions(context: Context): String {
        return """
            To enable storage access manually:
            
            1. Open Settings
            2. Go to Apps → DriftGuardAI
            3. Tap Permissions
            4. Enable Storage/Files access
            
            Or grant "All files access" for unrestricted file browsing.
        """.trimIndent()
    }
}
