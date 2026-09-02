package org.jethavanarama.dhammasermons.notifications

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.jethavanarama.dhammasermons.MainActivity
import org.jethavanarama.dhammasermons.R
import org.jethavanarama.dhammasermons.data.model.Sermon
import org.jethavanarama.dhammasermons.ui.i18n.AppLanguage
import org.jethavanarama.dhammasermons.ui.i18n.getStrings
import org.jethavanarama.dhammasermons.ui.theme.*

class SermonResumeReminderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow display over lockscreen and turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        var sermon = SermonResumeReminderManager.getActiveSermon(this)
        var posMs = SermonResumeReminderManager.getActivePositionMs(this)
        var durMs = SermonResumeReminderManager.getActiveDurationMs(this)

        if (sermon == null) {
            val progressMgr = org.jethavanarama.dhammasermons.player.PlaybackProgressManager(this)
            sermon = progressMgr.getLastPlayedSermon()
            if (sermon != null) {
                posMs = progressMgr.getSavedPosition(sermon.id)
                durMs = progressMgr.getSavedDuration(sermon.id)
            }
        }

        if (sermon == null) {
            sermon = org.jethavanarama.dhammasermons.data.model.Sermon(
                id = intent?.getStringExtra("sermon_id") ?: "active_sermon",
                title = intent?.getStringExtra("sermon_title") ?: "මනසේ ශාන්තිය හා නිවන කරා යන මඟ",
                thero = "පූජ්‍ය පාමංකඩ ආනන්ද ස්වාමීන් වහන්සේ",
                source = org.jethavanarama.dhammasermons.data.model.SermonSource.JETHAVANARAMA,
                audioUrl = "https://www.jethavanarama.org"
            )
            posMs = intent?.getLongExtra("position_ms", 4500000L) ?: 4500000L
            durMs = intent?.getLongExtra("duration_ms", 7200000L) ?: 7200000L
        }

        val settingsPrefs = getSharedPreferences("panchaya_settings", Context.MODE_PRIVATE)
        val langName = settingsPrefs.getString("app_language", AppLanguage.SINHALA.name)
        val appLang = try { AppLanguage.valueOf(langName ?: AppLanguage.SINHALA.name) } catch (e: Exception) { AppLanguage.SINHALA }
        val str = appLang.getStrings()

        val activeSermon = sermon

        setContent {
            DhammaSermonsTheme(themeMode = ThemeMode.DARK_OBSIDIAN) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    SermonResumeReminderContent(
                        sermon = activeSermon,
                        positionMs = posMs,
                        durationMs = durMs,
                        str = str,
                        onResumeClick = {
                            SermonResumeReminderManager.stopAlarm(this@SermonResumeReminderActivity)
                            val intent = Intent(this@SermonResumeReminderActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra(NewSermonNotificationManager.EXTRA_SERMON_ID, activeSermon.id)
                                putExtra(NewSermonNotificationManager.EXTRA_AUTO_PLAY, true)
                            }
                            startActivity(intent)
                            finish()
                        },
                        onSnoozeClick = {
                            SermonResumeReminderManager.snooze(this@SermonResumeReminderActivity, 6)
                            finish()
                        },
                        onStopClick = {
                            SermonResumeReminderManager.stopAlarm(this@SermonResumeReminderActivity)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SermonResumeReminderContent(
    sermon: Sermon,
    positionMs: Long,
    durationMs: Long,
    str: org.jethavanarama.dhammasermons.ui.i18n.AppStrings,
    onResumeClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ChakraRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ChakraAngle"
    )

    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0.5f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BackgroundDark,
                        Color(0xFF161208),
                        BackgroundDark
                    )
                )
            )
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Glowing Dhamma Chakra Header
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(SaffronPrimary.copy(alpha = 0.12f))
                .border(2.dp, SaffronPrimary.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_dhamma_chakra),
                contentDescription = "Dhamma Chakra",
                tint = SaffronPrimary,
                modifier = Modifier
                    .size(68.dp)
                    .rotate(rotationAngle)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = str.resumeReminderTitle,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = SaffronPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = str.resumeReminderSubtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary,
                lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Sermon Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!sermon.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = sermon.thumbnailUrl,
                        contentDescription = sermon.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Text(
                    text = sermon.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = sermon.thero,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = SaffronPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = SaffronPrimary,
                    trackColor = BorderDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${str.listeningProgress}: ${formatDuration(positionMs)}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                    if (durationMs > 0) {
                        Text(
                            text = formatDuration(durationMs),
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons
        // 1. Primary Button: Resume Listening
        Button(
            onClick = onResumeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SaffronPrimary,
                contentColor = Color.Black
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = str.resumeButton,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Secondary Button: Remind in 6 Hours
        OutlinedButton(
            onClick = onSnoozeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = SaffronPrimary
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.6f))
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = str.remindIn6Hours,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Stop Alarm Button
        TextButton(
            onClick = onStopClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.AlarmOff,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = str.stopAlarm,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun formatDuration(millis: Long): String {
    val totalSec = millis / 1000
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
