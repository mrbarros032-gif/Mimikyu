package com.lin0721.linmusic.core.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.widget.Toast
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.preferences.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "PlaybackNetworkGuard"

// ： Wi-Fi ，“Wi-Fi Only Playback”
class PlaybackNetworkGuard(
    private val context: Context,
    private val scope: CoroutineScope,
    private val settingsPreferences: SettingsPreferences,
    private val isPlaying: () -> Boolean,
    private val onPauseRequested: () -> Unit
) {

    private var lastWasWifi = false

    // （INTERNET+VALIDATED），PlayRetry
    private var recoveryCallback: (() -> Unit)? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, capabilities)
            val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isMobile = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

            if (lastWasWifi && isMobile && !isWifi) {
                scope.launch(Dispatchers.Main) {
                    val alertEnabled = settingsPreferences.mobileAlert.first()
                    val wifiOnly = settingsPreferences.wifiOnlyPlay.first()

                    if (wifiOnly) {
                        onPauseRequested()
                        Toast.makeText(context, "Playback paused ('Stream over Wi-Fi only' enabled)", Toast.LENGTH_LONG).show()
                    } else if (alertEnabled && isPlaying()) {
                        Toast.makeText(context, "Switched to mobile data playback", Toast.LENGTH_LONG).show()
                    }
                }
            }
            if (isWifi) {
                lastWasWifi = true
            } else if (isMobile) {
                lastWasWifi = false
            }

            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            ) {
                recoveryCallback?.let { callback ->
                    recoveryCallback = null
                    scope.launch(Dispatchers.Main) { callback() }
                }
            }
        }
    }

    // ，Clear
    fun awaitNetworkRecovery(onRecovered: () -> Unit) {
        recoveryCallback = onRecovered
    }

    // of ， of Play
    fun cancelRecoveryWait() {
        recoveryCallback = null
    }


    fun register() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm?.registerNetworkCallback(request, networkCallback)
    }

    fun unregister() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        kotlin.runCatching { cm?.unregisterNetworkCallback(networkCallback) }
            .onFailure { AppLogger.w(TAG, "Failed to unregister network callback", it) }
    }

    // “Wi-Fi Only Playback”
    suspend fun blockPlaybackOnMobile(): Boolean {
        val wifiOnly = settingsPreferences.wifiOnlyPlay.first()
        if (wifiOnly && isMobileNetwork()) {
            Toast.makeText(context, "Currently on mobile data. 'Stream over Wi-Fi only' is enabled.", Toast.LENGTH_SHORT).show()
            return true
        }
        return false
    }

    private fun isMobileNetwork(): Boolean {
        return kotlin.runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        }.onFailure { AppLogger.w(TAG, "Cellular network detection error", it) }.getOrDefault(false)
    }
}
