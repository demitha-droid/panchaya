package org.jethavanarama.dhammasermons.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jethavanarama.dhammasermons.data.db.PlaylistEntity
import org.jethavanarama.dhammasermons.data.db.getDisplayName
import org.jethavanarama.dhammasermons.data.db.getDisplayDescription
import org.jethavanarama.dhammasermons.ui.i18n.strings
import org.jethavanarama.dhammasermons.ui.theme.*

val PLAYLIST_ICONS = listOf(
    "dhamma_wheel" to "☸",
    "lotus" to "🪷",
    "stupa" to "🛕",
    "monk" to "🧘",
    "mala" to "📿"
)

val PLAYLIST_COLORS = listOf(
    "#D97706", // Saffron
    "#F59E0B", // Gold
    "#B45309", // Terracotta
    "#059669", // Jade Emerald
    "#DC2626", // Lotus Red
    "#8B5CF6"  // Lavender Purple
)

fun parseHexColor(hex: String, fallback: Color = Color(0xFFD97706)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlaylistSheet(
    playlist: PlaylistEntity? = null, // null means creating a new playlist
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, iconName: String, colorAccentHex: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val str = strings
    var name by remember(playlist, str) { mutableStateOf(playlist?.getDisplayName(str).orEmpty()) }
    var description by remember(playlist, str) { mutableStateOf(playlist?.getDisplayDescription(str).orEmpty()) }
    var selectedIcon by remember(playlist) { mutableStateOf(playlist?.iconName ?: "dhamma_wheel") }
    var selectedColor by remember(playlist) { mutableStateOf(playlist?.colorAccentHex ?: "#D97706") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BorderDark)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (playlist == null) str.newPlaylist else str.editPlaylist,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextMuted
                    )
                }
            }

            // Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(str.playlistName) },
                placeholder = { Text(str.playlistNamePlaceholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = parseHexColor(selectedColor),
                    unfocusedBorderColor = BorderDark,
                    focusedLabelColor = parseHexColor(selectedColor),
                    unfocusedLabelColor = TextMuted,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(str.playlistDescriptionLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = parseHexColor(selectedColor),
                    unfocusedBorderColor = BorderDark,
                    focusedLabelColor = parseHexColor(selectedColor),
                    unfocusedLabelColor = TextMuted,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Icon Picker
            Text(
                text = str.coverIconLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(PLAYLIST_ICONS) { (iconKey, emoji) ->
                    val isSelected = selectedIcon == iconKey
                    val accentColor = parseHexColor(selectedColor)
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else SurfaceDark)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) accentColor else BorderDark,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedIcon = iconKey },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 22.sp)
                    }
                }
            }

            // Accent Color Picker
            Text(
                text = str.themeAccentLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(PLAYLIST_COLORS) { colorHex ->
                    val color = parseHexColor(colorHex)
                    val isSelected = selectedColor.equals(colorHex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = colorHex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (playlist != null && onDelete != null) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = str.deletePlaylist,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(str.delete)
                    }
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(name, description, selectedIcon, selectedColor)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(if (playlist != null && onDelete != null) 1.5f else 1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = parseHexColor(selectedColor),
                        contentColor = Color.White
                    ),
                    enabled = name.isNotBlank()
                ) {
                    Text(
                        text = str.save,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(str.deletePlaylist, color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(str.deletePlaylistConfirm, color = TextSecondary) },
            containerColor = SurfaceCard,
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text(str.delete, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(str.cancel, color = TextMuted)
                }
            }
        )
    }
}
