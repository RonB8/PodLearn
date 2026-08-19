package com.example.podlingo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.podlingo.data.local.entity.PlaylistEntity
import com.example.podlingo.data.local.entity.PlaylistEpisodeCrossRef
import kotlinx.coroutines.flow.Flow

/** A playlist row joined with how many episodes it currently holds, for the playlists list screen. */
data class PlaylistWithCount(
    val id: String,
    val name: String,
    val createdAtEpochMs: Long,
    val episodeCount: Int,
)

/** An episode within a playlist, joined with its podcast's artwork - [com.example.podlingo.data.local.entity.EpisodeEntity] doesn't carry that. */
data class PlaylistEpisodeItem(
    val id: String,
    val title: String,
    val artworkUrl: String?,
)

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: String, name: String)

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistFlow(playlistId: String): Flow<PlaylistEntity?>

    @Query(
        """
        SELECT playlists.id AS id, playlists.name AS name, playlists.createdAtEpochMs AS createdAtEpochMs,
               COUNT(playlist_episodes.episodeId) AS episodeCount
        FROM playlists
        LEFT JOIN playlist_episodes ON playlist_episodes.playlistId = playlists.id
        GROUP BY playlists.id
        ORDER BY playlists.createdAtEpochMs DESC
        """,
    )
    fun getAllPlaylistsWithCount(): Flow<List<PlaylistWithCount>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addEpisodeToPlaylist(crossRef: PlaylistEpisodeCrossRef)

    @Query("DELETE FROM playlist_episodes WHERE playlistId = :playlistId AND episodeId = :episodeId")
    suspend fun removeEpisodeFromPlaylist(playlistId: String, episodeId: String)

    @Query(
        """
        SELECT episodes.id AS id, episodes.title AS title, podcasts.imageUrl AS artworkUrl
        FROM episodes
        INNER JOIN playlist_episodes ON playlist_episodes.episodeId = episodes.id
        INNER JOIN podcasts ON podcasts.id = episodes.podcastId
        WHERE playlist_episodes.playlistId = :playlistId
        ORDER BY playlist_episodes.addedAtEpochMs DESC
        """,
    )
    fun getEpisodesInPlaylist(playlistId: String): Flow<List<PlaylistEpisodeItem>>

    @Query("SELECT playlistId FROM playlist_episodes WHERE episodeId = :episodeId")
    fun getPlaylistIdsForEpisode(episodeId: String): Flow<List<String>>
}
