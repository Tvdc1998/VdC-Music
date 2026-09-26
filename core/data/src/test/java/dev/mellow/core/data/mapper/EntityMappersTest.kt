package dev.mellow.core.data.mapper

import dev.mellow.core.common.getArtworkUrl
import dev.mellow.core.database.entity.AlbumEntity
import dev.mellow.core.database.entity.ArtistEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntityMappersTest {

    @Test
    fun `Jellyfin album mapping preserves item ID when imageTag present`() {
        val entity = AlbumEntity(
            id = "album-uuid-123",
            serverId = "server-1",
            name = "Test Album",
            sortName = "test album",
            artistId = "artist-uuid-456",
            artistName = "Test Artist",
            year = 2024,
            trackCount = 10,
            genres = listOf("Rock"),
            imageTag = "hash_tag_abc",
            isFavorite = false,
            resolvedArtistId = null,
            dateAdded = 0L,
            lastSynced = 0L,
        )

        val model = entity.toModel()
        assertEquals("album-uuid-123", model.imageId)

        val url = getArtworkUrl("http://jellyfin:8096", model.imageId)
        assertEquals("http://jellyfin:8096/Items/album-uuid-123/Images/Primary?maxWidth=600&quality=90&format=Webp", url)
    }

    @Test
    fun `Jellyfin album mapping returns null imageId when imageTag missing`() {
        val entity = AlbumEntity(
            id = "album-uuid-123",
            serverId = "server-1",
            name = "Test Album",
            sortName = "test album",
            artistId = null,
            artistName = null,
            year = null,
            trackCount = 0,
            genres = emptyList(),
            imageTag = null,
            isFavorite = false,
            resolvedArtistId = null,
            dateAdded = 0L,
            lastSynced = 0L,
        )

        val model = entity.toModel()
        assertNull(model.imageId)

        val url = getArtworkUrl("http://jellyfin:8096", model.imageId)
        assertNull(url)
    }

    @Test
    fun `Local album mapping uses imageTag or fallback ID`() {
        val entity = AlbumEntity(
            id = "local_album_999",
            serverId = "local_device",
            name = "Local Album",
            sortName = "local album",
            artistId = "local_artist_111",
            artistName = "Local Artist",
            year = 2023,
            trackCount = 5,
            genres = emptyList(),
            imageTag = "local_album_999",
            isFavorite = false,
            resolvedArtistId = null,
            dateAdded = 0L,
            lastSynced = 0L,
        )

        val model = entity.toModel()
        assertEquals("local_album_999", model.imageId)

        val url = getArtworkUrl(null, model.imageId)
        assertEquals("content://com.malinskiy.mellow.artwork/local_album_999", url)
    }

    @Test
    fun `Local artist mapping resolves to album imageTag`() {
        val entity = ArtistEntity(
            id = "local_artist_111",
            serverId = "local_device",
            name = "Local Artist",
            sortName = "local artist",
            albumCount = 2,
            imageTag = "local_album_999",
            isFavorite = false,
            overview = null,
            genres = emptyList(),
            cleanName = "local artist",
            musicBrainzId = null,
            lastSynced = 0L,
        )

        val model = entity.toModel()
        assertEquals("local_album_999", model.imageId)

        val url = getArtworkUrl("local://device", model.imageId)
        assertEquals("content://com.malinskiy.mellow.artwork/local_album_999", url)
    }
}
