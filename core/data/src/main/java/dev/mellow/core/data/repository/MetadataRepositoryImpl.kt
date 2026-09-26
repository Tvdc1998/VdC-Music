package dev.mellow.core.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mellow.core.common.MellowResult
import dev.mellow.core.database.dao.AlbumDao
import dev.mellow.core.database.dao.ArtistDao
import dev.mellow.core.database.dao.TrackDao
import dev.mellow.core.model.MetadataSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataRepositoryImpl(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val okHttpClient: OkHttpClient = OkHttpClient(),
) : MetadataRepository {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        trackDao: TrackDao,
        albumDao: AlbumDao,
        artistDao: ArtistDao,
    ) : this(
        context = context,
        trackDao = trackDao,
        albumDao = albumDao,
        artistDao = artistDao,
        okHttpClient = OkHttpClient(),
    )

    override suspend fun searchOnlineMetadata(query: String): MellowResult<List<MetadataSearchResult>> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) {
                return@withContext MellowResult.Success(emptyList())
            }

            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = "https://musicbrainz.org/ws/2/recording/?query=$encodedQuery&fmt=json"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mellow/1.0.0 ( https://github.com/mellow-player )")
                    .header("Accept", "application/json")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext MellowResult.Error(
                        Exception("MusicBrainz query failed with HTTP code ${response.code}"),
                    )
                }

                val bodyString = response.body?.string()
                    ?: return@withContext MellowResult.Success(emptyList())

                val json = JSONObject(bodyString)
                val recordings = json.optJSONArray("recordings")
                    ?: return@withContext MellowResult.Success(emptyList())

                val results = mutableListOf<MetadataSearchResult>()
                val maxItems = minOf(recordings.length(), 20)

                for (i in 0 until maxItems) {
                    val recObj = recordings.optJSONObject(i) ?: continue
                    val title = recObj.optString("title", "")
                    if (title.isBlank()) continue

                    var artistName = ""
                    val artistCredit = recObj.optJSONArray("artist-credit")
                    if (artistCredit != null && artistCredit.length() > 0) {
                        val names = mutableListOf<String>()
                        for (j in 0 until artistCredit.length()) {
                            val creditObj = artistCredit.optJSONObject(j) ?: continue
                            val name = creditObj.optString("name", "")
                            if (name.isNotBlank()) names.add(name)
                        }
                        artistName = names.joinToString(", ")
                    }

                    var albumName = ""
                    var releaseId: String? = null
                    var year: Int? = null
                    var trackNum: Int? = null
                    var discNum: Int? = null

                    val releases = recObj.optJSONArray("releases")
                    if (releases != null && releases.length() > 0) {
                        val firstRelease = releases.optJSONObject(0)
                        if (firstRelease != null) {
                            albumName = firstRelease.optString("title", "")
                            val relId = firstRelease.optString("id", "")
                            if (relId.isNotBlank()) {
                                releaseId = relId
                            }

                            val dateStr = firstRelease.optString("date", "")
                            if (dateStr.length >= 4) {
                                year = dateStr.substring(0, 4).toIntOrNull()
                            }

                            val media = firstRelease.optJSONArray("media")
                            if (media != null && media.length() > 0) {
                                val firstMedia = media.optJSONObject(0)
                                if (firstMedia != null) {
                                    val pos = firstMedia.optInt("position", 1)
                                    if (pos > 0) discNum = pos

                                    val tracksArr = firstMedia.optJSONArray("track")
                                    if (tracksArr != null && tracksArr.length() > 0) {
                                        val trackObj = tracksArr.optJSONObject(0)
                                        if (trackObj != null) {
                                            val trackPos = trackObj.optInt("position", 0)
                                            if (trackPos > 0) {
                                                trackNum = trackPos
                                            } else {
                                                trackNum = trackObj.optString("number", "").toIntOrNull()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val coverUrl = releaseId?.let { "https://coverartarchive.org/release/$it/front-500" }

                    results.add(
                        MetadataSearchResult(
                            title = title,
                            artist = artistName,
                            album = albumName,
                            year = year,
                            trackNumber = trackNum,
                            discNumber = discNum,
                            genre = null,
                            coverUrl = coverUrl,
                        ),
                    )
                }

                MellowResult.Success(results)
            } catch (e: Exception) {
                MellowResult.Error(e)
            }
        }

    override suspend fun updateTrackMetadata(
        trackId: String,
        title: String,
        artist: String,
        album: String,
        genre: String?,
        year: Int?,
        trackNumber: Int?,
        discNumber: Int?,
        coverUrl: String?,
    ): MellowResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingTrack = trackDao.getTrackById(trackId)
                ?: return@withContext MellowResult.Error(Exception("Track not found: $trackId"))

            val albumId = existingTrack.albumId
            val newTrackImageTag = if (!coverUrl.isNullOrBlank()) "local_art_$trackId" else existingTrack.imageTag

            val updatedTrack = existingTrack.copy(
                name = title.ifBlank { existingTrack.name },
                sortName = title.ifBlank { existingTrack.sortName },
                artistName = artist.ifBlank { existingTrack.artistName },
                albumName = album.ifBlank { existingTrack.albumName },
                genres = if (!genre.isNullOrBlank()) listOf(genre) else existingTrack.genres,
                trackNumber = trackNumber ?: existingTrack.trackNumber,
                discNumber = discNumber ?: existingTrack.discNumber,
                imageTag = newTrackImageTag,
                lastSynced = System.currentTimeMillis(),
            )

            trackDao.upsertTracks(listOf(updatedTrack))

            if (!albumId.isNullOrBlank()) {
                val existingAlbum = albumDao.getAlbumById(albumId)
                if (existingAlbum != null) {
                    val newAlbumImageTag = if (!coverUrl.isNullOrBlank()) "local_art_$albumId" else existingAlbum.imageTag
                    val updatedAlbum = existingAlbum.copy(
                        name = album.ifBlank { existingAlbum.name },
                        sortName = album.ifBlank { existingAlbum.sortName },
                        artistName = artist.ifBlank { existingAlbum.artistName },
                        year = year ?: existingAlbum.year,
                        genres = if (!genre.isNullOrBlank()) listOf(genre) else existingAlbum.genres,
                        imageTag = newAlbumImageTag,
                        lastSynced = System.currentTimeMillis(),
                    )
                    albumDao.upsertAlbums(listOf(updatedAlbum))
                }
            }

            if (!coverUrl.isNullOrBlank()) {
                try {
                    val bytes: ByteArray? = when {
                        coverUrl.startsWith("http://") || coverUrl.startsWith("https://") -> {
                            val request = Request.Builder()
                                .url(coverUrl)
                                .header("User-Agent", "Mellow/1.0.0 ( https://github.com/mellow-player )")
                                .build()
                            val response = okHttpClient.newCall(request).execute()
                            if (response.isSuccessful) response.body?.bytes() else null
                        }
                        coverUrl.startsWith("content://") || coverUrl.startsWith("file://") -> {
                            val uri = android.net.Uri.parse(coverUrl)
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        }
                        else -> null
                    }

                    if (bytes != null && bytes.isNotEmpty()) {
                        val artworkDir = File(context.cacheDir, "artwork").apply { mkdirs() }
                        val idsToSave = mutableSetOf(trackId, "local_art_$trackId")
                        if (!albumId.isNullOrBlank()) {
                            idsToSave.add(albumId)
                            idsToSave.add("local_art_$albumId")
                        }

                        for (id in idsToSave) {
                            val cacheFile = File(artworkDir, "$id.webp")
                            val noArtMarker = File(artworkDir, "$id.noart")
                            cacheFile.writeBytes(bytes)
                            if (noArtMarker.exists()) {
                                noArtMarker.delete()
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Ignore cover download failures during tag update
                }
            }

            MellowResult.Success(Unit)
        } catch (e: Exception) {
            MellowResult.Error(e)
        }
    }
}
