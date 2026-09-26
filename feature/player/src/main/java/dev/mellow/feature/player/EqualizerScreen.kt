package dev.mellow.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mellow.core.data.repository.EqualizerRepository
import dev.mellow.core.designsystem.icon.PhosphorIcons
import dev.mellow.core.designsystem.theme.MellowPalette
import dev.mellow.core.designsystem.theme.MellowShapes
import dev.mellow.core.designsystem.theme.MellowSpacing
import dev.mellow.core.designsystem.theme.MellowTheme
import dev.mellow.core.model.EqualizerState
import java.util.Locale

@Composable
fun EqualizerScreen(
    modifier: Modifier = Modifier,
    state: EqualizerState = EqualizerState(),
    onBack: () -> Unit = {},
    onToggleEnabled: (Boolean) -> Unit = {},
    onSelectPreset: (String) -> Unit = {},
    onBandLevelChange: (Int, Int) -> Unit = { _, _ -> },
    onBassBoostChange: (Int) -> Unit = {},
    onPlaybackSpeedChange: (Float) -> Unit = {},
    onPitchWithSpeedChange: (Boolean) -> Unit = {},
    onReset: () -> Unit = {},
) {
    val bandCenterFreqs = EqualizerRepository.DEFAULT_CENTER_FREQS_HZ

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MellowTheme.colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MellowSpacing.Sp2, vertical = MellowSpacing.Sp3),
        ) {
            IconButton(onClick = onBack) {
                Icon(PhosphorIcons.ArrowLeft, "Back", tint = MellowTheme.colors.foreground)
            }
            Text(
                "Audio & Equalizer",
                style = MaterialTheme.typography.headlineLarge,
                color = MellowTheme.colors.foreground,
            )
            TextButton(onClick = onReset) {
                Text(
                    "Reset",
                    style = MaterialTheme.typography.labelLarge,
                    color = MellowTheme.colors.accentStrong,
                )
            }
        }

        // Section: Equalizer Master Switch
        SectionHeader("EQUALIZER")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleEnabled(!state.enabled) }
                .padding(horizontal = MellowSpacing.Sp4, vertical = MellowSpacing.Sp3),
        ) {
            Icon(
                PhosphorIcons.Sliders,
                contentDescription = null,
                tint = if (state.enabled) MellowTheme.colors.accentStrong else MellowTheme.colors.muted,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = MellowSpacing.Sp3),
            ) {
                Text(
                    "Equalizer Effects",
                    style = MaterialTheme.typography.titleMedium,
                    color = MellowTheme.colors.foreground,
                )
                Text(
                    if (state.enabled) "Active (${state.presetName})" else "Disabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MellowTheme.colors.muted,
                )
            }
            Switch(
                checked = state.enabled,
                onCheckedChange = onToggleEnabled,
            )
        }

        HorizontalDivider(color = MellowTheme.colors.border)

        // Section: Presets
        SectionHeader("PRESETS")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = MellowSpacing.Sp4, vertical = MellowSpacing.Sp2),
            horizontalArrangement = Arrangement.spacedBy(MellowSpacing.Sp2),
        ) {
            val presets = EqualizerRepository.PRESETS.map { it.name } + listOf("Custom")
            presets.forEach { preset ->
                val isSelected = state.presetName.equals(preset, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (!preset.equals("Custom", ignoreCase = true)) {
                            onSelectPreset(preset)
                        }
                    },
                    label = {
                        Text(
                            preset,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MellowTheme.colors.surface,
                        labelColor = MellowTheme.colors.foreground,
                        selectedContainerColor = MellowTheme.colors.accentStrong,
                        selectedLabelColor = MellowPalette.Stone900,
                    ),
                    shape = MellowShapes.Full,
                    enabled = state.enabled || isSelected,
                )
            }
        }

        Spacer(Modifier.height(MellowSpacing.Sp2))

        // Section: Frequency Bands
        SectionHeader("FREQUENCY BANDS")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MellowSpacing.Sp4, vertical = MellowSpacing.Sp3)
                .clip(MellowShapes.Medium)
                .background(MellowTheme.colors.surface)
                .padding(vertical = MellowSpacing.Sp4, horizontal = MellowSpacing.Sp2),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                bandCenterFreqs.forEachIndexed { bandIndex, freqHz ->
                    val dbValue = state.bandLevelsDb.getOrElse(bandIndex) { 0 }
                    FrequencyBandColumn(
                        freqHz = freqHz,
                        gainDb = dbValue,
                        enabled = state.enabled,
                        onGainChange = { newDb ->
                            onBandLevelChange(bandIndex, newDb)
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(MellowSpacing.Sp2))

        // Section: Bass Boost
        SectionHeader("BASS BOOST")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MellowSpacing.Sp4, vertical = MellowSpacing.Sp2)
                .clip(MellowShapes.Medium)
                .background(MellowTheme.colors.surface)
                .padding(MellowSpacing.Sp4),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Bass Boost Strength",
                    style = MaterialTheme.typography.titleMedium,
                    color = MellowTheme.colors.foreground,
                )
                val pct = ((state.bassBoostStrength / 1000f) * 100).toInt()
                Text(
                    "$pct%",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (state.enabled && state.bassBoostStrength > 0) MellowTheme.colors.accentStrong else MellowTheme.colors.muted,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(MellowSpacing.Sp2))
            Slider(
                value = state.bassBoostStrength.toFloat(),
                onValueChange = { onBassBoostChange(it.toInt()) },
                valueRange = 0f..1000f,
                enabled = state.enabled,
                colors = SliderDefaults.colors(
                    thumbColor = MellowTheme.colors.accentStrong,
                    activeTrackColor = MellowTheme.colors.accentStrong,
                    inactiveTrackColor = MellowPalette.Stone700,
                ),
            )
        }

        HorizontalDivider(
            color = MellowTheme.colors.border,
            modifier = Modifier.padding(top = MellowSpacing.Sp4),
        )

        // Section: Playback Speed Acceleration (Zonder Tijdrekken)
        SectionHeader("PLAYBACK SPEED & PITCH")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MellowSpacing.Sp4, vertical = MellowSpacing.Sp2)
                .clip(MellowShapes.Medium)
                .background(MellowTheme.colors.surface)
                .padding(MellowSpacing.Sp4),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Speed Multiplier",
                        style = MaterialTheme.typography.titleMedium,
                        color = MellowTheme.colors.foreground,
                    )
                    Text(
                        "0.01 step precision",
                        style = MaterialTheme.typography.bodySmall,
                        color = MellowTheme.colors.muted,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(MellowShapes.Full)
                        .background(MellowTheme.colors.accentStrong)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        String.format(Locale.US, "%.2fx", state.playbackSpeed),
                        style = MaterialTheme.typography.titleMedium,
                        color = MellowPalette.Stone900,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(MellowSpacing.Sp4))

            // Speed Slider with 0.01 step resolution
            var localSliderSpeed by remember(state.playbackSpeed) {
                mutableFloatStateOf(state.playbackSpeed)
            }

            Slider(
                value = localSliderSpeed.coerceIn(0.50f, 2.00f),
                onValueChange = { raw ->
                    val rounded = (Math.round(raw * 100f) / 100f)
                    localSliderSpeed = rounded
                    onPlaybackSpeedChange(rounded)
                },
                valueRange = 0.50f..2.00f,
                colors = SliderDefaults.colors(
                    thumbColor = MellowTheme.colors.accentStrong,
                    activeTrackColor = MellowTheme.colors.accentStrong,
                    inactiveTrackColor = MellowPalette.Stone700,
                ),
            )

            // Fast speed preset buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MellowSpacing.Sp2),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val speedPresets = listOf(0.75f, 1.00f, 1.25f, 1.50f, 2.00f)
                speedPresets.forEach { speed ->
                    val isSelected = Math.abs(state.playbackSpeed - speed) < 0.005f
                    Box(
                        modifier = Modifier
                            .clip(MellowShapes.Small)
                            .background(
                                if (isSelected) MellowTheme.colors.accentStrong else MellowTheme.colors.background,
                            )
                            .clickable { onPlaybackSpeedChange(speed) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            String.format(Locale.US, "%.2fx", speed),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MellowPalette.Stone900 else MellowTheme.colors.foreground,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(Modifier.height(MellowSpacing.Sp4))
            HorizontalDivider(color = MellowTheme.colors.border)
            Spacer(Modifier.height(MellowSpacing.Sp3))

            // Pitch mode toggle switch ("Zonder tijdrekken")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPitchWithSpeedChange(!state.pitchWithSpeed) },
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Pitch scales with speed (no time-stretching)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MellowTheme.colors.foreground,
                    )
                    Text(
                        if (state.pitchWithSpeed)
                            "Natural tape/vinyl rate scaling (no phase vocoder artifacts)"
                        else
                            "Time-stretching enabled (pitch locked)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MellowTheme.colors.muted,
                    )
                }
                Switch(
                    checked = state.pitchWithSpeed,
                    onCheckedChange = onPitchWithSpeedChange,
                )
            }
        }

        Spacer(Modifier.height(MellowSpacing.Sp16))
    }
}

