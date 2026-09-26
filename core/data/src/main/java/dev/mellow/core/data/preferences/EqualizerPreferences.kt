package dev.mellow.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mellow.core.model.EqualizerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.equalizerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "equalizer_preferences",
)

@Singleton
class EqualizerPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.equalizerDataStore

    val equalizerState: Flow<EqualizerState> = dataStore.data.map { preferences ->
        val enabled = preferences[ENABLED] ?: false
        val presetName = preferences[PRESET_NAME] ?: "Flat"
        val bandStr = preferences[BAND_LEVELS] ?: "0,0,0,0,0"
        val bandLevels = parseBandLevels(bandStr)
        val bassBoost = preferences[BASS_BOOST] ?: 0
        val speed = preferences[PLAYBACK_SPEED] ?: 1.00f
        val pitchWithSpeed = preferences[PITCH_WITH_SPEED] ?: true

        EqualizerState(
            enabled = enabled,
            presetName = presetName,
            bandLevelsDb = bandLevels,
            bassBoostStrength = bassBoost,
            playbackSpeed = speed,
            pitchWithSpeed = pitchWithSpeed,
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ENABLED] = enabled
        }
    }

    suspend fun setPreset(presetName: String, bandLevelsDb: List<Int>) {
        dataStore.edit { preferences ->
            preferences[PRESET_NAME] = presetName
            preferences[BAND_LEVELS] = bandLevelsDb.joinToString(",")
        }
    }

    suspend fun setBandLevel(bandIndex: Int, levelDb: Int) {
        dataStore.edit { preferences ->
            val currentStr = preferences[BAND_LEVELS] ?: "0,0,0,0,0"
            val currentLevels = parseBandLevels(currentStr).toMutableList()
            while (currentLevels.size <= bandIndex) {
                currentLevels.add(0)
            }
            currentLevels[bandIndex] = levelDb
            preferences[PRESET_NAME] = "Custom"
            preferences[BAND_LEVELS] = currentLevels.joinToString(",")
        }
    }

    suspend fun setBandLevels(bandLevelsDb: List<Int>, presetName: String = "Custom") {
        dataStore.edit { preferences ->
            preferences[PRESET_NAME] = presetName
            preferences[BAND_LEVELS] = bandLevelsDb.joinToString(",")
        }
    }

    suspend fun setBassBoost(strength: Int) {
        dataStore.edit { preferences ->
            preferences[BASS_BOOST] = strength.coerceIn(0, 1000)
        }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        dataStore.edit { preferences ->
            // Round to 2 decimal places (0.01 step)
            val roundedSpeed = (Math.round(speed.coerceIn(0.50f, 2.00f) * 100f) / 100f)
            preferences[PLAYBACK_SPEED] = roundedSpeed
        }
    }

    suspend fun setPitchWithSpeed(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PITCH_WITH_SPEED] = enabled
        }
    }

    suspend fun reset() {
        dataStore.edit { preferences ->
            preferences[ENABLED] = false
            preferences[PRESET_NAME] = "Flat"
            preferences[BAND_LEVELS] = "0,0,0,0,0"
            preferences[BASS_BOOST] = 0
            preferences[PLAYBACK_SPEED] = 1.00f
            preferences[PITCH_WITH_SPEED] = true
        }
    }

    private fun parseBandLevels(str: String): List<Int> {
        return try {
            str.split(",").map { it.trim().toInt() }
        } catch (e: Exception) {
            listOf(0, 0, 0, 0, 0)
        }
    }

    companion object {
        private val ENABLED = booleanPreferencesKey("eq_enabled")
        private val PRESET_NAME = stringPreferencesKey("eq_preset_name")
        private val BAND_LEVELS = stringPreferencesKey("eq_band_levels")
        private val BASS_BOOST = intPreferencesKey("eq_bass_boost")
        private val PLAYBACK_SPEED = floatPreferencesKey("eq_playback_speed")
        private val PITCH_WITH_SPEED = booleanPreferencesKey("eq_pitch_with_speed")
    }
}
