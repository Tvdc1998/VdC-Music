package dev.mellow.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mellow.core.data.repository.OnlineLyricsResult
import dev.mellow.core.designsystem.icon.PhosphorIcons
import dev.mellow.core.designsystem.theme.MellowSpacing
import dev.mellow.core.designsystem.theme.MellowTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchLyricsBottomSheet(
    initialQuery: String,
    onDismiss: () -> Unit,
    onSearch: suspend (String) -> List<OnlineLyricsResult>,
    onSelectResult: (OnlineLyricsResult) -> Unit,
) {
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<OnlineLyricsResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun triggerSearch() {
        if (query.isBlank()) return
        isLoading = true
        errorMsg = null
        scope.launch {
            try {
                results = onSearch(query)
            } catch (e: Exception) {
                errorMsg = e.message ?: "Search failed"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (query.isNotBlank()) {
            triggerSearch()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MellowTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MellowSpacing.Sp6)
                .padding(bottom = MellowSpacing.Sp8),
        ) {
            Text(
                text = "Search Lyrics Online",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MellowTheme.colors.foreground,
            )

            Spacer(Modifier.height(MellowSpacing.Sp4))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Song Title & Artist") },
                trailingIcon = {
                    IconButton(onClick = { triggerSearch() }) {
                        Icon(PhosphorIcons.MagnifyingGlass, contentDescription = "Search")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { triggerSearch() }),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(MellowSpacing.Sp4))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MellowTheme.colors.foreground)
                    }
                }
                errorMsg != null -> {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                }
                results.isEmpty() -> {
                    Text("No lyrics found. Try refining your search query.", color = MellowTheme.colors.muted)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(MellowSpacing.Sp3),
                    ) {
                        items(results, key = { it.id }) { item ->
                            Card(
                                onClick = { onSelectResult(item) },
                                colors = CardDefaults.cardColors(containerColor = MellowTheme.colors.background),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(MellowSpacing.Sp4)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.trackName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MellowTheme.colors.foreground,
                                        )
                                        Text(
                                            text = "${item.artistName} • ${item.albumName ?: "Single"}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MellowTheme.colors.muted,
                                        )
                                    }
                                    Spacer(Modifier.width(MellowSpacing.Sp2))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (item.isSynced) MellowTheme.colors.accent.copy(alpha = 0.2f) else MellowTheme.colors.surface,
                                    ) {
                                        Text(
                                            text = if (item.isSynced) "Synced LRC" else "Plain Text",
                                            fontSize = 11.sp,
                                            color = if (item.isSynced) MellowTheme.colors.accent else MellowTheme.colors.muted,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
