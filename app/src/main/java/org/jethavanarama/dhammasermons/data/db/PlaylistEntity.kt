package org.jethavanarama.dhammasermons.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val iconName: String = "dhamma_wheel", // "dhamma_wheel", "lotus", "stupa", "monk", "meditation"
    val colorAccentHex: String = "#D97706",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_sermons",
    primaryKeys = ["playlistId", "sermonId"],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["sermonId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistSermonCrossRef(
    val playlistId: Long,
    val sermonId: String,
    val orderIndex: Int,
    val addedAt: Long = System.currentTimeMillis()
)

fun PlaylistEntity.getDisplayName(str: org.jethavanarama.dhammasermons.ui.i18n.AppStrings): String {
    return when (this.name.trim()) {
        "භාවනා වැඩසටහන්", "Meditation Programs" -> str.defaultPlaylistMeditation
        "මහා දේශනා", "Great Sermons" -> str.defaultPlaylistGreatSermons
        "දිනපතා ශ්‍රවණයට", "Daily Listening" -> str.defaultPlaylistDailyListening
        else -> this.name
    }
}

fun PlaylistEntity.getDisplayDescription(str: org.jethavanarama.dhammasermons.ui.i18n.AppStrings): String {
    return when (this.name.trim()) {
        "භාවනා වැඩසටහන්", "Meditation Programs" -> str.defaultPlaylistMeditationDesc
        "මහා දේශනා", "Great Sermons" -> str.defaultPlaylistGreatSermonsDesc
        "දිනපතා ශ්‍රවණයට", "Daily Listening" -> str.defaultPlaylistDailyListeningDesc
        else -> this.description
    }
}

