package com.example.podlingo.data.repository

import com.example.podlingo.data.local.dao.PlaylistDao
import com.example.podlingo.data.local.dao.PlaylistEpisodeItem
import com.example.podlingo.data.local.dao.PlaylistWithCount
import com.example.podlingo.data.local.entity.PlaylistEntity
import com.example.podlingo.data.local.entity.PlaylistEpisodeCrossRef
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class PlaylistRepository @Inject constructor(private val playlistDao: PlaylistDao) {

    fun getPlaylists(): Flow<List<PlaylistWithCount>> = playlistDao.getAllPlaylistsWithCount()

    fun getPlaylist(playlistId: String): Flow<PlaylistEntity?> = playlistDao.getPlaylistFlow(playlistId)

    suspend fun createPlaylist(name: String): String {
        val id = UUID.randomUUID().toString()
        playlistDao.insertPlaylist(PlaylistEntity(id, name.trim(), System.currentTimeMillis()))
        return id
    }

    suspend fun renamePlaylist(playlistId: String, name: String) {
        playlistDao.renamePlaylist(playlistId, name.trim())
    }

    suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addEpisode(playlistId: String, episodeId: String) {
        playlistDao.addEpisodeToPlaylist(PlaylistEpisodeCrossRef(playlistId, episodeId, System.currentTimeMillis()))
    }

    suspend fun removeEpisode(playlistId: String, episodeId: String) {
        playlistDao.removeEpisodeFromPlaylist(playlistId, episodeId)
    }

    fun getEpisodes(playlistId: String): Flow<List<PlaylistEpisodeItem>> = playlistDao.getEpisodesInPlaylist(playlistId)

    fun getPlaylistIdsForEpisode(episodeId: String): Flow<List<String>> = playlistDao.getPlaylistIdsForEpisode(episodeId)
}
