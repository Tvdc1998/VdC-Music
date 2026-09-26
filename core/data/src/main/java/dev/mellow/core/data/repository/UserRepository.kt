package dev.mellow.core.data.repository

import dev.mellow.core.common.MellowResult
import dev.mellow.core.model.Server

interface UserRepository {
    suspend fun authenticate(serverUrl: String, username: String, password: String): MellowResult<Server>
    suspend fun getServers(): MellowResult<List<Server>>
    fun observeServers(): kotlinx.coroutines.flow.Flow<List<Server>>
    fun observeActiveServer(): kotlinx.coroutines.flow.Flow<Server?>
    suspend fun switchServer(serverId: String): MellowResult<Unit>
    suspend fun setFavorite(itemId: String, isFavorite: Boolean): MellowResult<Unit>
    suspend fun reportPlaybackProgress(itemId: String, positionMs: Long): MellowResult<Unit>
    suspend fun reportPlaybackStarted(itemId: String): MellowResult<Unit>
    suspend fun reportPlaybackStopped(itemId: String, positionMs: Long): MellowResult<Unit>
    suspend fun logout(): MellowResult<Unit>
}
