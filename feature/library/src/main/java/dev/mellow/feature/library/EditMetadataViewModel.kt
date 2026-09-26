package dev.mellow.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mellow.core.common.MellowResult
import dev.mellow.core.data.repository.LibraryRepository
import dev.mellow.core.data.repository.MetadataRepository
import dev.mellow.core.model.MetadataSearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditMetadataFormState(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val genre: String = "",
    val year: String = "",
    val trackNumber: String = "",
    val discNumber: String = "",
    val coverUrl: String = "",
)

sealed interface EditMetadataUiState {
    data object Loading : EditMetadataUiState
    data class Success(
        val trackId: String,
        val formState: EditMetadataFormState,
        val searchResults: List<MetadataSearchResult> = emptyList(),
        val isSearching: Boolean = false,
        val isSaving: Boolean = false,
        val saveSuccess: Boolean = false,
        val errorMessage: String? = null,
    ) : EditMetadataUiState
    data class Error(val message: String) : EditMetadataUiState
}

@HiltViewModel
class EditMetadataViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val metadataRepository: MetadataRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditMetadataUiState>(EditMetadataUiState.Loading)
    val uiState: StateFlow<EditMetadataUiState> = _uiState.asStateFlow()

    private var currentTrackId: String? = savedStateHandle.get<String>("trackId")

    init {
        currentTrackId?.let { loadTrack(it) }
    }

    fun loadTrack(trackId: String) {
        currentTrackId = trackId
        viewModelScope.launch {
            _uiState.value = EditMetadataUiState.Loading
            when (val result = libraryRepository.getTrack(trackId)) {
                is MellowResult.Success -> {
                    val track = result.data
                    if (track == null) {
                        _uiState.value = EditMetadataUiState.Error("Track not found")
                        return@launch
                    }

                    var yearStr = ""
                    val albumId = track.albumId
                    if (!albumId.isNullOrBlank()) {
                        val albumResult = libraryRepository.getAlbum(albumId)
                        if (albumResult is MellowResult.Success && albumResult.data != null) {
                            val album = albumResult.data
                            val yearVal = album?.year
                            if (yearVal != null && yearVal > 0) {
                                yearStr = yearVal.toString()
                            }
                        }
                    }

                    val formState = EditMetadataFormState(
                        title = track.name,
                        artist = track.artistName ?: "",
                        album = track.albumName ?: "",
                        genre = track.genres.firstOrNull() ?: "",
                        year = yearStr,
                        trackNumber = track.trackNumber?.toString() ?: "",
                        discNumber = track.discNumber?.toString() ?: "",
                        coverUrl = "",
                    )

                    _uiState.value = EditMetadataUiState.Success(
                        trackId = trackId,
                        formState = formState,
                    )
                }
                is MellowResult.Error -> {
                    _uiState.value = EditMetadataUiState.Error(
                        result.exception.message ?: "Failed to load track",
                    )
                }
                else -> {
                    _uiState.value = EditMetadataUiState.Error("Unknown error")
                }
            }
        }
    }

    fun updateForm(transform: (EditMetadataFormState) -> EditMetadataFormState) {
        val currentState = _uiState.value as? EditMetadataUiState.Success ?: return
        _uiState.value = currentState.copy(
            formState = transform(currentState.formState),
            errorMessage = null,
        )
    }

    fun searchOnlineMetadata() {
        val currentState = _uiState.value as? EditMetadataUiState.Success ?: return
        val query = "${currentState.formState.artist} ${currentState.formState.title}".trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isSearching = true, errorMessage = null)
            when (val result = metadataRepository.searchOnlineMetadata(query)) {
                is MellowResult.Success -> {
                    _uiState.value = (_uiState.value as? EditMetadataUiState.Success)?.copy(
                        searchResults = result.data,
                        isSearching = false,
                    ) ?: currentState
                }
                is MellowResult.Error -> {
                    _uiState.value = (_uiState.value as? EditMetadataUiState.Success)?.copy(
                        isSearching = false,
                        errorMessage = result.exception.message ?: "Failed to search online metadata",
                    ) ?: currentState
                }
                else -> {
                    _uiState.value = (_uiState.value as? EditMetadataUiState.Success)?.copy(
                        isSearching = false,
                        errorMessage = "Unknown search error",
                    ) ?: currentState
                }
            }
        }
    }

    fun applySearchResult(result: MetadataSearchResult) {
        val currentState = _uiState.value as? EditMetadataUiState.Success ?: return
        val updatedForm = currentState.formState.copy(
            title = result.title.ifBlank { currentState.formState.title },
            artist = result.artist.ifBlank { currentState.formState.artist },
            album = result.album.ifBlank { currentState.formState.album },
            genre = result.genre ?: currentState.formState.genre,
            year = result.year?.toString() ?: currentState.formState.year,
            trackNumber = result.trackNumber?.toString() ?: currentState.formState.trackNumber,
            discNumber = result.discNumber?.toString() ?: currentState.formState.discNumber,
            coverUrl = result.coverUrl ?: currentState.formState.coverUrl,
        )
        _uiState.value = currentState.copy(
            formState = updatedForm,
            searchResults = emptyList(),
        )
    }

    fun saveMetadata() {
        val currentState = _uiState.value as? EditMetadataUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = currentState.copy(isSaving = true, errorMessage = null)
            val form = currentState.formState
            val result = metadataRepository.updateTrackMetadata(
                trackId = currentState.trackId,
                title = form.title,
                artist = form.artist,
                album = form.album,
                genre = form.genre.takeIf { it.isNotBlank() },
                year = form.year.toIntOrNull(),
                trackNumber = form.trackNumber.toIntOrNull(),
                discNumber = form.discNumber.toIntOrNull(),
                coverUrl = form.coverUrl.takeIf { it.isNotBlank() },
            )

            when (result) {
                is MellowResult.Success -> {
                    _uiState.value = (_uiState.value as? EditMetadataUiState.Success)?.copy(
                        isSaving = false,
                        saveSuccess = true,
                    ) ?: currentState
                }
                is MellowResult.Error -> {
                    _uiState.value = (_uiState.value as? EditMetadataUiState.Success)?.copy(
                        isSaving = false,
                        errorMessage = result.exception.message ?: "Failed to save metadata",
                    ) ?: currentState
                }
                else -> {
                    _uiState.value = (_uiState.value as? EditMetadataUiState.Success)?.copy(
                        isSaving = false,
                        errorMessage = "Unknown save error",
                    ) ?: currentState
                }
            }
        }
    }
}
