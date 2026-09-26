package dev.mellow.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.mellow.core.designsystem.theme.MellowShapes
import dev.mellow.core.designsystem.theme.MellowSpacing
import dev.mellow.core.designsystem.theme.MellowTheme

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MellowTheme.colors.surfaceElevated),
    ) {
        PixelBlastPlaceholder(
            modifier = Modifier.fillMaxSize(),
            color = MellowTheme.colors.foreground.copy(alpha = 0.15f),
        )
    }
}

@Composable
fun AlbumGridSkeleton(
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = topPadding + MellowSpacing.Sp4,
            start = MellowSpacing.Sp4,
            end = MellowSpacing.Sp4,
            bottom = MellowSpacing.Sp16,
        ),
        horizontalArrangement = Arrangement.spacedBy(MellowSpacing.Sp4),
        verticalArrangement = Arrangement.spacedBy(MellowSpacing.Sp4),
    ) {
        items(8) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MellowShapes.Medium)
                    .background(MellowTheme.colors.surface)
                    .padding(MellowSpacing.Sp3),
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    cornerRadius = 12.dp,
                )
                Spacer(Modifier.height(MellowSpacing.Sp3))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp),
                    cornerRadius = 4.dp,
                )
                Spacer(Modifier.height(MellowSpacing.Sp2))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp),
                    cornerRadius = 4.dp,
                )
            }
        }
    }
}

@Composable
fun ArtistListSkeleton(
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = topPadding + MellowSpacing.Sp4,
            start = MellowSpacing.Sp4,
            end = MellowSpacing.Sp4,
            bottom = MellowSpacing.Sp16,
        ),
        verticalArrangement = Arrangement.spacedBy(MellowSpacing.Sp3),
    ) {
        items(8) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MellowTheme.colors.surface)
                    .padding(MellowSpacing.Sp3),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MellowTheme.colors.surfaceElevated),
                ) {
                    PixelBlastPlaceholder(
                        modifier = Modifier.fillMaxSize(),
                        color = MellowTheme.colors.foreground.copy(alpha = 0.15f),
                    )
                }
                Spacer(Modifier.width(MellowSpacing.Sp4))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(16.dp),
                        cornerRadius = 4.dp,
                    )
                    Spacer(Modifier.height(MellowSpacing.Sp2))
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(12.dp),
                        cornerRadius = 4.dp,
                    )
                }
            }
        }
    }
}

@Composable
fun TrackListSkeleton(
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = topPadding + MellowSpacing.Sp4,
            start = MellowSpacing.Sp4,
            end = MellowSpacing.Sp4,
            bottom = MellowSpacing.Sp16,
        ),
        verticalArrangement = Arrangement.spacedBy(MellowSpacing.Sp2),
    ) {
        items(10) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MellowTheme.colors.surface)
                    .padding(MellowSpacing.Sp3),
            ) {
                SkeletonBox(
                    modifier = Modifier.size(44.dp),
                    cornerRadius = 8.dp,
                )
                Spacer(Modifier.width(MellowSpacing.Sp3))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(15.dp),
                        cornerRadius = 4.dp,
                    )
                    Spacer(Modifier.height(MellowSpacing.Sp2))
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(12.dp),
                        cornerRadius = 4.dp,
                    )
                }
            }
        }
    }
}
