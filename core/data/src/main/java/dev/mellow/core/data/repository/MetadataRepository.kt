package dev.mellow.core.data.repository

import dev.mellow.core.common.MellowResult
import dev.mellow.core.model.MetadataSearchResult

interface MetadataRepository {
    suspend fun searchOnlineMetadata(query: String): MellowResult<List<MetadataSearchResult>>
    suspend fun updateTrackMetadata(
        trackId: String,
        title: String,
        artist: String,
        album: String,
        genre: String?,
        year: Int?,
        trackNumber: Int?,
        discNumber: Int?,
        coverUrl: String?,
    ): MellowResult<Unit>
}
