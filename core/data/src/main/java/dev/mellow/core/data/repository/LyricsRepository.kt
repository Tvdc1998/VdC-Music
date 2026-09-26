package dev.mellow.core.data.repository

import dev.mellow.core.common.MellowResult

data class OnlineLyricsResult(
    val id: String,
    val trackName: String,
    val artistName: String,
    val albumName: String?,
    val durationSeconds: Double?,
    val isSynced: Boolean,
    val plainLyrics: String?,
    val syncedLyrics: String?,
)

data class ParsedLyricsLine(
    val startMs: Long,
    val text: String,
)

interface LyricsRepository {
    suspend fun fetchCachedLyrics(trackId: String): List<ParsedLyricsLine>
    suspend fun searchOnlineLyrics(query: String): MellowResult<List<OnlineLyricsResult>>
    suspend fun saveLyrics(trackId: String, serverId: String, lines: List<ParsedLyricsLine>): MellowResult<Unit>
    fun parseLrcLyrics(lrcText: String): List<ParsedLyricsLine>
}
