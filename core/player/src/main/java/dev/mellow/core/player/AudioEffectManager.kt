package dev.mellow.core.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import dev.mellow.core.data.repository.EqualizerRepository
import dev.mellow.core.model.EqualizerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioEffectManager(
    private val scope: CoroutineScope,
    private val equalizerRepository: EqualizerRepository,
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var player: Player? = null
    private var currentAudioSessionId: Int = 0
    private var lastState: EqualizerState? = null
    private var observeJob: Job? = null

    fun attachPlayer(player: Player) {
        this.player = player
        observeEqualizerState()
    }

    fun onAudioSessionIdChanged(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentAudioSessionId) return
        Log.d(TAG, "onAudioSessionIdChanged: $audioSessionId (was $currentAudioSessionId)")
        currentAudioSessionId = audioSessionId
        releaseEffects()
        initEffects(audioSessionId)
        lastState?.let { state ->
            scope.launch(Dispatchers.Main.immediate) {
                applyState(state)
            }
        }
    }

    private fun initEffects(sessionId: Int) {
        try {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = false
            }
            Log.d(TAG, "Equalizer initialized for session $sessionId, bands=${equalizer?.numberOfBands}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Equalizer for session $sessionId", e)
            equalizer = null
        }

        try {
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = false
            }
            Log.d(TAG, "BassBoost initialized for session $sessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize BassBoost for session $sessionId", e)
            bassBoost = null
        }
    }

    private fun observeEqualizerState() {
        observeJob?.cancel()
        observeJob = scope.launch {
            equalizerRepository.equalizerState.collectLatest { state ->
                lastState = state
                applyState(state)
            }
        }
    }

    private suspend fun applyState(state: EqualizerState) {
        val eq = equalizer
        if (eq != null) {
            try {
                eq.enabled = state.enabled
                if (state.enabled) {
                    val numBands = eq.numberOfBands.toInt()
                    val range = eq.bandLevelRange // [minMb, maxMb] e.g. [-1500, 1500]
                    val minMb = range[0].toInt()
                    val maxMb = range[1].toInt()

                    state.bandLevelsDb.forEachIndexed { bandIndex, levelDb ->
                        if (bandIndex < numBands) {
                            val targetMillibels = (levelDb * 100).coerceIn(minMb, maxMb)
                            eq.setBandLevel(bandIndex.toShort(), targetMillibels.toShort())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error applying Equalizer state", e)
            }
        }

        val bb = bassBoost
        if (bb != null) {
            try {
                val shouldEnableBb = state.enabled && state.bassBoostStrength > 0 && bb.strengthSupported
                bb.enabled = shouldEnableBb
                if (shouldEnableBb) {
                    bb.setStrength(state.bassBoostStrength.coerceIn(0, 1000).toShort())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error applying BassBoost state", e)
            }
        }

        withContext(Dispatchers.Main.immediate) {
            player?.let { p ->
                try {
                    val targetSpeed = state.playbackSpeed.coerceIn(0.50f, 2.00f)
                    val targetPitch = if (state.pitchWithSpeed) targetSpeed else 1.0f
                    val params = PlaybackParameters(targetSpeed, targetPitch)
                    if (p.playbackParameters != params) {
                        p.playbackParameters = params
                        Log.d(TAG, "Applied PlaybackParameters: speed=$targetSpeed, pitch=$targetPitch")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error applying PlaybackParameters", e)
                }
            }
        }
    }

    fun release() {
        observeJob?.cancel()
        observeJob = null
        releaseEffects()
        player = null
    }

    private fun releaseEffects() {
        try {
            equalizer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing Equalizer", e)
        }
        equalizer = null

        try {
            bassBoost?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing BassBoost", e)
        }
        bassBoost = null
    }

    companion object {
        private const val TAG = "AudioEffectManager"
    }
}
