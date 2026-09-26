package dev.mellow.core.data.scanner

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mellow.core.database.dao.AlbumDao
import dev.mellow.core.database.dao.ArtistAliasDao
import dev.mellow.core.database.dao.ArtistDao
import dev.mellow.core.database.dao.ServerDao
import dev.mellow.core.database.dao.TrackDao
import dev.mellow.core.database.entity.AlbumEntity
import dev.mellow.core.database.entity.ArtistAliasEntity
import dev.mellow.core.database.entity.ArtistEntity
import dev.mellow.core.database.entity.ServerEntity
import dev.mellow.core.database.entity.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverDao: ServerDao,
    private val trackDao: TrackDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val artistAliasDao: ArtistAliasDao,
) {
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    suspend fun scanLocalLibrary(): Result<Int> = withContext(Dispatchers.IO) {
        if (!hasStoragePermission()) {
            return@withContext Result.failure(SecurityException("Permission denied"))
        }

        try {
            val localServer = ServerEntity(
                id = LOCAL_SERVER_ID,
                name = "Local Device",
                url = "local://device",
                userId = "local_user",
                accessToken = "local",
                isActive = serverDao.getActiveServer() == null,
                lastConnected = System.currentTimeMillis(),
            )
            serverDao.upsert(localServer)

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.MIME_TYPE,
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC",
            ) ?: return@withContext Result.success(0)

            val scannedTrackEntities = mutableListOf<TrackEntity>()
            val albumMap = mutableMapOf<String, AlbumEntity>()
            val artistMap = mutableMapOf<String, ArtistEntity>()
            val artistAliasList = mutableListOf<ArtistAliasEntity>()

            cursor.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val artistIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dateAddedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                val now = System.currentTimeMillis()

                while (c.moveToNext()) {
                    val mediaId = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unknown Track"
                    val rawArtist = c.getString(artistCol) ?: "Unknown Artist"
                    val rawArtistId = c.getLong(artistIdCol)
                    val rawAlbum = c.getString(albumCol) ?: "Unknown Album"
                    val rawAlbumId = c.getLong(albumIdCol)
                    val durationMs = c.getLong(durationCol)
                    val trackNo = c.getInt(trackCol).let { if (it <= 0) null else it % 1000 }
                    val year = c.getInt(yearCol).let { if (it <= 0) null else it }
                    val dateAdded = c.getLong(dateAddedCol)
                    val mimeType = c.getString(mimeCol)

                    val trackId = "local_track_$mediaId"
                    val albumId = "local_album_$rawAlbumId"
                    val artistId = "local_artist_$rawArtistId"

                    if (!artistMap.containsKey(artistId)) {
                        val artistEntity = ArtistEntity(
                            id = artistId,
                            serverId = LOCAL_SERVER_ID,
                            name = rawArtist,
                            sortName = rawArtist.lowercase(),
                            albumCount = 1,
                            imageTag = albumId,
                            isFavorite = false,
                            overview = null,
                            genres = emptyList(),
                            cleanName = rawArtist.lowercase(),
                            musicBrainzId = null,
                            lastSynced = now,
                        )
                        artistMap[artistId] = artistEntity
                        artistAliasList.add(
                            ArtistAliasEntity(
                                serverId = LOCAL_SERVER_ID,
                                rawArtistId = artistId,
                                canonicalArtistId = artistId,
                                lastSynced = now,
                            )
                        )
                    }

                    if (!albumMap.containsKey(albumId)) {
                        val albumEntity = AlbumEntity(
                            id = albumId,
                            serverId = LOCAL_SERVER_ID,
                            name = rawAlbum,
                            sortName = rawAlbum.lowercase(),
                            artistId = artistId,
                            artistName = rawArtist,
                            year = year,
                            trackCount = 1,
                            genres = emptyList(),
                            imageTag = albumId,
                            isFavorite = false,
                            resolvedArtistId = artistId,
                            dateAdded = dateAdded,
                            lastSynced = now,
                        )
                        albumMap[albumId] = albumEntity
                    } else {
                        val current = albumMap[albumId]!!
                        albumMap[albumId] = current.copy(trackCount = current.trackCount + 1)
                    }

                    val trackEntity = TrackEntity(
                        id = trackId,
                        serverId = LOCAL_SERVER_ID,
                        name = title,
                        sortName = title.lowercase(),
                        albumId = albumId,
                        albumName = rawAlbum,
                        artistId = artistId,
                        artistName = rawArtist,
                        trackNumber = trackNo,
                        discNumber = 1,
                        durationMs = durationMs,
                        genres = emptyList(),
                        imageTag = albumId,
                        isFavorite = false,
                        playCount = 0,
                        lastPlayedAt = 0L,
                        normalizationGain = null,
                        container = mimeType?.removePrefix("audio/"),
                        codec = mimeType?.removePrefix("audio/"),
                        bitrate = null,
                        sampleRate = null,
                        channels = null,
                        resolvedArtistId = artistId,
                        dateAdded = dateAdded,
                        lastSynced = now,
                    )
                    scannedTrackEntities.add(trackEntity)
                }
            }

            if (artistMap.isNotEmpty()) {
                artistDao.upsertArtists(artistMap.values.toList())
            }
            if (artistAliasList.isNotEmpty()) {
                artistAliasDao.upsertAliases(artistAliasList)
            }
            if (albumMap.isNotEmpty()) {
                albumDao.upsertAlbums(albumMap.values.toList())
            }
            if (scannedTrackEntities.isNotEmpty()) {
                trackDao.upsertTracks(scannedTrackEntities)
            }

            albumDao.resolveArtistIds(LOCAL_SERVER_ID)
            albumDao.resolveArtistAliases(LOCAL_SERVER_ID)
            trackDao.resolveArtistIds(LOCAL_SERVER_ID)
            trackDao.resolveArtistAliases(LOCAL_SERVER_ID)

            Log.d(TAG, "Local scan complete: ${scannedTrackEntities.size} tracks, ${albumMap.size} albums, ${artistMap.size} artists")
            Result.success(scannedTrackEntities.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning local media store", e)
            Result.failure(e)
        }
    }

    companion object {
        const val LOCAL_SERVER_ID = "local_device"
        private const val TAG = "LocalMediaScanner"
    }
}
