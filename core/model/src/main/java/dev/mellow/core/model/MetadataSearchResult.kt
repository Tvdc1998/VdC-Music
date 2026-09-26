package dev.mellow.core.model

data class MetadataSearchResult(
    val title: String,
    val artist: String,
    val album: String,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val genre: String? = null,
    val coverUrl: String? = null,
)
