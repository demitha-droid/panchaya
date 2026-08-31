package org.jethavanarama.dhammasermons.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jethavanarama.dhammasermons.data.model.DownloadState
import org.jethavanarama.dhammasermons.data.model.Language
import org.jethavanarama.dhammasermons.data.model.Sermon
import org.jethavanarama.dhammasermons.data.model.SermonSource

@Entity(tableName = "sermons")
data class SermonEntity(
    @PrimaryKey val id: String,
    val title: String,
    val thero: String,
    val source: String, // "JETHAVANARAMA" or "PAHURA_YOUTUBE"
    val audioUrl: String?,
    val downloadUrl: String?,
    val youtubeVideoId: String?,
    val youtubeUrl: String?,
    val thumbnailUrl: String?,
    val date: String?,
    val category: String?,
    val language: String,
    val durationSeconds: Long,
    val localFilePath: String?,
    val isFavorite: Boolean,
    val lastPlayedPositionMs: Long,
    val downloadedAtTimestamp: Long? = null
) {
    fun toDomain(): Sermon {
        return Sermon(
            id = id,
            title = title,
            thero = thero,
            source = if (source == SermonSource.PAHURA_YOUTUBE.name) SermonSource.PAHURA_YOUTUBE else SermonSource.JETHAVANARAMA,
            audioUrl = audioUrl,
            downloadUrl = downloadUrl,
            youtubeVideoId = youtubeVideoId,
            youtubeUrl = youtubeUrl,
            thumbnailUrl = thumbnailUrl,
            date = date,
            category = category,
            language = Language.entries.find { it.code == language } ?: Language.SINHALA,
            durationSeconds = durationSeconds,
            localFilePath = localFilePath,
            isFavorite = isFavorite,
            lastPlayedPositionMs = lastPlayedPositionMs,
            downloadState = if (!localFilePath.isNullOrBlank()) DownloadState.Downloaded(localFilePath) else DownloadState.NotDownloaded
        )
    }

    companion object {
        fun fromDomain(sermon: Sermon, downloadedAt: Long? = null): SermonEntity {
            return SermonEntity(
                id = sermon.id,
                title = sermon.title,
                thero = sermon.thero,
                source = sermon.source.name,
                audioUrl = sermon.audioUrl,
                downloadUrl = sermon.downloadUrl,
                youtubeVideoId = sermon.youtubeVideoId,
                youtubeUrl = sermon.youtubeUrl,
                thumbnailUrl = sermon.thumbnailUrl,
                date = sermon.date,
                category = sermon.category,
                language = sermon.language.code,
                durationSeconds = sermon.durationSeconds,
                localFilePath = sermon.localFilePath,
                isFavorite = sermon.isFavorite,
                lastPlayedPositionMs = sermon.lastPlayedPositionMs,
                downloadedAtTimestamp = downloadedAt
            )
        }
    }
}
