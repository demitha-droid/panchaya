package org.jethavanarama.dhammasermons.data.model

import java.util.Locale

data class Sermon(
    val id: String,
    val title: String,
    val thero: String,
    val source: SermonSource,
    val audioUrl: String? = null,
    val downloadUrl: String? = null,
    val youtubeUrl: String? = null,
    val youtubeVideoId: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Long = 0L,
    val date: String? = null,
    val category: String? = null,
    val language: Language = Language.SINHALA,
    val localFilePath: String? = null,
    val downloadState: DownloadState = DownloadState.NotDownloaded,
    val lastPlayedPositionMs: Long = 0L,
    val isFavorite: Boolean = false
) {
    val isDownloaded: Boolean
        get() = !localFilePath.isNullOrBlank()

    val playbackUrl: String?
        get() = localFilePath ?: audioUrl

    val durationFormatted: String?
        get() = if (durationSeconds > 0L) {
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            val seconds = durationSeconds % 60
            if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        } else null
}

enum class SermonSource {
    JETHAVANARAMA,
    PAHURA_YOUTUBE
}

enum class Language(val code: String, val displayName: String) {
    SINHALA("sinhala", "සිංහල"),
    ENGLISH("english", "English"),
    HINDI("hindi", "हिन्दी")
}

sealed interface DownloadState {
    data object NotDownloaded : DownloadState
    data class Downloading(val progress: Float) : DownloadState // 0.0 to 1.0
    data class Downloaded(val filePath: String) : DownloadState
    data class Failed(val reason: String, val canResume: Boolean = false) : DownloadState
    data class Paused(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadState
}

data class SermonCategory(
    val id: String,
    val name: String,
    val slug: String,
    val bannerImageUrl: String? = null
)

data class Thero(
    val id: String,
    val name: String
)

data class PaginatedSermons(
    val sermons: List<Sermon> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val hasNextPage: Boolean = false,
    val hasPreviousPage: Boolean = false
)
