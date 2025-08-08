package com.yourusername.micamplifier

import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook

@AliucordPlugin
class MicAmplifier : Plugin() {

    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    override fun start() {
        // Keep the plugin stable: only patch APIs that exist on Android
        // We attempt to observe/attach to newly generated audio session IDs.
        // Important: On most devices this pertains to playback, not mic input.
        try {
            val method = AudioManager::class.java.getDeclaredMethod("generateAudioSessionId")
            patcher.patch(method, Hook { hook ->
                val sessionId = hook.result as? Int ?: return@Hook
                // Defensive guard against invalid session
                if (sessionId > 0) {
                    setupAudioEffects(sessionId)
                    logger.info("MicAmplifier: Initialized audio effects on sessionId=$sessionId")
                }
            })
        } catch (e: Throwable) {
            logger.error("MicAmplifier: Failed to hook AudioManager.generateAudioSessionId", e)
        }
    }

    override fun stop() {
        patcher.unpatchAll()
        releaseAudioEffects()
    }

    private fun setupAudioEffects(sessionId: Int) {
        releaseAudioEffects()

        try {
            // Equalizer: attempt to boost presence region (1–4kHz) modestly
            equalizer = Equalizer(0, sessionId).apply {
                enabled = true
                val bands = numberOfBands.toInt()
                for (i in 0 until bands) {
                    val centerHz = getCenterFreq(i.toShort()) / 1000 // API returns mHz
                    val boost = when {
                        centerHz in 1000..4000 -> 1200 // +12dB for presence region
                        centerHz in 500..999 -> 800    // +8dB low mids
                        else -> 400                   // +4dB gentle lift
                    }
                    setBandLevel(i.toShort(), boost.toShort())
                }
            }

            // LoudnessEnhancer: target gain in millibels
            // Use conservative defaults to avoid clipping artifacts
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                setTargetGain(1200) // +12dB; consider testing 600–2000
                enabled = true
            }
        } catch (e: Throwable) {
            logger.error("MicAmplifier: Failed to setup audio effects for sessionId=$sessionId", e)
            releaseAudioEffects()
        }
    }

    private fun releaseAudioEffects() {
        try {
            equalizer?.release()
        } catch (_: Throwable) {}
        try {
            loudnessEnhancer?.release()
        } catch (_: Throwable) {}
        equalizer = null
        loudnessEnhancer = null
    }
}
