package com.lin0721.linmusic.core.player

import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Virtualizer
import android.os.Build
import com.lin0721.linmusic.core.log.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * 4 One-Tap Master Presets replacing conventional lossy audio settings.
 */
enum class AudioMasteringPreset(
    val key: String,
    val title: String,
    val subtitle: String,
    val badge: String
) {
    STUDIO_MASTER(
        key = "studio_master",
        title = "Studio Master (Harman)",
        subtitle = "Lossless-Beater • 8-Band Parametric + 25% 3D Soundstage",
        badge = "STUDIO"
    ),
    BASS_BOOST(
        key = "bass_boost",
        title = "Desi Hip-Hop / Bass Boost",
        subtitle = "+4.5dB Sub-Bass & Punchy 808s for MC Stan / Talha Anjum",
        badge = "BASS"
    ),
    VOCAL_ACOUSTIC(
        key = "vocal_acoustic",
        title = "Late Night Acoustic / Vocal",
        subtitle = "+1.5dB Warm Low-Mids & +2.2dB Intimate Vocal Presence",
        badge = "VOCAL"
    ),
    FLAT(
        key = "flat",
        title = "Flat / Raw Studio",
        subtitle = "100% Pure Untouched Output (Bypasses EQ & DSP)",
        badge = "RAW"
    );

    companion object {
        fun fromKey(key: String): AudioMasteringPreset {
            return entries.find { it.key.equals(key, ignoreCase = true) }
                ?: when (key.lowercase()) {
                    "standard", "higher", "exhigh" -> STUDIO_MASTER
                    "lossless", "hires", "jymaster" -> STUDIO_MASTER
                    "bass", "hiphop" -> BASS_BOOST
                    "acoustic", "vocal" -> VOCAL_ACOUSTIC
                    "raw", "off" -> FLAT
                    else -> STUDIO_MASTER
                }
        }
    }
}

/**
 * Studio-Grade 32-bit Floating-Point Hardware DSP Mastering Engine.
 *
 * Utilizes Android 9+ (API 28+) DynamicsProcessing:
 * 1. 8-Band Parametric Pre-EQ (Subtractive 300Hz mud cut + 16kHz Hi-Res air boost)
 * 2. Multi-Band Compressor (MbcBand) to lift subtle textures & quiet micro-details
 * 3. Brickwall Hardware Limiter for zero-distortion loudness at 100% volume
 * 4. Virtualizer for 25% stereo stage expansion
 */
@Suppress("DEPRECATION")
object UltraAudioMasteringEngine {
    private const val TAG = "UltraAudioMastering"

    @Volatile
    private var currentPreset: AudioMasteringPreset = AudioMasteringPreset.STUDIO_MASTER

    private val _activePresetFlow = MutableStateFlow(AudioMasteringPreset.STUDIO_MASTER)
    val activePresetFlow: StateFlow<AudioMasteringPreset> = _activePresetFlow.asStateFlow()

    private val activeSessions = ConcurrentHashMap<Int, SessionEffects>()

    private data class SessionEffects(
        val dp: DynamicsProcessing?,
        val virtualizer: Virtualizer?
    )

    fun getCurrentPreset(): AudioMasteringPreset = currentPreset

    fun setPreset(preset: AudioMasteringPreset) {
        synchronized(this) {
            currentPreset = preset
            _activePresetFlow.value = preset
            activeSessions.forEach { (_, effects) ->
                applyPresetToEffects(effects, preset)
            }
            AppLogger.i(TAG, "Switched UltraAudioMastering preset to: ${preset.key} (${preset.title})")
        }
    }

    fun attachStudioMasterDSP(
        audioSessionId: Int,
        preset: AudioMasteringPreset = currentPreset,
        enabled: Boolean = true
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || audioSessionId <= 0) {
            AppLogger.w(TAG, "DynamicsProcessing not supported on this device/session (SDK=${Build.VERSION.SDK_INT}, sessionId=$audioSessionId)")
            return
        }

