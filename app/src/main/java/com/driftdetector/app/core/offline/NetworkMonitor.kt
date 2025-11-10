package com.driftdetector.app.core.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Monitors network connectivity and notifies observers
 */
class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.Unknown)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.d("🌐 Network available")
            updateNetworkStatus()
        }

        override fun onLost(network: Network) {
            Timber.d("📴 Network lost")
            updateNetworkStatus()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            Timber.d("🔄 Network capabilities changed")
            updateNetworkStatus()
        }
    }

    init {
        registerNetworkCallback()
        updateNetworkStatus()
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun updateNetworkStatus() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        _networkStatus.value = when {
            capabilities == null -> NetworkStatus.Disconnected
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkStatus.WiFi
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkStatus.Cellular
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkStatus.Ethernet
            else -> NetworkStatus.Other
        }

        _isOnline.value = _networkStatus.value != NetworkStatus.Disconnected

        Timber.d("📶 Network status updated: ${_networkStatus.value}, Online: ${_isOnline.value}")
    }

    fun isCurrentlyOnline(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun hasWiFi(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    fun hasCellular(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    }

    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Timber.d("✅ Network callback unregistered")
        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister network callback")
        }
    }
}

/**
 * Network connectivity status
 */
sealed class NetworkStatus {
    object Unknown : NetworkStatus()
    object Disconnected : NetworkStatus()
    object WiFi : NetworkStatus()
    object Cellular : NetworkStatus()
    object Ethernet : NetworkStatus()
    object Other : NetworkStatus()

    val isConnected: Boolean get() = this !is Disconnected && this !is Unknown

    override fun toString(): String = when (this) {
        is Unknown -> "Unknown"
        is Disconnected -> "Disconnected"
        is WiFi -> "WiFi"
        is Cellular -> "Cellular"
        is Ethernet -> "Ethernet"
        is Other -> "Other"
    }
}
