package org.jethavanarama.dhammasermons.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC, id DESC")
    fun getPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    fun getPlaylistById(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistByIdOnce(id: Long): PlaylistEntity?

    @Query("""
        SELECT s.* FROM sermons s
        INNER JOIN playlist_sermons ps ON s.id = ps.sermonId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.orderIndex ASC
    """)
    fun getSermonsForPlaylist(playlistId: Long): Flow<List<SermonEntity>>

    @Query("""
        SELECT s.* FROM sermons s
        INNER JOIN playlist_sermons ps ON s.id = ps.sermonId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.orderIndex ASC
    """)
    suspend fun getSermonsForPlaylistOnce(playlistId: Long): List<SermonEntity>

    @Query("SELECT playlistId FROM playlist_sermons WHERE sermonId = :sermonId")
    fun getPlaylistsContainingSermon(sermonId: String): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM playlist_sermons WHERE playlistId = :playlistId")
    fun getSermonCountForPlaylist(playlistId: Long): Flow<Int>

    @Query("SELECT MAX(orderIndex) FROM playlist_sermons WHERE playlistId = :playlistId")
    suspend fun getMaxOrderIndex(playlistId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSermonCrossRef(crossRef: PlaylistSermonCrossRef)

    @Query("DELETE FROM playlist_sermons WHERE playlistId = :playlistId AND sermonId = :sermonId")
    suspend fun removeSermonFromPlaylist(playlistId: Long, sermonId: String)

    @Query("UPDATE playlist_sermons SET orderIndex = :newIndex WHERE playlistId = :playlistId AND sermonId = :sermonId")
    suspend fun updateOrderIndex(playlistId: Long, sermonId: String, newIndex: Int)

    @Transaction
    suspend fun reorderSermons(playlistId: Long, orderedSermonIds: List<String>) {
        orderedSermonIds.forEachIndexed { index, sermonId ->
            updateOrderIndex(playlistId, sermonId, index)
        }
        touchPlaylistUpdated(playlistId, System.currentTimeMillis())
    }

    @Query("UPDATE playlists SET updatedAt = :timestamp WHERE id = :playlistId")
    suspend fun touchPlaylistUpdated(playlistId: Long, timestamp: Long = System.currentTimeMillis())
}
