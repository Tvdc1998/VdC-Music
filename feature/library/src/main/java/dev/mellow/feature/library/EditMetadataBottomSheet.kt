package dev.mellow.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import dev.mellow.core.designsystem.icon.PhosphorIcons
import dev.mellow.core.designsystem.theme.MellowShapes
import dev.mellow.core.designsystem.theme.MellowSpacing
import dev.mellow.core.designsystem.theme.MellowTheme
import dev.mellow.core.model.MetadataSearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMetadataBottomSheet(
    trackId: String,
    onDismiss: () -> Unit,
    viewModel: EditMetadataViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(trackId) {
        viewModel.loadTrack(trackId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MellowTheme.colors.surfaceElevated,
        contentColor = MellowTheme.colors.foreground,
        dragHandle = {
            Spacer(
                modifier = Modifier
                    .padding(vertical = MellowSpacing.Sp3)
                    .size(width = 36.dp, height = 4.dp)
                    .background(MellowTheme.colors.muted.copy(alpha = 0.4f), MellowShapes.Full),
            )
        },
    ) {
        when (val state = uiState) {
            is EditMetadataUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MellowTheme.colors.accent)
                }
            }
            is EditMetadataUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MellowSpacing.Sp6),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.message,
                        color = MellowTheme.colors.foreground,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(MellowSpacing.Sp4))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MellowTheme.colors.accent,
                        ),
                    ) {
                        Text("Close")
                    }
                }
            }
            is EditMetadataUiState.Success -> {
                LaunchedEffect(state.saveSuccess) {
                    if (state.saveSuccess) {
                        onDismiss()
                    }
                }

                EditMetadataContent(
                    state = state,
                    onUpdateForm = { transform -> viewModel.updateForm(transform) },
                    onSearchOnline = { viewModel.searchOnlineMetadata() },
                    onApplyResult = { result -> viewModel.applySearchResult(result) },
                    onSave = { viewModel.saveMetadata() },
                )
            }
        }
    }
}

@Composable
private fun EditMetadataContent(
    state: EditMetadataUiState.Success,
    onUpdateForm: ((EditMetadataFormState) -> EditMetadataFormState) -> Unit,
    onSearchOnline: () -> Unit,
    onApplyResult: (MetadataSearchResult) -> Unit,
    onSave: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val form = state.formState

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MellowSpacing.Sp4)
            .padding(bottom = MellowSpacing.Sp8),
    ) {
        // Top Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MellowSpacing.Sp2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Edit Metadata",
                style = MaterialTheme.typography.titleLarge,
                color = MellowTheme.colors.foreground,
            )

            Button(
                onClick = onSave,
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MellowTheme.colors.accent,
                ),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MellowTheme.colors.foreground,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Save")
                }
            }
        }

        HorizontalDivider(
            color = MellowTheme.colors.border,
            modifier = Modifier.padding(vertical = MellowSpacing.Sp2),
        )

        state.errorMessage?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = MellowSpacing.Sp2),
            )
        }

        // Form Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .verticalScroll(scrollState),
        ) {
            // Online Search Auto-fill button
            OutlinedButton(
                onClick = onSearchOnline,
                enabled = !state.isSearching,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MellowSpacing.Sp2),
                shape = MellowShapes.Medium,
            ) {
                if (state.isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MellowTheme.colors.accent,
                    )
                    Spacer(Modifier.width(MellowSpacing.Sp2))
                    Text("Searching MusicBrainz…", color = MellowTheme.colors.foreground)
                } else {
                    Icon(
                        imageVector = PhosphorIcons.MagnifyingGlass,
                        contentDescription = null,
                        tint = MellowTheme.colors.accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(MellowSpacing.Sp2))
                    Text("Search Online Metadata", color = MellowTheme.colors.foreground)
                }
            }

            // Search Results List
            if (state.searchResults.isNotEmpty()) {
                Text(
                    text = "Select Online Result:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MellowTheme.colors.muted,
                    modifier = Modifier.padding(top = MellowSpacing.Sp2, bottom = MellowSpacing.Sp1),
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MellowTheme.colors.surfaceElevated,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = MellowSpacing.Sp3),
                ) {
                    Column {
                        state.searchResults.take(5).forEachIndexed { idx, result ->
                            if (idx > 0) {
                                HorizontalDivider(color = MellowTheme.colors.border)
                            }
                            SearchResultItem(result = result, onClick = { onApplyResult(result) })
                        }
                    }
                }
            }

            // Form Fields
            val tfColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MellowTheme.colors.accent,
                unfocusedBorderColor = MellowTheme.colors.border,
                focusedLabelColor = MellowTheme.colors.accent,
                unfocusedLabelColor = MellowTheme.colors.muted,
                focusedTextColor = MellowTheme.colors.foreground,
                unfocusedTextColor = MellowTheme.colors.foreground,
            )

            OutlinedTextField(
                value = form.title,
                onValueChange = { val str = it; onUpdateForm { f -> f.copy(title = str) } },
                label = { Text("Title") },
                colors = tfColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MellowSpacing.Sp1),
                singleLine = true,
            )

            OutlinedTextField(
                value = form.artist,
                onValueChange = { val str = it; onUpdateForm { f -> f.copy(artist = str) } },
                label = { Text("Artist") },
                colors = tfColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MellowSpacing.Sp1),
                singleLine = true,
            )

            OutlinedTextField(
                value = form.album,
                onValueChange = { val str = it; onUpdateForm { f -> f.copy(album = str) } },
                label = { Text("Album") },
                colors = tfColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MellowSpacing.Sp1),
                singleLine = true,
            )

            OutlinedTextField(
                value = form.genre,
                onValueChange = { val str = it; onUpdateForm { f -> f.copy(genre = str) } },
                label = { Text("Genre") },
                colors = tfColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MellowSpacing.Sp1),
                singleLine = true,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MellowSpacing.Sp1),
                horizontalArrangement = Arrangement.spacedBy(MellowSpacing.Sp2),
            ) {
                OutlinedTextField(
                    value = form.year,
                    onValueChange = { val str = it; onUpdateForm { f -> f.copy(year = str) } },
                    label = { Text("Year") },
                    colors = tfColors,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = form.trackNumber,
                    onValueChange = { val str = it; onUpdateForm { f -> f.copy(trackNumber = str) } },
                    label = { Text("Track #") },
                    colors = tfColors,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = form.discNumber,
                    onValueChange = { val str = it; onUpdateForm { f -> f.copy(discNumber = str) } },
                    label = { Text("Disc #") },
                    colors = tfColors,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            OutlinedTextField(
                value = form.coverUrl,
                onValueChange = { val str = it; onUpdateForm { f -> f.copy(coverUrl = str) } },
                label = { Text("Artwork URL") },
                colors = tfColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MellowSpacing.Sp1),
                singleLine = true,
            )

            if (form.coverUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .padding(top = MellowSpacing.Sp2)
                        .size(80.dp)
                        .clip(MellowShapes.Small)
                        .background(MellowTheme.colors.border),
                ) {
                    AsyncImage(
                        model = form.coverUrl,
                        contentDescription = "Cover Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(80.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    result: MetadataSearchResult,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MellowSpacing.Sp3, vertical = MellowSpacing.Sp2),
    ) {
        if (!result.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = result.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(MellowShapes.Small),
            )
            Spacer(Modifier.width(MellowSpacing.Sp3))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MellowTheme.colors.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subText = buildString {
                append(result.artist)
                if (result.album.isNotBlank()) append(" • ").append(result.album)
                if (result.year != null) append(" (").append(result.year).append(")")
            }
            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = MellowTheme.colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
