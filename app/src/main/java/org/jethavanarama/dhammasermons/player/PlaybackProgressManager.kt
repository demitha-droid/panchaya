package org.jethavanarama.dhammasermons.player

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jethavanarama.dhammasermons.data.db.AppDatabase
import org.jethavanarama.dhammasermons.data.db.SermonEntity
import org.jethavanarama.dhammasermons.data.model.Sermon

/**
 * Manages persistent playback progress across app lifecycles, background kills, and device restarts.
 * Provides instant synchronous reads/writes via SharedPreferences with async persistence to Room DB.
 */
class PlaybackProgressManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("dhamma_playback_progress", Context.MODE_PRIVATE)
    private val db = AppDatabase.getDatabase(context)
    private val sermonDao = db.sermonDao()
    private val gson = Gson()
    private val ioScope = CoroutineScope(Dispatchers.IO)

    /**
     * Saves playback position in SharedPreferences immediately and updates Room DB.
     */
    fun saveProgress(sermon: Sermon, positionMs: Long, durationMs: Long) {
        if (sermon.id.isBlank()) return

        // If completed or within last 5 seconds (or >98%), reset progress to 0 so next play starts from beginning
        val isNearEnd = (durationMs > 0 && positionMs >= durationMs - 5000L) || (durationMs > 10000L && positionMs >= durationMs * 0.98f)
        val posToSave = if (isNearEnd) 0L else positionMs

        prefs.edit()
            .putLong("pos_${sermon.id}", posToSave)
            .putLong("dur_${sermon.id}", durationMs)
            .putLong("time_${sermon.id}", System.currentTimeMillis())
            .putString("last_played_sermon_json", gson.toJson(sermon.copy(lastPlayedPositionMs = posToSave)))
            .putString("last_played_sermon_id", sermon.id)
            .apply()

        // Asynchronously synchronize with Room SQLite DB
        ioScope.launch {
            try {
                val existing = sermonDao.getSermonById(sermon.id)
                if (existing != null) {
                    sermonDao.updatePlaybackPosition(sermon.id, posToSave)
                } else {
                    sermonDao.insertOrUpdate(SermonEntity.fromDomain(sermon.copy(lastPlayedPositionMs = posToSave)))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Returns the saved resume position in milliseconds for a given sermon ID.
     * Returns 0L if not found or if the sermon was completed.
     */
    fun getSavedPosition(sermonId: String): Long {
        if (sermonId.isBlank()) return 0L
        return prefs.getLong("pos_$sermonId", 0L)
    }

    /**
     * Returns the saved duration in milliseconds for a given sermon ID.
     */
    fun getSavedDuration(sermonId: String): Long {
        if (sermonId.isBlank()) return 0L
        return prefs.getLong("dur_$sermonId", 0L)
    }

    /**
     * Clears progress when a sermon is finished.
     */
    fun clearProgress(sermonId: String) {
        if (sermonId.isBlank()) return
        prefs.edit()
            .putLong("pos_$sermonId", 0L)
            .apply()

        ioScope.launch {
            try {
                sermonDao.updatePlaybackPosition(sermonId, 0L)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Restores the last played sermon and its saved position after app restart.
     */
    fun getLastPlayedSermon(): Sermon? {
        val json = prefs.getString("last_played_sermon_json", null) ?: return null
        return try {
            val sermon = gson.fromJson(json, Sermon::class.java)
            if (sermon != null) {
                val pos = getSavedPosition(sermon.id)
                sermon.copy(lastPlayedPositionMs = pos)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
