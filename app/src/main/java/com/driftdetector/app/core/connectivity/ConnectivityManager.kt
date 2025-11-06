package com.driftdetector.app.core.connectivity

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
 * Monitors network connectivity for real-time sync
 * Handles offline/online transitions gracefully
 */
class NetworkConnectivityManager(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Unknown)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkType = MutableStateFlow<NetworkType>(NetworkType.NONE)
    val networkType: StateFlow<NetworkType> = _networkType.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            Timber.i("🟢 Network available")
            updateNetworkState(network)
            _networkState.value = NetworkState.Available
            _isOnline.value = true
        }

        override fun onLost(network: Network) {
            Timber.w("⚫ Network lost")
            _networkState.value = NetworkState.Lost
            _isOnline.value = false
            _networkType.value = NetworkType.NONE
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            updateNetworkState(network)

            val hasInternet = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )

            val isValidated = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )

            if (hasInternet && isValidated) {
                _networkState.value = NetworkState.Available
                _isOnline.value = true
            }
        }

        override fun onUnavailable() {
            Timber.w("⚫ Network unavailable")
            _networkState.value = NetworkState.Unavailable
            _isOnline.value = false
            _networkType.value = NetworkType.NONE
        }
    }

    /**
     * Start monitoring network connectivity
     */
    fun startMonitoring() {
        try {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

            // Get initial state
            updateCurrentNetworkState()

            Timber.i("✅ Network monitoring started")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start network monitoring")
        }
    }

    /**
     * Stop monitoring network connectivity
     */
    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Timber.i("Network monitoring stopped")
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop network monitoring")
        }
    }

    /**
     * Check if currently online
     */
    fun isCurrentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Get current network type
     */
    fun getCurrentNetworkType(): NetworkType {
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkType.NONE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }
    }

    /**
     * Check if on metered network (cellular)
     */
    fun isMeteredConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /**
     * Get network bandwidth estimate
     */
    fun getNetworkBandwidth(): NetworkBandwidth {
        val network = connectivityManager.activeNetwork ?: return NetworkBandwidth.UNKNOWN
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkBandwidth.UNKNOWN

        val downstreamBandwidth = capabilities.linkDownstreamBandwidthKbps
        val upstreamBandwidth = capabilities.linkUpstreamBandwidthKbps

        return when {
            downstreamBandwidth >= 50000 -> NetworkBandwidth.HIGH // 50+ Mbps
            downstreamBandwidth >= 10000 -> NetworkBandwidth.MEDIUM // 10+ Mbps
            downstreamBandwidth >= 1000 -> NetworkBandwidth.LOW // 1+ Mbps
            else -> NetworkBandwidth.POOR
        }
    }

    private fun updateNetworkState(network: Network) {
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        if (capabilities != null) {
            val type = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                    NetworkType.WIFI

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                    NetworkType.CELLULAR

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                    NetworkType.ETHERNET

                else -> NetworkType.OTHER
            }

            _networkType.value = type
            Timber.d("Network type: $type")
        }
    }

    private fun updateCurrentNetworkState() {
        val isOnline = isCurrentlyOnline()
        _isOnline.value = isOnline
        _networkState.value = if (isOnline) NetworkState.Available else NetworkState.Unavailable
        _networkType.value = getCurrentNetworkType()
    }
}

/**
 * Network state
 */
sealed class NetworkState {
    object Unknown : NetworkState()
    object Available : NetworkState()
    object Unavailable : NetworkState()
    object Lost : NetworkState()
}

/**
 * Network type
 */
enum class NetworkType {
    NONE,
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER
}

/**
 * Network bandwidth classification
 */
enum class NetworkBandwidth {
    UNKNOWN,
    POOR,
    LOW,
    MEDIUM,
    HIGH
}
