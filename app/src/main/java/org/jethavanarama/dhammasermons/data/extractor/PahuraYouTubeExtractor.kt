package org.jethavanarama.dhammasermons.data.extractor

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jethavanarama.dhammasermons.data.model.Language
import org.jethavanarama.dhammasermons.data.model.PaginatedSermons
import org.jethavanarama.dhammasermons.data.model.Sermon
import org.jethavanarama.dhammasermons.data.model.SermonSource
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed interface AudioExtractionResult {
    data class Success(
        val audioUrl: String,
        val format: String,
        val bitrate: Int,
        val durationSeconds: Long = 0L
    ) : AudioExtractionResult

    data class Error(val message: String) : AudioExtractionResult
}

class PahuraYouTubeExtractor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    private val channelId = "UC2w5Et5n0nfhHHGUrmogs2A"
    private val innertubeApiKey = "AIzaSyAO_g8Bc99g5kzsqDVLeNd2Qp7AsGTQ64"
    private val browseUrl = "https://www.youtube.com/youtubei/v1/browse?key=$innertubeApiKey"

    private val streamCache = ConcurrentHashMap<String, AudioExtractionResult.Success>()
    
    // Invidious instances cache
    @Volatile
    private var lastInstancesFetchTime = 0L
    private val dynamicInstances = mutableListOf<String>()
    private val staticFallbackInstances = listOf(
        "https://yt.chocolatemoo53.com",
        "https://inv.nadeko.net",
        "https://invidious.nerdvpn.de",
        "https://invidious.tiekoetter.com"
    )
    
    // Videos pagination
    private val pageTokens = ConcurrentHashMap<Int, String>()
    private val pageCache = ConcurrentHashMap<Int, PaginatedSermons>()

    // Shorts pagination
    private val shortsTokens = ConcurrentHashMap<Int, String>()
    private val shortsCache = ConcurrentHashMap<Int, PaginatedSermons>()

    fun clearCache() {
        pageCache.clear()
        pageTokens.clear()
        shortsCache.clear()
        shortsTokens.clear()
    }

    suspend fun fetchChannelSermons(page: Int = 1, forceRefresh: Boolean = false): PaginatedSermons = withContext(Dispatchers.IO) {
        if (forceRefresh && page == 1) {
            pageCache.clear()
            pageTokens.clear()
        } else if (!forceRefresh && pageCache.containsKey(page)) {
            return@withContext pageCache[page]!!
        }

        try {
            val jsonPayload = if (page == 1) {
                """{"context":{"client":{"clientName":"WEB","clientVersion":"2.20240801.01.00","hl":"en","gl":"US"}},"browseId":"$channelId","params":"EgZ2aWRlb3PyBgQKAjoA"}"""
            } else {
                val token = pageTokens[page]
                if (token != null) {
                    """{"context":{"client":{"clientName":"WEB","clientVersion":"2.20240801.01.00","hl":"en","gl":"US"}},"continuation":"$token"}"""
                } else {
                    return@withContext PaginatedSermons(emptyList(), page, page, false, page > 1)
                }
            }

            val request = Request.Builder()
                .url(browseUrl)
                .post(jsonPayload.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .header("X-YouTube-Client-Name", "1")
                .header("X-YouTube-Client-Version", "2.20240801.01.00")
                .header("Origin", "https://www.youtube.com")
                .build()

            val response = client.newCall(request).execute()
            val responseStr = response.body?.string().orEmpty()
            val result = parseBrowseResponse(responseStr, page, pageTokens)
            pageCache[page] = result
            result
        } catch (e: Exception) {
            e.printStackTrace()
            PaginatedSermons(emptyList(), page, page, false, page > 1)
        }
    }

    suspend fun fetchChannelShorts(page: Int = 1, forceRefresh: Boolean = false): PaginatedSermons = withContext(Dispatchers.IO) {
        if (forceRefresh && page == 1) {
            shortsCache.clear()
            shortsTokens.clear()
        } else if (!forceRefresh && shortsCache.containsKey(page)) {
            return@withContext shortsCache[page]!!
        }

        try {
            val jsonPayload = if (page == 1) {
                """{"context":{"client":{"clientName":"WEB","clientVersion":"2.20240801.01.00","hl":"en","gl":"US"}},"browseId":"$channelId","params":"EgZzaG9ydHPyBgUKA5oBAA%3D%3D"}"""
            } else {
                val token = shortsTokens[page]
                if (token != null) {
                    """{"context":{"client":{"clientName":"WEB","clientVersion":"2.20240801.01.00","hl":"en","gl":"US"}},"continuation":"$token"}"""
                } else {
                    return@withContext PaginatedSermons(emptyList(), page, page, false, page > 1)
                }
            }

            val request = Request.Builder()
                .url(browseUrl)
                .post(jsonPayload.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .header("X-YouTube-Client-Name", "1")
                .header("X-YouTube-Client-Version", "2.20240801.01.00")
                .header("Origin", "https://www.youtube.com")
                .build()

            val response = client.newCall(request).execute()
            val responseStr = response.body?.string().orEmpty()
            val result = parseBrowseResponse(responseStr, page, shortsTokens)
            shortsCache[page] = result
            result
        } catch (e: Exception) {
            e.printStackTrace()
            PaginatedSermons(emptyList(), page, page, false, page > 1)
        }
    }

    suspend fun fetchLatestSermons(): List<Sermon> {
        return fetchChannelSermons(1).sermons
    }

    private fun parseBrowseResponse(
        jsonStr: String,
        page: Int,
        tokenMap: ConcurrentHashMap<Int, String>
    ): PaginatedSermons {
        val sermons = mutableListOf<Sermon>()
        var nextContinuationToken: String? = null

        try {
            val json = JsonParser.parseString(jsonStr).asJsonObject

            if (page == 1) {
                val twoCol = json.getAsJsonObject("contents")?.getAsJsonObject("twoColumnBrowseResultsRenderer")
                val tabs = twoCol?.getAsJsonArray("tabs")
                var gridContents: com.google.gson.JsonArray? = null

                if (tabs != null) {
                    for (tabElement in tabs) {
                        val tabObj = tabElement.asJsonObject.getAsJsonObject("tabRenderer")
                        if (tabObj?.get("selected")?.asBoolean == true) {
                            gridContents = tabObj.getAsJsonObject("content")
                                ?.getAsJsonObject("richGridRenderer")
                                ?.getAsJsonArray("contents")
                            break
                        }
                    }
                }

                if (gridContents != null) {
                    for (itemElement in gridContents) {
                        parseGridItem(itemElement.asJsonObject, sermons)?.let { nextContinuationToken = it }
                    }
                }
            } else {
                val actions = json.getAsJsonArray("onResponseReceivedActions")
                if (actions != null && actions.size() > 0) {
                    val appendAction = actions[0].asJsonObject.getAsJsonObject("appendContinuationItemsAction")
                    val items = appendAction?.getAsJsonArray("continuationItems")
                    if (items != null) {
                        for (itemElement in items) {
                            parseGridItem(itemElement.asJsonObject, sermons)?.let { nextContinuationToken = it }
                        }
                    }
                }
            }

            if (!nextContinuationToken.isNullOrBlank()) {
                tokenMap[page + 1] = nextContinuationToken!!
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        val hasNext = !nextContinuationToken.isNullOrBlank() || tokenMap.containsKey(page + 1)
        val hasPrev = page > 1

        return PaginatedSermons(
            sermons = sermons,
            currentPage = page,
            totalPages = if (hasNext) page + 1 else page,
            hasNextPage = hasNext,
            hasPreviousPage = hasPrev
        )
    }

    private fun parseGridItem(item: JsonObject, outList: MutableList<Sermon>): String? {
        var continuationToken: String? = null

        val richItem = item.getAsJsonObject("richItemRenderer")?.getAsJsonObject("content")

        // 1. Regular video lockup
        val lockup = richItem?.getAsJsonObject("lockupViewModel")
        if (lockup != null) {
            val videoId = lockup.get("contentId")?.asString.orEmpty()
            val title = lockup.getAsJsonObject("metadata")
                ?.getAsJsonObject("lockupMetadataViewModel")
                ?.getAsJsonObject("title")
                ?.get("content")?.asString.orEmpty()

            val accessibilityLabel = lockup.getAsJsonObject("rendererContext")
                ?.getAsJsonObject("accessibilityContext")
                ?.get("label")?.asString.orEmpty()

            val durSec = parseDurationSeconds(accessibilityLabel)

            var timeAgo: String? = null
            val rows = lockup.getAsJsonObject("metadata")
                ?.getAsJsonObject("lockupMetadataViewModel")
                ?.getAsJsonObject("metadata")
                ?.getAsJsonObject("contentMetadataViewModel")
                ?.getAsJsonArray("metadataRows")
            if (rows != null && rows.size() > 0) {
                val parts = rows[0].asJsonObject.getAsJsonArray("metadataParts")
                if (parts != null && parts.size() > 1) {
                    timeAgo = parts[1].asJsonObject.getAsJsonObject("text")?.get("content")?.asString
                }
            }

            if (videoId.isNotBlank() && title.isNotBlank()) {
                outList.add(createSermon(videoId, title, durSec, timeAgo))
            }
        }

        // 2. Shorts lockup
        val shortsLockup = richItem?.getAsJsonObject("shortsLockupViewModel")
        if (shortsLockup != null) {
            val entityId = shortsLockup.get("entityId")?.asString.orEmpty()
            val videoId = entityId.removePrefix("shorts-shelf-item-")
            val title = shortsLockup.getAsJsonObject("overlayMetadata")
                ?.getAsJsonObject("primaryText")
                ?.get("content")?.asString.orEmpty()

            if (videoId.isNotBlank() && title.isNotBlank()) {
                outList.add(createSermon(videoId, title, 0L, null))
            }
        }

        // 3. Reel Item Renderer
        val reelItem = richItem?.getAsJsonObject("reelItemRenderer")
        if (reelItem != null) {
            val videoId = reelItem.get("videoId")?.asString.orEmpty()
            val title = reelItem.getAsJsonObject("headline")?.get("simpleText")?.asString.orEmpty()
            if (videoId.isNotBlank() && title.isNotBlank()) {
                outList.add(createSermon(videoId, title, 0L, null))
            }
        }

        // 4. Video Renderer
        val videoRenderer = richItem?.getAsJsonObject("videoRenderer")
        if (videoRenderer != null) {
            val videoId = videoRenderer.get("videoId")?.asString.orEmpty()
            val titleRuns = videoRenderer.getAsJsonObject("title")?.getAsJsonArray("runs")
            val title = if (titleRuns != null && titleRuns.size() > 0) {
                titleRuns[0].asJsonObject.get("text")?.asString.orEmpty()
            } else ""

            val lengthSec = videoRenderer.get("lengthSeconds")?.asLong ?: 0L
            val publishedTime = videoRenderer.getAsJsonObject("publishedTimeText")?.get("simpleText")?.asString

            if (videoId.isNotBlank() && title.isNotBlank()) {
                outList.add(createSermon(videoId, title, lengthSec, publishedTime))
            }
        }

        // 5. Continuation token
        val continuationItem = item.getAsJsonObject("continuationItemRenderer")
        if (continuationItem != null) {
            continuationToken = continuationItem.getAsJsonObject("continuationEndpoint")
                ?.getAsJsonObject("continuationCommand")
                ?.get("token")?.asString
        }

        return continuationToken
    }

    private fun parseDurationSeconds(label: String): Long {
        if (label.isBlank()) return 0L
        var totalSec = 0L

        val hMatch = """(\d+)\s+hours?""".toRegex(RegexOption.IGNORE_CASE).find(label)
        if (hMatch != null) {
            totalSec += (hMatch.groupValues[1].toLongOrNull() ?: 0L) * 3600L
        }

        val mMatch = """(\d+)\s+minutes?""".toRegex(RegexOption.IGNORE_CASE).find(label)
        if (mMatch != null) {
            totalSec += (mMatch.groupValues[1].toLongOrNull() ?: 0L) * 60L
        }

        val sMatch = """(\d+)\s+seconds?""".toRegex(RegexOption.IGNORE_CASE).find(label)
        if (sMatch != null) {
            totalSec += (sMatch.groupValues[1].toLongOrNull() ?: 0L)
        }

        return totalSec
    }

    private fun createSermon(videoId: String, rawTitle: String, durationSec: Long = 0L, dateText: String? = null): Sermon {
        val cleanTitle = cleanSermonTitle(rawTitle)
        val theroName = extractTheroName(rawTitle)
        val thumb = "https://i3.ytimg.com/vi/$videoId/hqdefault.jpg"

        return Sermon(
            id = "yt_$videoId",
            title = cleanTitle,
            thero = theroName,
            source = SermonSource.PAHURA_YOUTUBE,
            youtubeVideoId = videoId,
            youtubeUrl = "https://www.youtube.com/watch?v=$videoId",
            thumbnailUrl = thumb,
            durationSeconds = durationSec,
            date = dateText,
            language = Language.SINHALA
        )
    }

    /**
     * Resolves high-quality audio stream for any YouTube video/short:
     * 1. Check in-memory cache
     * 2. High-performance Invidious audio streams & /latest_version redirects
     * 3. NewPipeExtractor with automatic initialization
     * 4. Multi-node fast converter cluster
     */
    suspend fun extractAudioStreamUrl(videoId: String): AudioExtractionResult = withContext(Dispatchers.IO) {
        streamCache[videoId]?.let { return@withContext it }

        // Strategy 1: Invidious Direct Audio Stream & Proxy Resolution (Fastest & most up-to-date)
        try {
            val result = extractFromInvidious(videoId)
            if (result is AudioExtractionResult.Success) {
                streamCache[videoId] = result
                return@withContext result
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Strategy 2: NewPipeExtractor
        try {
            val result = extractWithNewPipe(videoId)
            if (result is AudioExtractionResult.Success) {
                streamCache[videoId] = result
                return@withContext result
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Strategy 3: Fast Multi-Node SaveNow / Loader conversion
        try {
            val result = extractFromLoaderAndSaveNow(videoId)
            if (result is AudioExtractionResult.Success) {
                streamCache[videoId] = result
                return@withContext result
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        AudioExtractionResult.Error("Could not resolve MP3 audio stream for this video.")
    }

    private fun getHealthyInvidiousInstances(): List<String> {
        val now = System.currentTimeMillis()
        synchronized(dynamicInstances) {
            if (dynamicInstances.isNotEmpty() && (now - lastInstancesFetchTime < 3600_000L)) {
                return dynamicInstances.toList()
            }
        }

        val discovered = mutableListOf<String>()
        try {
            val req = Request.Builder()
                .url("https://api.invidious.io/instances.json?sort_by=health")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string().orEmpty()
            if (body.isNotBlank()) {
                val array = JsonParser.parseString(body).asJsonArray
                for (elem in array) {
                    val item = elem.asJsonArray
                    if (item.size() >= 2) {
                        val domain = item[0].asString
                        val info = item[1].asJsonObject
                        val type = info.get("type")?.asString
                        val uri = info.get("uri")?.asString
                        val apiEnabled = info.get("api")?.asBoolean ?: false
                        if (type == "https" && !uri.isNullOrBlank() && !domain.endsWith(".onion") && !domain.endsWith(".i2p")) {
                            val cleanUri = uri.removeSuffix("/")
                            if (apiEnabled) {
                                discovered.add(0, cleanUri)
                            } else {
                                discovered.add(cleanUri)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore failure, fall back to known instances
        }

        val combined = LinkedHashSet<String>()
        combined.add("https://yt.chocolatemoo53.com")
        combined.addAll(discovered)
        combined.addAll(staticFallbackInstances)

        synchronized(dynamicInstances) {
            dynamicInstances.clear()
            dynamicInstances.addAll(combined)
            lastInstancesFetchTime = now
            return dynamicInstances.toList()
        }
    }

    private fun extractFromInvidious(videoId: String): AudioExtractionResult {
        val instances = getHealthyInvidiousInstances()

        for (instance in instances) {
            // Sub-strategy A: Direct audio stream redirect via /latest_version (Fastest & direct)
            for (itag in listOf(140, 251)) {
                try {
                    val streamUrl = "$instance/latest_version?id=$videoId&itag=$itag"
                    val testReq = Request.Builder()
                        .url(streamUrl)
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Range", "bytes=0-1024")
                        .build()
                    val testResp = client.newCall(testReq).execute()
                    val code = testResp.code
                    val contentType = testResp.header("Content-Type").orEmpty()
                    val finalUrl = testResp.request.url.toString()
                    testResp.close()

                    if ((code == 200 || code == 206) &&
                        (contentType.startsWith("audio/") || contentType.startsWith("video/") || finalUrl.contains("videoplayback"))
                    ) {
                        return AudioExtractionResult.Success(
                            audioUrl = finalUrl,
                            format = if (itag == 140) "m4a" else "opus",
                            bitrate = if (itag == 140) 128 else 160
                        )
                    }
                } catch (e: Exception) {
                    // Try next
                }
            }

            // Sub-strategy B: /api/v1/videos/$videoId JSON inspection
            try {
                val request = Request.Builder()
                    .url("$instance/api/v1/videos/$videoId")
                    .header("User-Agent", "Mozilla/5.0")
                    .build()

                val response = client.newCall(request).execute()
                val resBody = response.body?.string().orEmpty()
                if (resBody.isBlank()) continue
                val json = JsonParser.parseString(resBody).asJsonObject

                val adaptiveFormats = json.getAsJsonArray("adaptiveFormats")
                if (adaptiveFormats != null) {
                    var bestAudioUrl: String? = null
                    var bestBitrate = 0
                    var bestFormat = "m4a"

                    for (f in adaptiveFormats) {
                        val obj = f.asJsonObject
                        val type = obj.get("type")?.asString.orEmpty()
                        if (type.startsWith("audio/")) {
                            val bitrate = obj.get("bitrate")?.asInt ?: 0
                            val url = obj.get("url")?.asString.orEmpty()
                            if (url.isNotBlank() && bitrate > bestBitrate) {
                                bestBitrate = bitrate
                                bestAudioUrl = url
                                bestFormat = if (type.contains("mp4") || type.contains("m4a")) "m4a" else "opus"
                            }
                        }
                    }

                    if (bestAudioUrl != null) {
                        try {
                            val probeReq = Request.Builder()
                                .url(bestAudioUrl)
                                .header("User-Agent", "Mozilla/5.0")
                                .header("Range", "bytes=0-512")
                                .build()
                            val probeResp = client.newCall(probeReq).execute()
                            val pCode = probeResp.code
                            probeResp.close()
                            if (pCode == 200 || pCode == 206) {
                                return AudioExtractionResult.Success(
                                    audioUrl = bestAudioUrl,
                                    format = bestFormat,
                                    bitrate = bestBitrate / 1000
                                )
                            }
                        } catch (e: Exception) {
                            // Direct url probe failed, continue
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue to next instance
            }
        }
        return AudioExtractionResult.Error("Invidious unavailable")
    }

    private fun extractFromLoaderAndSaveNow(videoId: String): AudioExtractionResult {
        val videoUrl = "https://www.youtube.com/watch?v=$videoId"
        val initUrls = listOf(
            "https://loader.to/ajax/download.php?button=1&start=1&end=1&format=mp3&url=$videoUrl",
            "https://p.savenow.to/ajax/download.php?button=1&start=1&end=1&format=mp3&url=$videoUrl",
            "https://lto2.affadaffa.com/ajax/download.php?button=1&start=1&end=1&format=mp3&url=$videoUrl"
        )

        for (initUrl in initUrls) {
            try {
                val req = Request.Builder()
                    .url(initUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                    .header("Accept", "application/json")
                    .build()

                val resp = client.newCall(req).execute()
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) continue

                val json = JsonParser.parseString(body).asJsonObject
                val directUrl = json.get("download_url")?.asString

                if (!directUrl.isNullOrBlank()) {
                    return AudioExtractionResult.Success(
                        audioUrl = directUrl,
                        format = "mp3",
                        bitrate = 128
                    )
                }

                val isSuccess = json.get("success")?.asBoolean ?: false
                val id = json.get("id")?.asString

                if (isSuccess && !id.isNullOrBlank()) {
                    val progressUrl = json.get("progress_url")?.asString
                        ?: "https://lto2.affadaffa.com/api/progress?id=$id"

                    // Poll progress endpoint (quick max 4 attempts to avoid blocking UI)
                    for (i in 1..4) {
                        Thread.sleep(800)
                        try {
                            val pollReq = Request.Builder()
                                .url(progressUrl)
                                .header("User-Agent", "Mozilla/5.0")
                                .header("Accept", "application/json")
                                .build()
                            val pollResp = client.newCall(pollReq).execute()
                            val pollBody = pollResp.body?.string().orEmpty()
                            if (pollBody.isNotBlank()) {
                                val pollJson = JsonParser.parseString(pollBody).asJsonObject
                                val dlUrl = pollJson.get("download_url")?.asString
                                if (!dlUrl.isNullOrBlank()) {
                                    return AudioExtractionResult.Success(
                                        audioUrl = dlUrl,
                                        format = "mp3",
                                        bitrate = 128
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            // Continue polling
                        }
                    }
                }
            } catch (e: Exception) {
                // Try next endpoint
            }
        }
        return AudioExtractionResult.Error("SaveNow/Loader unavailable")
    }

    private fun extractWithNewPipe(videoId: String): AudioExtractionResult {
        return try {
            try {
                if (NewPipe.getDownloader() == null) {
                    NewPipe.init(NewPipeDownloaderImpl.init())
                }
            } catch (e: Exception) {
                // Ignore init error
            }
            val service = ServiceList.YouTube
            val extractor = service.getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
            extractor.fetchPage()

            val audioStreams = extractor.audioStreams
            if (!audioStreams.isNullOrEmpty()) {
                val bestAudio = audioStreams.maxByOrNull { it.averageBitrate } ?: audioStreams.first()
                val audioUrl = bestAudio.url.orEmpty()
                if (audioUrl.isNotBlank()) {
                    AudioExtractionResult.Success(
                        audioUrl = audioUrl,
                        format = bestAudio.format?.name ?: "mp3",
                        bitrate = bestAudio.averageBitrate
                    )
                } else {
                    AudioExtractionResult.Error("Audio stream URL was empty")
                }
            } else {
                AudioExtractionResult.Error("No audio streams in NewPipe")
            }
        } catch (e: Exception) {
            AudioExtractionResult.Error("NewPipe extraction failed: ${e.message}")
        }
    }

    private fun cleanSermonTitle(title: String): String {
        return title
            .replace("#[a-zA-Z0-9._]+".toRegex(), "")
            .replace("(?i)ven\\.[a-zA-Z\\s]+thero".toRegex(), "")
            .replace("(?i)dharmayai obai".toRegex(), "")
            .replace("(?i)jethavanarama viharaya".toRegex(), "")
            .replace("(?i)pahura".toRegex(), "")
            .replace("\\|.*".toRegex(), "")
            .replace("-.*".toRegex(), "")
            .trim()
            .ifEmpty { title.trim() }
    }

    private fun extractTheroName(title: String): String {
        val lower = title.lowercase()
        return when {
            lower.contains("niwathapa") || lower.contains("hadapangoda") -> "පූජ්‍ය හඳපාන්ගොඩ නිවාතාප හිමි"
            lower.contains("wossaggadhamma") || lower.contains("wossagghadhamma") || lower.contains("polgahawela") -> "පූජ්‍ය පොල්ගහවෙල වොස්සග්ගධම්ම හිමි"
            lower.contains("dhammavijaya") || lower.contains("ranwala") -> "පූජ්‍ය රන්වල ධම්මවිජය හිමි"
            lower.contains("amadasana") || lower.contains("battaramulle") -> "පූජ්‍ය බත්තරමුල්ලේ අමාදස්සන හිමි"
            lower.contains("dhammajiva") || lower.contains("dodangoda") -> "පූජ්‍ය දොඩංගොඩ ධම්ම ජීව හිමි"
            lower.contains("guru") || lower.contains("swamin") -> "ගුරු ස්වාමීන් වහන්සේ"
            else -> "පහුර ධර්ම දේශකයාණන් වහන්සේ"
        }
    }
}