@Composable
private fun FrequencyBandColumn(
    freqHz: Int,
    gainDb: Int,
    enabled: Boolean,
    onGainChange: (Int) -> Unit,
) {
    val freqLabel = if (freqHz >= 1000) {
        String.format(Locale.US, "%.1f k", freqHz / 1000f)
    } else {
        "$freqHz Hz"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(52.dp),
    ) {
        val sign = if (gainDb > 0) "+" else ""
        Text(
            "$sign$gainDb dB",
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled && gainDb != 0) MellowTheme.colors.accentStrong else MellowTheme.colors.muted,
            fontWeight = if (gainDb != 0) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp,
        )

        Spacer(Modifier.height(MellowSpacing.Sp2))

        // Band Slider (-15 dB to +15 dB)
        var localGain by remember(gainDb) { mutableFloatStateOf(gainDb.toFloat()) }

        Slider(
            value = localGain,
            onValueChange = { raw ->
                val rounded = Math.round(raw)
                localGain = rounded.toFloat()
                onGainChange(rounded)
            },
            valueRange = -15f..15f,
            enabled = enabled,
            modifier = Modifier.height(140.dp),
            colors = SliderDefaults.colors(
                thumbColor = if (enabled) MellowTheme.colors.accentStrong else MellowTheme.colors.muted,
                activeTrackColor = if (enabled) MellowTheme.colors.accentStrong else MellowTheme.colors.muted,
                inactiveTrackColor = MellowPalette.Stone700,
            ),
        )

        Spacer(Modifier.height(MellowSpacing.Sp2))

        Text(
            freqLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MellowTheme.colors.foreground,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MellowTheme.colors.muted,
        modifier = Modifier.padding(
            start = MellowSpacing.Sp4,
            top = MellowSpacing.Sp5,
            bottom = MellowSpacing.Sp2,
        ),
    )
}
