package dev.mellow.core.data

import dev.mellow.core.data.repository.EqualizerRepository
import dev.mellow.core.model.EqualizerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerTest {

    @Test
    fun equalizerState_defaultValues() {
        val state = EqualizerState()
        assertFalse(state.enabled)
        assertEquals("Flat", state.presetName)
        assertEquals(listOf(0, 0, 0, 0, 0), state.bandLevelsDb)
        assertEquals(0, state.bassBoostStrength)
        assertEquals(1.00f, state.playbackSpeed, 0.001f)
        assertTrue(state.pitchWithSpeed)
    }

    @Test
    fun equalizerRepository_presetsCheck() {
        val rock = EqualizerRepository.PRESETS.find { it.name == "Rock" }
        assertEquals(listOf(4, 2, -1, 2, 5), rock?.bandLevelsDb)

        val bassBoost = EqualizerRepository.PRESETS.find { it.name == "Bass Boost" }
        assertEquals(listOf(6, 4, 2, 0, 0), bassBoost?.bandLevelsDb)
    }

    @Test
    fun defaultCenterFreqs_correct() {
        assertEquals(listOf(60, 230, 910, 3600, 14000), EqualizerRepository.DEFAULT_CENTER_FREQS_HZ)
    }
}