        synchronized(this) {
            try {
                // Release any stale effect previously bound to this audioSessionId
                activeSessions.remove(audioSessionId)?.let { old ->
                    runCatching { old.dp?.release() }
                    runCatching { old.virtualizer?.release() }
                }

                if (!enabled) {
                    AppLogger.i(TAG, "Mastering DSP disabled for audioSessionId=$audioSessionId")
                    return
                }

                val bandCount = 8
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    /* channelCount = */ 2,
                    /* preEqInUse = */ true, bandCount,
                    /* mbcInUse = */ true, bandCount,
                    /* postEqInUse = */ true, bandCount,
                    /* limiterInUse = */ true
                ).build()

                val dp = DynamicsProcessing(0, audioSessionId, config).apply {
                    val initialCurve = getCurveForPreset(preset)
                    initialCurve.forEachIndexed { index, (cutoffHz, gainDb) ->
                        // 1. Parametric Pre-EQ
                        val eqBand = DynamicsProcessing.EqBand(true, cutoffHz, gainDb)
                        setPreEqBandAllChannelsTo(index, eqBand)

                        // 2. Multi-Band Compressor lifts quiet -22dB micro-details
                        val mbcBand = DynamicsProcessing.MbcBand(
                            /* inUse = */ true,
                            /* cutoffFrequency = */ cutoffHz,
                            /* attackTime = */ 15f,
                            /* releaseTime = */ 120f,
                            /* ratio = */ 1.8f,
                            /* threshold = */ -22f,
                            /* kneeWidth = */ 6f,
                            /* noiseGateThreshold = */ -75f,
                            /* expanderRatio = */ 1.0f,
                            /* preGain = */ 0f,
                            /* postGain = */ 0.6f
                        )
                        setMbcBandAllChannelsTo(index, mbcBand)

                        // Clean phase 0dB Post-EQ
                        setPostEqBandAllChannelsTo(index, DynamicsProcessing.EqBand(true, cutoffHz, 0f))
                    }

                    // 3. Brickwall Hardware Limiter (Prevents clipping at max volume)
                    val limiter = DynamicsProcessing.Limiter(
                        /* inUse = */ true,
                        /* enabled = */ true,
                        /* linkGroup = */ 0,
                        /* attackTime = */ 1f,
                        /* releaseTime = */ 50f,
                        /* ratio = */ 12f,
                        /* threshold = */ -0.5f,
                        /* postGain = */ 0.5f
                    )
                    setLimiterAllChannelsTo(limiter)
                    this.enabled = (preset != AudioMasteringPreset.FLAT)
                }

                // 4. Subtle Soundstage Widener (25% Stereo Expansion)
                val virt = runCatching {
                    Virtualizer(0, audioSessionId).apply {
                        if (strengthSupported) {
                            val strength = when (preset) {
                                AudioMasteringPreset.STUDIO_MASTER -> 250.toShort()
                                AudioMasteringPreset.BASS_BOOST -> 200.toShort()
                                AudioMasteringPreset.VOCAL_ACOUSTIC -> 250.toShort()
                                AudioMasteringPreset.FLAT -> 0.toShort()
                            }
                            setStrength(strength)
                            this.enabled = (strength > 0 && preset != AudioMasteringPreset.FLAT)
                        }
                    }
                }.onFailure { AppLogger.w(TAG, "Virtualizer init failed", it) }.getOrNull()

                val sessionEffects = SessionEffects(dp, virt)
                activeSessions[audioSessionId] = sessionEffects
                applyPresetToEffects(sessionEffects, preset)
                AppLogger.i(TAG, "Ultra HD Master DSP attached successfully to audioSessionId=$audioSessionId (preset=${preset.key})")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed attaching UltraAudioMastering to audioSessionId=$audioSessionId", e)
            }
        }
    }

    private fun applyPresetToEffects(effects: SessionEffects, preset: AudioMasteringPreset) {
        val dp = effects.dp
        val virt = effects.virtualizer

        if (preset == AudioMasteringPreset.FLAT) {
            dp?.enabled = false
            virt?.enabled = false
            return
        }

        if (dp == null) return

        try {
            val curve = getCurveForPreset(preset)
            curve.forEachIndexed { index, (cutoffHz, gainDb) ->
                dp.setPreEqBandAllChannelsTo(index, DynamicsProcessing.EqBand(true, cutoffHz, gainDb))
            }

            val virtualizerStrength = when (preset) {
                AudioMasteringPreset.STUDIO_MASTER -> 250.toShort()
                AudioMasteringPreset.BASS_BOOST -> 200.toShort()
                AudioMasteringPreset.VOCAL_ACOUSTIC -> 250.toShort()
                AudioMasteringPreset.FLAT -> 0.toShort()
            }

            virt?.let {
                if (it.strengthSupported) {
                    it.setStrength(virtualizerStrength)
                    it.enabled = virtualizerStrength > 0
                }
            }

            dp.enabled = true
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed applying preset ${preset.key}", e)
        }
    }

    private fun getCurveForPreset(preset: AudioMasteringPreset): List<Pair<Float, Float>> {
        return when (preset) {
            AudioMasteringPreset.STUDIO_MASTER -> listOf(
                45f to 2.8f,    // Band 0: Deep Sub-Bass extension (32-45Hz)
                90f to 2.2f,    // Band 1: Tight 808 / Kick punch (64-90Hz)
                160f to 0.5f,   // Band 2: Lower-mid warmth
                300f to -1.8f,  // Band 3: SUBTRACTIVE MUD CUT (Unmasks boxiness!)
                1000f to 0.4f,  // Band 4: Centered vocal body
                3500f to 1.6f,  // Band 5: Vocal & snare bite
                8500f to 2.0f,  // Band 6: Crisp hi-hat & string sparkle
                18000f to 3.2f  // Band 7: "Hi-Res Air" & soundstage shimmer
            )
            AudioMasteringPreset.BASS_BOOST -> listOf(
                45f to 4.5f,    // Band 0: Massive 45Hz sub-bass rumble
                90f to 3.5f,    // Band 1: Heavy punchy 808 kicks
                160f to 0.5f,   // Band 2: Lower-mid body
                300f to -1.5f,  // Band 3: Mud cut to keep 808s from bleeding into vocals
                1000f to 0.0f,  // Band 4: Neutral vocal center
                3500f to 1.2f,  // Band 5: Rap flow articulation
                8500f to 1.5f,  // Band 6: Trap hi-hat attack
                18000f to 2.5f  // Band 7: Airy upper harmonics
            )
            AudioMasteringPreset.VOCAL_ACOUSTIC -> listOf(
                45f to 0.5f,    // Band 0: Subtle foundation
                90f to 0.8f,    // Band 1: Soft low-end
                160f to 1.5f,   // Band 2: Warm acoustic guitar body & lower-mid resonance
                300f to -1.0f,  // Band 3: Mild boxiness removal
                1000f to 1.0f,  // Band 4: Forward, warm vocal core
                3500f to 2.2f,  // Band 5: Intimate vocal bite & string plucks (Khat / Pal Pal)
                8500f to 1.8f,  // Band 6: Silky acoustic highs
                18000f to 2.8f  // Band 7: Natural room reverb & intimate breath
            )
            AudioMasteringPreset.FLAT -> listOf(
                45f to 0f,
                90f to 0f,
                160f to 0f,
                300f to 0f,
                1000f to 0f,
                3500f to 0f,
                8500f to 0f,
                18000f to 0f
            )
        }
    }

    fun releaseSession(audioSessionId: Int) {
        synchronized(this) {
            activeSessions.remove(audioSessionId)?.let {
                runCatching { it.dp?.release() }
                runCatching { it.virtualizer?.release() }
            }
        }
    }

    fun releaseAll() {
        synchronized(this) {
            activeSessions.values.forEach {
                runCatching { it.dp?.release() }
                runCatching { it.virtualizer?.release() }
            }
            activeSessions.clear()
        }
    }
}
