package org.jethavanarama.dhammasermons.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jethavanarama.dhammasermons.data.extractor.JethavanaramaExtractor
import org.jethavanarama.dhammasermons.data.extractor.PahuraYouTubeExtractor
import org.jethavanarama.dhammasermons.data.model.Sermon

class NewSermonCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val jethavanaramaExtractor = JethavanaramaExtractor()
    private val pahuraExtractor = PahuraYouTubeExtractor()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch latest sermons from both Jethavanarama Monastery & Pahura Channel
            val jethLatest = try {
                jethavanaramaExtractor.fetchLatestSermons()
            } catch (e: Exception) {
                emptyList<Sermon>()
            }

            val pahuraLatest = try {
                pahuraExtractor.fetchChannelSermons(page = 1, forceRefresh = true).sermons
            } catch (e: Exception) {
                emptyList<Sermon>()
            }

            val combinedLatest = (jethLatest + pahuraLatest).distinctBy { it.id }
            if (combinedLatest.isEmpty()) {
                return@withContext Result.retry()
            }

            val seenIds = NewSermonNotificationManager.getSeenSermonIds(applicationContext)
            val isInitial = NewSermonNotificationManager.isInitialRun(applicationContext)

            if (isInitial) {
                // First run: Record existing sermons as seen baseline
                val initialIds = combinedLatest.map { it.id }.toSet()
                NewSermonNotificationManager.saveSeenSermonIds(applicationContext, initialIds)
                return@withContext Result.success()
            }

            // Detect new sermons
            val newSermons = combinedLatest.filter { !seenIds.contains(it.id) }
            if (newSermons.isNotEmpty()) {
                // Show notification for new sermons (up to 3 newest)
                newSermons.take(3).forEach { sermon ->
                    NewSermonNotificationManager.showNewSermonNotification(applicationContext, sermon)
                }

                // Update seen set with all current IDs
                val updatedIds = (seenIds + combinedLatest.map { it.id }).toSet()
                NewSermonNotificationManager.saveSeenSermonIds(applicationContext, updatedIds)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
