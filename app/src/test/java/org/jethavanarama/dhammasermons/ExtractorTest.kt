package org.jethavanarama.dhammasermons

import kotlinx.coroutines.runBlocking
import org.jethavanarama.dhammasermons.data.extractor.AudioExtractionResult
import org.jethavanarama.dhammasermons.data.extractor.JethavanaramaExtractor
import org.jethavanarama.dhammasermons.data.extractor.PahuraYouTubeExtractor
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractorTest {

    @Test
    fun testJethavanaramaExtractor() = runBlocking {
        val jethExtractor = JethavanaramaExtractor()
        val paginated = jethExtractor.fetchCategorySermons("all", page = 1)
        println("=== Jethavanarama All Categories Test ===")
        println("Fetched ${paginated.sermons.size} sermons on Page ${paginated.currentPage} (Total pages: ${paginated.totalPages})")
        assertTrue("Sermons list should not be empty", paginated.sermons.isNotEmpty())

        val firstSermon = paginated.sermons.first()
        println("First sermon: ${firstSermon.title} (ID: ${firstSermon.id})")
        println("Thumbnail URL: ${firstSermon.thumbnailUrl}")
        assertNotNull("Thumbnail URL should not be null", firstSermon.thumbnailUrl)
        assertTrue("Thumbnail URL must start with http", firstSermon.thumbnailUrl!!.startsWith("http"))

        val resolved = jethExtractor.resolveSermonAudioUrl(firstSermon)
        println("Resolved Audio URL: ${resolved.audioUrl}")
        assertNotNull("Jethavanarama audio URL must be resolved", resolved.audioUrl)
        assertTrue("Audio URL must start with http", resolved.audioUrl!!.startsWith("http"))
    }

    @Test
    fun testJethavanaramaTheroSearchAllCategories() = runBlocking {
        val jethExtractor = JethavanaramaExtractor()
        val paginated = jethExtractor.searchSermons(theroId = "1583", categoryId = "", page = 1)
        println("=== Jethavanarama Thero Search All Categories Test ===")
        println("Fetched ${paginated.sermons.size} sermons for Thero 1583 across ALL categories")
        assertTrue("Thero search should return sermons", paginated.sermons.isNotEmpty())
        val firstSermon = paginated.sermons.first()
        println("First Thero sermon thumbnail: ${firstSermon.thumbnailUrl}")
        assertNotNull("Thumbnail URL should not be null", firstSermon.thumbnailUrl)
    }

    @Test
    fun testPahuraYouTubeAudioExtraction() = runBlocking {
        try {
            org.schabi.newpipe.extractor.NewPipe.init(
                org.jethavanarama.dhammasermons.data.extractor.NewPipeDownloaderImpl.init(),
                org.schabi.newpipe.extractor.localization.Localization.DEFAULT,
                org.schabi.newpipe.extractor.localization.ContentCountry("US")
            )
        } catch (e: Exception) {
            println("NewPipe init exception: ${e.message}")
        }

        val extractor = PahuraYouTubeExtractor()
        val paginated = extractor.fetchChannelSermons(page = 1)
        println("\n=== Pahura YouTube Channel Test ===")
        println("Fetched ${paginated.sermons.size} sermons from Pahura channel on Page ${paginated.currentPage}")
        assertTrue("Pahura sermons should not be empty", paginated.sermons.isNotEmpty())

        val firstSermon = paginated.sermons.first()
        println("Pahura sermon thumbnail: ${firstSermon.thumbnailUrl}")
        println("Pahura sermon duration: ${firstSermon.durationFormatted} (${firstSermon.durationSeconds}s)")
        println("Pahura sermon upload date: ${firstSermon.date}")
        assertNotNull("Pahura thumbnail should not be null", firstSermon.thumbnailUrl)
        assertTrue("Duration seconds should be > 0", firstSermon.durationSeconds > 0)
        assertNotNull("Formatted duration string should exist", firstSermon.durationFormatted)

        val videoId = firstSermon.youtubeVideoId ?: "p_RAjUOre5w"
        println("Testing MP3 audio extraction for video: $videoId (${firstSermon.title})")

        val result = extractor.extractAudioStreamUrl(videoId)
        assertTrue("Audio extraction must be successful", result is AudioExtractionResult.Success)
        val success = result as AudioExtractionResult.Success
        println("SUCCESS! Audio URL: ${success.audioUrl}")
        println("Format: ${success.format} | Bitrate: ${success.bitrate}")
        assertTrue("MP3 URL should start with http", success.audioUrl.startsWith("http"))

        // Test downloading the first chunk of the MP3 stream to verify connectivity and content-type
        val client = okhttp3.OkHttpClient()
        val req = okhttp3.Request.Builder().url(success.audioUrl).header("Range", "bytes=0-1024").build()
        val resp = client.newCall(req).execute()
        println("HTTP Stream Status: ${resp.code} | Content-Type: ${resp.header("Content-Type")}")
        assertTrue("HTTP response code should be 200 or 206", resp.code == 200 || resp.code == 206)
        resp.close()
    }

    @Test
    fun testPahuraShortsExtraction() = runBlocking {
        val extractor = PahuraYouTubeExtractor()
        val paginatedShorts = extractor.fetchChannelShorts(page = 1)
        println("\n=== Pahura Shorts Test ===")
        println("Fetched ${paginatedShorts.sermons.size} shorts from Pahura channel on Page ${paginatedShorts.currentPage}")
        assertTrue("Shorts list should not be empty", paginatedShorts.sermons.isNotEmpty())

        val firstShort = paginatedShorts.sermons.first()
        println("First Short: ${firstShort.title} (Video ID: ${firstShort.youtubeVideoId})")
        println("First Short thumbnail: ${firstShort.thumbnailUrl}")
        assertNotNull("Short Video ID must exist", firstShort.youtubeVideoId)
        assertNotNull("Short thumbnail must exist", firstShort.thumbnailUrl)
    }

    @Test
    fun testExtractYouTubeId() {
        org.junit.Assert.assertEquals(
            "8TmhDr9GrBU",
            JethavanaramaExtractor.extractYouTubeId("https://img.youtube.com/vi/8TmhDr9GrBU&amp;pp/maxresdefault.jpg")
        )
        org.junit.Assert.assertEquals(
            "8TmhDr9GrBU",
            JethavanaramaExtractor.extractYouTubeId("https://img.youtube.com/vi/8TmhDr9GrBU&pp/maxresdefault.jpg")
        )
        org.junit.Assert.assertEquals(
            "Yc2bE__kCNQ",
            JethavanaramaExtractor.extractYouTubeId("https://img.youtube.com/vi/Yc2bE__kCNQ/maxresdefault.jpg")
        )
        org.junit.Assert.assertEquals(
            "bwt7gIBiGfU",
            JethavanaramaExtractor.extractYouTubeId("https://www.youtube.com/embed/bwt7gIBiGfU?rel=0")
        )
    }

    @Test
    fun testJethavanaramaThumbnails() = runBlocking {
        val jethExtractor = JethavanaramaExtractor()
        val paginated = jethExtractor.fetchCategorySermons("all", page = 1)
        println("\n=== Testing Thumbnails for all sermons on Page 1 ===")
        val client = okhttp3.OkHttpClient()
        for ((idx, sermon) in paginated.sermons.withIndex()) {
            println("[$idx] ${sermon.title}")
            println("    youtubeVideoId: ${sermon.youtubeVideoId}")
            println("    thumbnailUrl: ${sermon.thumbnailUrl}")
            assertNotNull("Thumbnail should not be null for sermon: ${sermon.title}", sermon.thumbnailUrl)
            assertTrue("Thumbnail must not contain &pp: ${sermon.thumbnailUrl}", !sermon.thumbnailUrl!!.contains("&pp"))
            assertTrue("Thumbnail must not contain &amp;: ${sermon.thumbnailUrl}", !sermon.thumbnailUrl!!.contains("&amp;"))

            // Verify the first 3 thumbnails return HTTP 200 OK
            if (idx < 3) {
                val req = okhttp3.Request.Builder().url(sermon.thumbnailUrl!!).head().build()
                val resp = client.newCall(req).execute()
                println("    HTTP Check: ${resp.code} for ${sermon.thumbnailUrl}")
                assertTrue("Thumbnail must return HTTP 200, got ${resp.code}", resp.isSuccessful)
                resp.close()
            }
        }
    }

    @Test
    fun testJethavanaramaDurationExtraction() = runBlocking {
        val jethExtractor = JethavanaramaExtractor()
        val sermons = jethExtractor.fetchLatestSermons()
        assertTrue("Sermons should not be empty", sermons.isNotEmpty())

        val firstWithVideo = sermons.firstOrNull { !it.youtubeVideoId.isNullOrBlank() }
        assertNotNull("Should have a sermon with youtube video id", firstWithVideo)

        println("\n=== Jethavanarama Duration Test ===")
        println("Testing sermon: ${firstWithVideo!!.title}")
        println("YouTube Video ID: ${firstWithVideo.youtubeVideoId}")

        val duration = jethExtractor.resolveDurationSeconds(firstWithVideo.youtubeVideoId!!)
        println("Resolved Duration: $duration seconds")
        assertTrue("Resolved duration should be greater than 0", duration > 0L)

        val sampleSermon = firstWithVideo.copy(durationSeconds = duration)
        println("Formatted Duration Timestamp: ${sampleSermon.durationFormatted}")
        assertNotNull("Formatted duration timestamp should not be null", sampleSermon.durationFormatted)
    }
}
