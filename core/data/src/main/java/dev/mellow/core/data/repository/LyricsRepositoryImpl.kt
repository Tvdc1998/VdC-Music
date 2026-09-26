package dev.mellow.core.data.repository

import dev.mellow.core.common.MellowResult
import dev.mellow.core.database.dao.LyricsDao
import dev.mellow.core.database.entity.LyricsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepositoryImpl(
    private val lyricsDao: LyricsDao,
    private val okHttpClient: OkHttpClient = OkHttpClient(),
) : LyricsRepository {

    @Inject
    constructor(
        lyricsDao: LyricsDao,
    ) : this(
        lyricsDao = lyricsDao,
        okHttpClient = OkHttpClient(),
    )

    override suspend fun fetchCachedLyrics(trackId: String): List<ParsedLyricsLine> =
        withContext(Dispatchers.IO) {
            val entity = lyricsDao.getLyrics(trackId) ?: return@withContext emptyList()
            try {
                val arr = JSONArray(entity.lyricsData)
                (0 until arr.length()).mapNotNull { i ->
                    val obj = arr.getJSONObject(i)
                    val text = obj.optString("text", "")
                    if (text.isBlank()) null
                    else ParsedLyricsLine(startMs = obj.optLong("startMs", -1L), text = text)
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

    override suspend fun searchOnlineLyrics(query: String): MellowResult<List<OnlineLyricsResult>> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext MellowResult.Success(emptyList())
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "https://lrclib.net/api/search?q=$encoded"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mellow/1.0.0 ( https://github.com/mellow-player )")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext MellowResult.Error(Exception("LRCLIB query failed with HTTP code ${response.code}"))
                }

                val bodyStr = response.body?.string() ?: return@withContext MellowResult.Success(emptyList())
                val jsonArr = JSONArray(bodyStr)
                val results = mutableListOf<OnlineLyricsResult>()

                val maxItems = minOf(jsonArr.length(), 20)
                for (i in 0 until maxItems) {
                    val obj = jsonArr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "")
                    val trackName = obj.optString("trackName", obj.optString("name", ""))
                    val artistName = obj.optString("artistName", "")
                    val albumName = obj.optString("albumName", null)
                    val duration = if (obj.has("duration") && !obj.isNull("duration")) obj.optDouble("duration") else null
                    val synced: String? = if (obj.has("syncedLyrics") && !obj.isNull("syncedLyrics")) {
                        val s = obj.optString("syncedLyrics")
                        if (s.isNotBlank()) s else null
                    } else null
                    val plain: String? = if (obj.has("plainLyrics") && !obj.isNull("plainLyrics")) {
                        val p = obj.optString("plainLyrics")
                        if (p.isNotBlank()) p else null
                    } else null

                    if (trackName.isNotBlank()) {
                        results.add(
                            OnlineLyricsResult(
                                id = id,
                                trackName = trackName,
                                artistName = artistName,
                                albumName = albumName,
                                durationSeconds = duration,
                                isSynced = synced != null,
                                plainLyrics = plain,
                                syncedLyrics = synced,
                            )
                        )
                    }
                }
                MellowResult.Success(results)
            } catch (e: Exception) {
                MellowResult.Error(e)
            }
        }

    override suspend fun saveLyrics(
        trackId: String,
        serverId: String,
        lines: List<ParsedLyricsLine>,
    ): MellowResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val arr = JSONArray()
            for (line in lines) {
                arr.put(JSONObject().apply {
                    put("startMs", line.startMs)
                    put("text", line.text)
                })
            }
            lyricsDao.upsert(
                LyricsEntity(
                    trackId = trackId,
                    serverId = serverId,
                    lyricsData = arr.toString(),
                    lastSynced = System.currentTimeMillis(),
                )
            )
            MellowResult.Success(Unit)
        } catch (e: Exception) {
            MellowResult.Error(e)
        }
    }

    override fun parseLrcLyrics(lrcText: String): List<ParsedLyricsLine> {
        if (lrcText.isBlank()) return emptyList()
        val timeRegex = Regex("""\[(\d{2,}):(\d{2})(?:\.(\d{2,3}))?]""")
        val result = mutableListOf<ParsedLyricsLine>()
        var isSynced = false

        for (rawLine in lrcText.lines()) {
            val trimmed = rawLine.trim()
            if (trimmed.isBlank()) continue
            if (trimmed.startsWith("[ar:") || trimmed.startsWith("[ti:") ||
                trimmed.startsWith("[al:") || trimmed.startsWith("[by:") ||
                trimmed.startsWith("[length:") || trimmed.startsWith("[re:")) continue

            val matches = timeRegex.findAll(trimmed).toList()
            if (matches.isNotEmpty()) {
                isSynced = true
                val text = trimmed.replace(timeRegex, "").trim()
                for (match in matches) {
                    val min = match.groupValues[1].toLongOrNull() ?: 0L
                    val sec = match.groupValues[2].toLongOrNull() ?: 0L
                    val fracStr = match.groupValues[3]
                    val fracMs = when (fracStr.length) {
                        2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                        3 -> fracStr.toLongOrNull() ?: 0L
                        1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                        else -> 0L
                    }
                    val totalMs = (min * 60 + sec) * 1000 + fracMs
                    result.add(ParsedLyricsLine(startMs = totalMs, text = text))
                }
            } else if (!isSynced) {
                result.add(ParsedLyricsLine(startMs = -1L, text = trimmed))
            }
        }
        return if (isSynced) result.sortedBy { it.startMs } else result
    }
}
