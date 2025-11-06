package com.driftdetector.app.core.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.*
import org.json.JSONObject

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

/**
 * Manages user authentication and JWT tokens
 * Provides secure session management for real-time monitoring access
 */
class AuthManager(
    private val context: Context,
    private val gson: Gson = Gson()
) {

    private val dataStore = context.authDataStore

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
    }

    /**
     * Check if user is authenticated
     */
    suspend fun isAuthenticated(): Boolean {
        val token = getAccessToken()
        return token != null && !isTokenExpired(token)
    }

    /**
     * Get current access token
     */
    suspend fun getAccessToken(): String? {
        return dataStore.data.map { preferences ->
            preferences[KEY_ACCESS_TOKEN]
        }.first()
    }

    /**
     * Get current refresh token
     */
    suspend fun getRefreshToken(): String? {
        return dataStore.data.map { preferences ->
            preferences[KEY_REFRESH_TOKEN]
        }.first()
    }

    /**
     * Get current user session
     */
    suspend fun getCurrentUser(): UserSession? {
        return dataStore.data.map { preferences ->
            val userId = preferences[KEY_USER_ID]
            val email = preferences[KEY_USER_EMAIL]
            val role = preferences[KEY_USER_ROLE]
            val token = preferences[KEY_ACCESS_TOKEN]

            if (userId != null && email != null && token != null) {
                UserSession(
                    userId = userId,
                    email = email,
                    role = role ?: "user",
                    token = token
                )
            } else null
        }.first()
    }

    /**
     * Flow of authentication state
     */
    val authStateFlow: Flow<AuthState> = dataStore.data.map { preferences ->
        val token = preferences[KEY_ACCESS_TOKEN]

        if (token != null && !isTokenExpired(token)) {
            AuthState.Authenticated(
                userId = preferences[KEY_USER_ID] ?: "",
                email = preferences[KEY_USER_EMAIL] ?: ""
            )
        } else {
            AuthState.Unauthenticated
        }
    }

    /**
     * Login with credentials
     */
    suspend fun login(email: String, password: String): Result<UserSession> {
        return try {
            // In production, this would make an API call to authenticate
            // For now, we'll create a mock session

            // TODO: Replace with actual API call
            // val response = apiService.login(LoginRequest(email, password))

            // Mock authentication for demonstration
            val mockToken = generateMockJWT(email)
            val session = UserSession(
                userId = UUID.randomUUID().toString(),
                email = email,
                role = "data_scientist",
                token = mockToken
            )

            saveSession(session, mockToken)

            Timber.i("✅ Login successful for $email")
            Result.success(session)

        } catch (e: Exception) {
            Timber.e(e, "Login failed")
            Result.failure(e)
        }
    }

    /**
     * Logout current user
     */
    suspend fun logout() {
        try {
            dataStore.edit { preferences ->
                preferences.clear()
            }
            Timber.i("✅ Logout successful")
        } catch (e: Exception) {
            Timber.e(e, "Logout failed")
        }
    }

    /**
     * Refresh access token
     */
    suspend fun refreshToken(): Result<String> {
        return try {
            val refreshToken = getRefreshToken() ?: return Result.failure(
                Exception("No refresh token available")
            )

            // TODO: Replace with actual API call
            // val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))

            // Mock token refresh
            val newToken = generateMockJWT(getCurrentUser()?.email ?: "user@example.com")

            dataStore.edit { preferences ->
                preferences[KEY_ACCESS_TOKEN] = newToken
            }

            Timber.i("✅ Token refreshed")
            Result.success(newToken)

        } catch (e: Exception) {
            Timber.e(e, "Token refresh failed")
            Result.failure(e)
        }
    }

    /**
     * Save authentication session
     */
    private suspend fun saveSession(session: UserSession, refreshToken: String? = null) {
        dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = session.token
            preferences[KEY_USER_ID] = session.userId
            preferences[KEY_USER_EMAIL] = session.email
            preferences[KEY_USER_ROLE] = session.role

            refreshToken?.let {
                preferences[KEY_REFRESH_TOKEN] = it
            }
        }
    }

    /**
     * Check if JWT token is expired
     */
    private fun isTokenExpired(token: String): Boolean {
        return try {
            val payload = decodeJWTPayload(token)
            val exp = payload.optLong("exp", 0)

            if (exp == 0L) {
                // If no expiry, assume valid
                false
            } else {
                // Check if expired (with 5 minute buffer)
                val now = System.currentTimeMillis() / 1000 // Convert to seconds
                val bufferTime = 5 * 60 // 5 minutes in seconds
                (exp - now) < bufferTime
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking token expiry")
            true // Assume expired if we can't parse
        }
    }

    /**
     * Decode JWT payload
     */
    private fun decodeJWTPayload(token: String): JSONObject {
        val parts = token.split(".")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid JWT token format")
        }

        val payload = parts[1]
        val decodedBytes = Base64.getUrlDecoder().decode(payload)
        val decodedString = String(decodedBytes, Charsets.UTF_8)

        return JSONObject(decodedString)
    }

    /**
     * Generate a mock JWT token for demonstration
     * In production, this would come from the backend
     */
    private fun generateMockJWT(email: String): String {
        // This is a mock token - in production, the backend generates this
        // Format: header.payload.signature
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"alg":"HS256","typ":"JWT"}""".toByteArray()
        )

        val expiryTime = System.currentTimeMillis() / 1000 + (24 * 60 * 60) // 24 hours in seconds
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"sub":"${UUID.randomUUID()}","email":"$email","exp":$expiryTime,"role":"data_scientist"}""".toByteArray()
        )

        val signature = Base64.getUrlEncoder().withoutPadding().encodeToString(
            "mock_signature".toByteArray()
        )

        return "$header.$payload.$signature"
    }

    /**
     * Get user permissions
     */
    suspend fun getUserPermissions(): Set<Permission> {
        val user = getCurrentUser() ?: return emptySet()

        return when (user.role) {
            "admin" -> Permission.values().toSet()
            "data_scientist" -> setOf(
                Permission.VIEW_MODELS,
                Permission.MONITOR_DRIFT,
                Permission.CREATE_PATCHES,
                Permission.DEPLOY_PATCHES,
                Permission.VIEW_TELEMETRY
            )

            "viewer" -> setOf(
                Permission.VIEW_MODELS,
                Permission.MONITOR_DRIFT,
                Permission.VIEW_TELEMETRY
            )

            else -> emptySet()
        }
    }

    /**
     * Check if user has permission
     */
    suspend fun hasPermission(permission: Permission): Boolean {
        return getUserPermissions().contains(permission)
    }
}

/**
 * Authentication state
 */
sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val userId: String, val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * User session data
 */
data class UserSession(
    val userId: String,
    val email: String,
    val role: String,
    val token: String
)

/**
 * User permissions for RBAC
 */
enum class Permission {
    VIEW_MODELS,
    CREATE_MODELS,
    EDIT_MODELS,
    DELETE_MODELS,
    MONITOR_DRIFT,
    CREATE_PATCHES,
    DEPLOY_PATCHES,
    VIEW_TELEMETRY,
    MANAGE_USERS,
    ADMIN_SETTINGS
}
