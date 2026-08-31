package org.jethavanarama.dhammasermons.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SermonDao {

    @Query("SELECT * FROM sermons WHERE localFilePath IS NOT NULL AND localFilePath != '' ORDER BY downloadedAtTimestamp DESC")
    fun getDownloadedSermons(): Flow<List<SermonEntity>>

    @Query("SELECT * FROM sermons WHERE isFavorite = 1 ORDER BY date DESC")
    fun getFavoriteSermons(): Flow<List<SermonEntity>>

    @Query("SELECT * FROM sermons WHERE lastPlayedPositionMs > 0 ORDER BY date DESC LIMIT 20")
    fun getRecentlyPlayedSermons(): Flow<List<SermonEntity>>

    @Query("SELECT * FROM sermons WHERE id = :id LIMIT 1")
    suspend fun getSermonById(id: String): SermonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(sermon: SermonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sermons: List<SermonEntity>)

    @Query("UPDATE sermons SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE sermons SET lastPlayedPositionMs = :positionMs WHERE id = :id")
    suspend fun updatePlaybackPosition(id: String, positionMs: Long)

    @Query("UPDATE sermons SET localFilePath = :filePath, downloadedAtTimestamp = :timestamp WHERE id = :id")
    suspend fun markAsDownloaded(id: String, filePath: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE sermons SET localFilePath = NULL, downloadedAtTimestamp = NULL WHERE id = :id")
    suspend fun removeDownload(id: String)

    @Query("DELETE FROM sermons WHERE id = :id")
    suspend fun deleteById(id: String)
}
