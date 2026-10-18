package com.lin0721.linmusic.core.player

import android.content.Context
import com.lin0721.linmusic.core.preferences.SettingsPreferences
import kotlinx.coroutines.CoroutineScope

/**
 * Controller previously responsible for auto-resuming playback after external audio stops.
 * Permanently disabled to ensure playback stays stopped when users browse YouTube,
 * scroll Instagram reels, receive calls, or use the camera until explicitly resumed.
 */
class ExternalInterruptionResumeController(
    @Suppress("unused") private val context: Context,
    @Suppress("unused") private val settingsPreferences: SettingsPreferences
) {
    fun start(scope: CoroutineScope, onResumeRequested: () -> Unit) {
        // Explicitly disabled per user preference: no auto-resume when external audio stops.
    }

    fun onAudioFocusLoss() {}
    fun onUserOrSystemPause() {}
    fun onPlaybackStarted() {}
    fun release() {}
}
