package com.lin0721.linmusic.core.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.preferences.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "ExternalInterruptionResumeController"
// ：
private const val DEBOUNCE_DELAY_MS = 1500L
// ： 10 ，
private const val MAX_INTERRUPTION_WINDOW_MS = 10 * 60 * 1000L

// /Play
class ExternalInterruptionResumeController(
    private val context: Context,
    private val settingsPreferences: SettingsPreferences
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var scope: CoroutineScope? = null
    private var onResumeRequested: (() -> Unit)? = null

    @Volatile
    private var isEnabled: Boolean = true

    @Volatile
    private var isAwaitingResume: Boolean = false

    private var interruptionTimestamp: Long = 0L
    private var debounceJob: Job? = null
    private var pollingJob: Job? = null
    private var isRegistered: Boolean = false

    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
            super.onPlaybackConfigChanged(configs)
            checkAndScheduleResume()
        }
    }

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                AppLogger.i(TAG, "Headphones unplugged, cancelling interrupt resume")
                cancelWaiting()
            }
        }
    }

    fun start(scope: CoroutineScope, onResumeRequested: () -> Unit) {
        this.scope = scope
        this.onResumeRequested = onResumeRequested

        scope.launch {
            settingsPreferences.resumeAfterExternalInterruption.collect { enabled ->
                isEnabled = enabled
                if (enabled) {
                    registerListeners()
                } else {
                    cancelWaiting()
                    unregisterListeners()
                }
            }
        }
    }

    fun onAudioFocusLoss() {
        if (!isEnabled) return
        AppLogger.i(TAG, "Audio focus lost, waiting for external audio to pause")
        isAwaitingResume = true
        interruptionTimestamp = SystemClock.elapsedRealtime()
        debounceJob?.cancel()
        debounceJob = null
        startPollingFallback()
    }

    fun onUserOrSystemPause() {
        if (isAwaitingResume) {
            AppLogger.i(TAG, "User/system paused without focus loss, resetting resume wait")
            cancelWaiting()
        }
    }

    fun onPlaybackStarted() {
        if (isAwaitingResume) {
            cancelWaiting()
        }
    }

    fun release() {
        cancelWaiting()
        unregisterListeners()
        scope = null
        onResumeRequested = null
    }

    private fun cancelWaiting() {
        isAwaitingResume = false
        debounceJob?.cancel()
        debounceJob = null
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun isExternalAudioActive(): Boolean {
        return audioManager.isMusicActive || audioManager.mode != AudioManager.MODE_NORMAL
    }

    private fun checkAndScheduleResume() {
        if (!isAwaitingResume) return

        val elapsed = SystemClock.elapsedRealtime() - interruptionTimestamp
        if (elapsed > MAX_INTERRUPTION_WINDOW_MS) {
            AppLogger.i(TAG, "External interrupt exceeded max wait window (${elapsed}ms), discarding auto-resume")
            cancelWaiting()
            return
        }

        if (isExternalAudioActive()) {
            debounceJob?.cancel()
            debounceJob = null
        } else {
            if (debounceJob?.isActive == true) {
                return
            }
            debounceJob = scope?.launch {
                delay(DEBOUNCE_DELAY_MS)
                if (isAwaitingResume && !isExternalAudioActive()) {
                    val currentElapsed = SystemClock.elapsedRealtime() - interruptionTimestamp
                    if (currentElapsed <= MAX_INTERRUPTION_WINDOW_MS) {
                        AppLogger.i(TAG, "External audio stopped, resuming playback")
                        isAwaitingResume = false
                        pollingJob?.cancel()
                        pollingJob = null
                        onResumeRequested?.invoke()
                    } else {
                        cancelWaiting()
                    }
                }
            }
        }
    }

    private fun startPollingFallback() {
        pollingJob?.cancel()
        pollingJob = scope?.launch {
            while (isAwaitingResume) {
                delay(1000L)
                if (isAwaitingResume) {
                    checkAndScheduleResume()
                }
            }
        }
    }

    private fun registerListeners() {
        if (isRegistered) return
        try {
            audioManager.registerAudioPlaybackCallback(playbackCallback, null)
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            ContextCompat.registerReceiver(
                context,
                becomingNoisyReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            isRegistered = true
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to register audio listener", e)
        }
    }

    private fun unregisterListeners() {
        if (!isRegistered) return
        try {
            audioManager.unregisterAudioPlaybackCallback(playbackCallback)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to unregister AudioPlaybackCallback", e)
        }
        try {
            context.unregisterReceiver(becomingNoisyReceiver)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to unregister becomingNoisyReceiver", e)
        }
        isRegistered = false
    }
}
