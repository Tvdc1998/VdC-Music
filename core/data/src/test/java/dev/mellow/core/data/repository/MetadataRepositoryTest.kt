package dev.mellow.core.data.repository

import android.content.Context
import dev.mellow.core.common.MellowResult
import dev.mellow.core.database.dao.AlbumDao
import dev.mellow.core.database.dao.ArtistDao
import dev.mellow.core.database.dao.TrackDao
import dev.mellow.core.database.entity.TrackEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MetadataRepositoryTest {

    private val context = mockk<Context>(relaxed = true)
    private val trackDao = mockk<TrackDao>(relaxed = true)
    private val albumDao = mockk<AlbumDao>(relaxed = true)
    private val artistDao = mockk<ArtistDao>(relaxed = true)
    private val okHttpClient = mockk<OkHttpClient>(relaxed = true)

    private lateinit var repository: MetadataRepositoryImpl

    @Before
    fun setUp() {
        repository = MetadataRepositoryImpl(context, trackDao, albumDao, artistDao, okHttpClient)
    }

    @Test
    fun searchOnlineMetadata_blankQuery_returnsEmptyList() = runTest {
        val result = repository.searchOnlineMetadata("")
        assertTrue(result is MellowResult.Success)
        assertTrue((result as MellowResult.Success).data.isEmpty())
    }

    @Test
    fun searchOnlineMetadata_validJson_parsesCorrectly() = runTest {
        val jsonResponse = """
            {
              "recordings": [
                {
                  "id": "rec-123",
                  "title": "Bohemian Rhapsody",
                  "artist-credit": [
                    { "name": "Queen" }
                  ],
                  "releases": [
                    {
                      "id": "rel-456",
                      "title": "A Night at the Opera",
                      "date": "1975-11-21",
                      "media": [
                        {
                          "position": 1,
                          "track": [
                            { "position": 11 }
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val response = Response.Builder()
            .request(Request.Builder().url("https://musicbrainz.org").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody("application/json".toMediaType()))
            .build()

        val call = mockk<Call>()
        every { call.execute() } returns response
        every { okHttpClient.newCall(any()) } returns call

        val result = repository.searchOnlineMetadata("Queen Bohemian Rhapsody")

        if (result is MellowResult.Error) {
            throw result.exception
        }
        assertTrue(result is MellowResult.Success)
        val list = (result as MellowResult.Success).data
        assertEquals(1, list.size)
        val match = list.first()
        assertEquals("Bohemian Rhapsody", match.title)
        assertEquals("Queen", match.artist)
        assertEquals("A Night at the Opera", match.album)
        assertEquals(1975, match.year)
        assertEquals(11, match.trackNumber)
        assertEquals("https://coverartarchive.org/release/rel-456/front-500", match.coverUrl)
    }

    @Test
    fun updateTrackMetadata_existingTrack_updatesDao() = runTest {
        val existingTrack = TrackEntity(
            id = "track-1",
            serverId = "server-1",
            name = "Old Title",
            sortName = "Old Title",
            albumId = "album-1",
            albumName = "Old Album",
            artistId = "artist-1",
            artistName = "Old Artist",
            trackNumber = 1,
            discNumber = 1,
            durationMs = 180000,
            genres = listOf("Rock"),
            imageTag = null,
            isFavorite = false,
            playCount = 0,
            lastPlayedAt = 0,
            normalizationGain = null,
            container = null,
            codec = null,
            bitrate = null,
            sampleRate = null,
            channels = null,
            resolvedArtistId = null,
            dateAdded = 0,
            lastSynced = 0,
        )

        coEvery { trackDao.getTrackById("track-1") } returns existingTrack
        coEvery { albumDao.getAlbumById("album-1") } returns null

        val result = repository.updateTrackMetadata(
            trackId = "track-1",
            title = "New Title",
            artist = "New Artist",
            album = "New Album",
            genre = "Pop",
            year = 2022,
            trackNumber = 2,
            discNumber = 1,
            coverUrl = null,
        )

        assertTrue(result is MellowResult.Success)

        val slot = slot<List<TrackEntity>>()
        coVerify { trackDao.upsertTracks(capture(slot)) }
        val updated = slot.captured.first()
        assertEquals("New Title", updated.name)
        assertEquals("New Artist", updated.artistName)
        assertEquals("New Album", updated.albumName)
        assertEquals(listOf("Pop"), updated.genres)
        assertEquals(2, updated.trackNumber)
    }

    @Test
    fun updateTrackMetadata_withCoverUrl_updatesImageTag() = runTest {
        val existingTrack = TrackEntity(
            id = "track-2",
            serverId = "server-1",
            name = "Title",
            sortName = "Title",
            albumId = "album-2",
            albumName = "Album",
            artistId = "artist-1",
            artistName = "Artist",
            trackNumber = 1,
            discNumber = 1,
            durationMs = 180000,
            genres = listOf("Rock"),
            imageTag = null,
            isFavorite = false,
            playCount = 0,
            lastPlayedAt = 0,
            normalizationGain = null,
            container = null,
            codec = null,
            bitrate = null,
            sampleRate = null,
            channels = null,
            resolvedArtistId = null,
            dateAdded = 0,
            lastSynced = 0,
        )

        coEvery { trackDao.getTrackById("track-2") } returns existingTrack
        coEvery { albumDao.getAlbumById("album-2") } returns null

        val result = repository.updateTrackMetadata(
            trackId = "track-2",
            title = "Title",
            artist = "Artist",
            album = "Album",
            genre = null,
            year = null,
            trackNumber = null,
            discNumber = null,
            coverUrl = "https://coverartarchive.org/release/rel-123/front-500",
        )

        assertTrue(result is MellowResult.Success)

        val slot = slot<List<TrackEntity>>()
        coVerify { trackDao.upsertTracks(capture(slot)) }
        val updated = slot.captured.first()
        assertEquals("local_art_track-2", updated.imageTag)
    }
}
