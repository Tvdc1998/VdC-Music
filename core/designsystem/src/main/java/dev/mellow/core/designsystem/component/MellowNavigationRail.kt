package dev.mellow.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mellow.core.designsystem.theme.MellowPalette
import dev.mellow.core.designsystem.theme.MellowTheme

@Composable
fun MellowNavigationRail(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    activeAccentColor: Color? = null,
) {
    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .width(80.dp)
                .background(MellowTheme.colors.surface)
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            MellowNavDestination.entries.forEach { dest ->
                val isSelected = dest.route == selectedRoute
                val targetTint = if (isSelected) {
                    activeAccentColor ?: MellowTheme.colors.foreground
                } else {
                    MellowTheme.colors.muted
                }
                val animatedTint by animateColorAsState(
                    targetValue = targetTint,
                    animationSpec = tween(400),
                    label = "rail_tint",
                )

                val pillColor = if (isSelected) {
                    activeAccentColor?.copy(alpha = 0.20f) ?: MellowPalette.Stone800
                } else {
                    Color.Transparent
                }
                val animatedPill by animateColorAsState(
                    targetValue = pillColor,
                    animationSpec = tween(400),
                    label = "rail_pill",
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(animatedPill)
                        .clickable { onNavigate(dest.route) }
                        .padding(vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = dest.label,
                        tint = animatedTint,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = dest.label,
                        fontSize = 10.sp,
                        color = animatedTint,
                        letterSpacing = 0.02.sp,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(1.dp)
                .background(MellowTheme.colors.border),
        )
    }
}
