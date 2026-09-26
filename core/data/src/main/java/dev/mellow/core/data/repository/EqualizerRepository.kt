package dev.mellow.core.data.repository

import dev.mellow.core.data.preferences.EqualizerPreferences
import dev.mellow.core.model.EqualizerState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class EqualizerPreset(
    val name: String,
    val bandLevelsDb: List<Int>,
)

@Singleton
class EqualizerRepository @Inject constructor(
    private val equalizerPreferences: EqualizerPreferences,
) {
    val equalizerState: Flow<EqualizerState> = equalizerPreferences.equalizerState

    suspend fun setEnabled(enabled: Boolean) {
        equalizerPreferences.setEnabled(enabled)
    }

    suspend fun selectPreset(presetName: String) {
        val preset = PRESETS.find { it.name.equals(presetName, ignoreCase = true) }
        if (preset != null) {
            equalizerPreferences.setPreset(preset.name, preset.bandLevelsDb)
        }
    }

    suspend fun setBandLevel(bandIndex: Int, levelDb: Int) {
        equalizerPreferences.setBandLevel(bandIndex, levelDb)
    }

    suspend fun setBandLevels(bandLevelsDb: List<Int>) {
        equalizerPreferences.setBandLevels(bandLevelsDb, "Custom")
    }

    suspend fun setBassBoost(strength: Int) {
        equalizerPreferences.setBassBoost(strength)
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        equalizerPreferences.setPlaybackSpeed(speed)
    }

    suspend fun setPitchWithSpeed(enabled: Boolean) {
        equalizerPreferences.setPitchWithSpeed(enabled)
    }

    suspend fun reset() {
        equalizerPreferences.reset()
    }

    companion object {
        val PRESETS = listOf(
            EqualizerPreset("Flat", listOf(0, 0, 0, 0, 0)),
            EqualizerPreset("Rock", listOf(4, 2, -1, 2, 5)),
            EqualizerPreset("Bass Boost", listOf(6, 4, 2, 0, 0)),
            EqualizerPreset("Pop", listOf(-1, 2, 4, 2, -1)),
            EqualizerPreset("Classical", listOf(4, 3, 0, 2, 4)),
            EqualizerPreset("Jazz", listOf(3, 2, -1, 2, 4)),
            EqualizerPreset("Heavy Metal", listOf(4, 1, 4, 2, 1)),
            EqualizerPreset("Vocal", listOf(-2, 1, 3, 3, 1)),
            EqualizerPreset("Dance", listOf(5, 3, 0, 2, 4)),
        )

        val DEFAULT_CENTER_FREQS_HZ = listOf(60, 230, 910, 3600, 14000)
    }
}
