package dev.mellow.core.data.repository

import dev.mellow.core.common.MellowResult
import dev.mellow.core.database.dao.LyricsDao
import dev.mellow.core.database.entity.LyricsEntity
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
class LyricsRepositoryTest {

    private val lyricsDao = mockk<LyricsDao>(relaxed = true)
    private val okHttpClient = mockk<OkHttpClient>(relaxed = true)

    private lateinit var repository: LyricsRepositoryImpl

    @Before
    fun setUp() {
        repository = LyricsRepositoryImpl(lyricsDao, okHttpClient)
    }

    @Test
    fun parseLrcLyrics_validLrc_parsesSyncedTimestamps() {
        val lrc = """
            [00:12.34] Line one
            [01:05.50] Line two
        """.trimIndent()

        val lines = repository.parseLrcLyrics(lrc)
        assertEquals(2, lines.size)
        assertEquals(12340L, lines[0].startMs)
        assertEquals("Line one", lines[0].text)
        assertEquals(65500L, lines[1].startMs)
        assertEquals("Line two", lines[1].text)
    }

    @Test
    fun searchOnlineLyrics_validResponse_returnsLyricsList() = runTest {
        val jsonResponse = """
            [
              {
                "id": "lrc-1",
                "trackName": "Hotel California",
                "artistName": "Eagles",
                "albumName": "Hotel California",
                "duration": 390.0,
                "syncedLyrics": "[00:10.00] On a dark desert highway",
                "plainLyrics": "On a dark desert highway"
              }
            ]
        """.trimIndent()

        val response = Response.Builder()
            .request(Request.Builder().url("https://lrclib.net").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody("application/json".toMediaType()))
            .build()

        val call = mockk<Call>()
        every { call.execute() } returns response
        every { okHttpClient.newCall(any()) } returns call

        val result = repository.searchOnlineLyrics("Eagles Hotel California")
        assertTrue(result is MellowResult.Success)
        val list = (result as MellowResult.Success).data
        assertEquals(1, list.size)
        assertEquals("Hotel California", list[0].trackName)
        assertEquals("Eagles", list[0].artistName)
        assertTrue(list[0].isSynced)
    }

    @Test
    fun saveLyrics_validLines_upsertsToDao() = runTest {
        val lines = listOf(
            ParsedLyricsLine(10000L, "Hello world"),
        )

        val result = repository.saveLyrics("track-123", "local_device", lines)
        assertTrue(result is MellowResult.Success)

        val slot = slot<LyricsEntity>()
        coVerify { lyricsDao.upsert(capture(slot)) }
        assertEquals("track-123", slot.captured.trackId)
        assertEquals("local_device", slot.captured.serverId)
    }
}
