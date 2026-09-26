package dev.mellow.core.model

data class EqualizerState(
    val enabled: Boolean = false,
    val presetName: String = "Flat",
    val bandLevelsDb: List<Int> = listOf(0, 0, 0, 0, 0),
    val bassBoostStrength: Int = 0, // 0 to 1000
    val playbackSpeed: Float = 1.00f, // 0.50f to 2.00f
    val pitchWithSpeed: Boolean = true, // true = pitch scales with speed (no time stretching)
)
