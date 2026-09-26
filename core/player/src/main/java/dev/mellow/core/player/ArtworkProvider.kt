package dev.mellow.core.player

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.mellow.core.data.ArtworkPreCacher
import dev.mellow.core.database.dao.AlbumDao
import dev.mellow.core.database.dao.ArtistDao
import dev.mellow.core.database.dao.ServerDao
import dev.mellow.core.database.dao.TrackDao
import kotlinx.coroutines.runBlocking
import java.io.File

class ArtworkProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ArtworkEntryPoint {
        fun serverDao(): ServerDao
        fun artistDao(): ArtistDao
        fun albumDao(): AlbumDao
        fun trackDao(): TrackDao
        fun artworkPreCacher(): ArtworkPreCacher
    }

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val itemId = uri.lastPathSegment ?: return null
        val ctx = context ?: return null

        val artworkDir = File(ctx.cacheDir, "artwork")
        val candidateNames = listOf(
            "$itemId.webp",
            if (itemId.startsWith("local_art_")) "${itemId.removePrefix("local_art_")}.webp" else null,
        ).filterNotNull()

        for (candidateName in candidateNames) {
            val customFile = File(artworkDir, candidateName)
            if (customFile.exists() && customFile.length() > 0) {
                try {
                    return ParcelFileDescriptor.open(customFile, ParcelFileDescriptor.MODE_READ_ONLY)
                } catch (_: Exception) {}
            }
        }

        if (itemId.startsWith("local_")) {
            val entryPoint = EntryPointAccessors.fromApplication(
                ctx.applicationContext,
                ArtworkEntryPoint::class.java,
            )

            val resolvedAlbumId = when {
                itemId.startsWith("local_album_") -> itemId
                itemId.startsWith("local_artist_") -> {
                    runBlocking {
                        val artist = entryPoint.artistDao().getArtistById(itemId)
                        if (artist?.imageTag?.startsWith("local_album_") == true) {
                            artist.imageTag
                        } else {
                            val albums = entryPoint.albumDao().getAllAlbumsByArtist(itemId)
                            albums.firstOrNull()?.id
                        }
                    }
                }
                itemId.startsWith("local_track_") -> {
                    runBlocking {
                        entryPoint.trackDao().getTrackById(itemId)?.albumId
                    }
                }
                else -> null
            }

            val targetAlbumId = resolvedAlbumId ?: itemId
            val rawAlbumId = targetAlbumId.removePrefix("local_album_").removePrefix("local_").toLongOrNull()

            if (rawAlbumId != null) {
                // Try 1: MediaStore albumart ContentUri
                val albumArtUri = android.content.ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    rawAlbumId,
                )
                try {
                    val pfd = ctx.contentResolver.openFileDescriptor(albumArtUri, "r")
                    if (pfd != null) return pfd
                } catch (_: Exception) {}

                // Try 2: Extract embedded picture via MediaMetadataRetriever from an album track
                val trackEntity = runBlocking {
                    entryPoint.trackDao().getTracksByAlbumSync(targetAlbumId).firstOrNull()
                }
                if (trackEntity != null) {
                    val rawTrackId = trackEntity.id.removePrefix("local_track_").toLongOrNull()
                    if (rawTrackId != null) {
                        val trackUri = android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            rawTrackId,
                        )
                        val embeddedPfd = extractEmbeddedArtFd(ctx, trackUri, rawTrackId)
                        if (embeddedPfd != null) return embeddedPfd
                    }
                }
            }
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            ArtworkEntryPoint::class.java,
        )
        val server = runBlocking { entryPoint.serverDao().getActiveServer() } ?: return null
        val file = entryPoint.artworkPreCacher().resolveArtwork(server.url, server.accessToken, itemId)
            ?: return null
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun extractEmbeddedArtFd(context: Context, trackUri: Uri, rawTrackId: Long): ParcelFileDescriptor? {
        val cacheFile = File(context.cacheDir, "artwork_local_$rawTrackId.jpg")
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return try {
                ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
            } catch (_: Exception) {
                null
            }
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, trackUri)
            val picture = retriever.embeddedPicture ?: return null
            cacheFile.writeBytes(picture)
            ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (_: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    override fun getType(uri: Uri): String = "image/jpeg"
    override fun query(uri: Uri, p: Array<String>?, s: String?, sa: Array<String>?, so: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, sa: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, s: String?, sa: Array<String>?): Int = 0
}
