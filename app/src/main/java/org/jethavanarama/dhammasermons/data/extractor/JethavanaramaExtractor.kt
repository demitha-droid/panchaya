package org.jethavanarama.dhammasermons.data.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jethavanarama.dhammasermons.data.model.Language
import org.jethavanarama.dhammasermons.data.model.PaginatedSermons
import org.jethavanarama.dhammasermons.data.model.Sermon
import org.jethavanarama.dhammasermons.data.model.SermonCategory
import org.jethavanarama.dhammasermons.data.model.SermonSource
import org.jethavanarama.dhammasermons.data.model.Thero
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class JethavanaramaExtractor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    private val baseUrl = "https://www.jethavanarama.org"
    private val resolvedCache = ConcurrentHashMap<String, Sermon>()
    val durationCache = ConcurrentHashMap<String, Long>()

    val defaultCategories = listOf(
        SermonCategory("all", "සියලු දේශනා (All)", "/sermons/sinhala", ""),
        SermonCategory("maha-dehanawa", "මහා දේශනාව", "/sermons/sinhala/category/maha-dehanawa", "https://as-prod-jethavanarama-deezdkghfmfygade.southeastasia-01.azurewebsites.net/media/2gnmpbdu/smd.webp"),
        SermonCategory("savako", "සාවකෝ", "/sermons/sinhala/category/savako", "https://as-prod-jethavanarama-deezdkghfmfygade.southeastasia-01.azurewebsites.net/media/vmblgaku/sawako-web-1.webp"),
        SermonCategory("anuththaro", "අනුත්තරෝ", "/sermons/sinhala/category/anuththaro", "https://as-prod-jethavanarama-deezdkghfmfygade.southeastasia-01.azurewebsites.net/media/acwevlnk/anuththaro.webp"),
        SermonCategory("yathiwara-yathrawa", "යතිවර යාත්‍රාව", "/sermons/sinhala/category/yathiwara-yathrawa", "https://as-prod-jethavanarama-deezdkghfmfygade.southeastasia-01.azurewebsites.net/media/50vklkzt/yathiwara-yathrawa.webp"),
        SermonCategory("theravaru", "තෙරවරු", "/sermons/sinhala/category/theravaru", "https://as-prod-jethavanarama-deezdkghfmfygade.southeastasia-01.azurewebsites.net/media/3xdbddjl/tw.webp"),
        SermonCategory("niyaya-deshana", "න්‍යාය දේශනා", "/sermons/sinhala/category/niyaya-deshana", "https://as-prod-jethavanarama-deezdkghfmfygade.southeastasia-01.azurewebsites.net/media/fwzc1xae/niyaya.webp"),
        SermonCategory("bawana-wadasatahana", "භාවනා වැඩසටහන", "/sermons/sinhala/category/bawana-wadasatahana", "https://as-prod-jethavanarama-deezdkghfmfygade.southeastasia-01.azurewebsites.net/media/g2dgiumd/bawanawa.webp"),
        SermonCategory("aarya-mithree-bawabawa", "ආර්ය මෛත්‍රී භාවනාව", "/sermons/sinhala/category/aarya-mithree-bawabawa", "https://as-prod-jethavanarama-deezdkghfmfygade.southeastasia-01.azurewebsites.net/media/cnpjmae3/arya.webp"),
        SermonCategory("prayogika-niwan-maga", "ප්‍රායෝගික නිවන් මග", "/sermons/sinhala/category/prayogika-niwan-maga", "https://as-prod-jethavanarama-deezdkghfmfygade.southeastasia-01.azurewebsites.net/media/q2bnwmvh/prayogika-niwan-maga.webp"),
        SermonCategory("theropadeshaya", "ථේරෝපදේශය", "/sermons/sinhala/category/theropadeshaya", "https://as-prod-jethavanarama-deezdkghfmfygade.southeastasia-01.azurewebsites.net/media/rqehnz2j/theropadeshaya.webp")
    )

    val theros = listOf(
        Thero("1584", "ගුරු ස්වාමීන් වහන්සේ"),
        Thero("1583", "පූජ්‍ය බත්තරමුල්ලේ අමාදස්සන හිමි"),
        Thero("1585", "පූජ්‍ය දොඩංගොඩ ධම්ම ජීව හිමි"),
        Thero("1586", "පූජ්‍ය පොල්ගහවෙල වොස්සග්ගධම්ම හිමි"),
        Thero("1587", "පූජ්‍ය මහරගම මෙත්තාරාම හිමි"),
        Thero("1588", "පූජ්‍ය අරකාවිල දූතානන්ද හිමි"),
        Thero("1597", "පූජ්‍ය ගංගොඩවිල ධාරණධම්ම හිමි"),
        Thero("1598", "පූජ්‍ය මොරගොල්ලේ ඉන්දසුමන හිමි")
    )

    suspend fun fetchCategorySermons(
        categorySlugOrPath: String,
        page: Int = 1,
        language: Language = Language.SINHALA
    ): PaginatedSermons = withContext(Dispatchers.IO) {
        val cleanSlug = categorySlugOrPath.removePrefix("/sermons/sinhala/category/").removePrefix("/sermons/sinhala/").removePrefix("/")

        val url = if (cleanSlug == "all" || cleanSlug.isBlank() || cleanSlug == "sermons") {
            "$baseUrl/sermons/${language.code}/search?query=&thero=&category=&page=$page"
        } else if (page > 1) {
            "$baseUrl/sermons/${language.code}/category/$cleanSlug?page=$page"
        } else {
            "$baseUrl/sermons/${language.code}/category/$cleanSlug"
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        try {
            val response = client.newCall(request).execute()
            val html = response.body?.string().orEmpty()
            parsePaginatedHtml(html, page, language)
        } catch (e: Exception) {
            e.printStackTrace()
            PaginatedSermons(emptyList(), page, 1, false, false)
        }
    }

    suspend fun fetchLatestSermons(language: Language = Language.SINHALA): List<Sermon> = withContext(Dispatchers.IO) {
        fetchCategorySermons("all", 1, language).sermons
    }

    suspend fun searchSermons(
        query: String = "",
        theroId: String = "",
        categoryId: String = "",
        page: Int = 1,
        language: Language = Language.SINHALA
    ): PaginatedSermons = withContext(Dispatchers.IO) {
        val effectiveCat = if (categoryId == "all" || categoryId == "/sermons/sinhala") "" else categoryId
        val url = "$baseUrl/sermons/${language.code}/search?query=${query}&thero=${theroId}&category=${effectiveCat}&page=${page}"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        try {
            val response = client.newCall(request).execute()
            val html = response.body?.string().orEmpty()
            parsePaginatedHtml(html, page, language)
        } catch (e: Exception) {
            e.printStackTrace()
            PaginatedSermons(emptyList(), page, 1, false, false)
        }
    }

    private fun parsePaginatedHtml(html: String, page: Int, language: Language): PaginatedSermons {
        val doc = Jsoup.parse(html)
        val sermonItems = mutableListOf<Sermon>()

        // 1. Process by .card containers first to accurately capture image and link pairs
        val cardElements = doc.select(".card")
        val seenUrls = HashSet<String>()

        for (card in cardElements) {
            val linkEl = card.selectFirst("a[href*=/sermons/${language.code}/]") ?: continue
            val href = linkEl.attr("href")
            if (!href.matches(".*/\\d+$".toRegex()) || seenUrls.contains(href)) continue
            seenUrls.add(href)

            val titleEl = card.selectFirst("h3, h4, h5, .title, .sermon-title, a.stretched-link, p.font-bold")
            val title = titleEl?.text()?.trim() ?: linkEl.text().trim()
            if (title.isBlank() || title.length < 2) continue

            val theroEl = card.selectFirst(".thero, .speaker, .author, p.text-muted, div.text-muted:not(.fs-6)")
            val thero = theroEl?.text()?.trim() ?: "ජෙතවනාරාම ස්වාමීන් වහන්සේ"

            val imgEl = card.selectFirst("img")
            var rawThumb = imgEl?.attr("src")?.trim()

            // Filter out placeholder / logo images
            if (rawThumb != null && (rawThumb.contains("elementor-placeholder-image") || rawThumb.contains("/logo.") || rawThumb.endsWith("logo.png"))) {
                rawThumb = null
            }

            // Extract YouTube video ID if image is from img.youtube.com or ytimg.com
            val youtubeId = extractYouTubeId(rawThumb)
            if (youtubeId != null) {
                rawThumb = "https://i.ytimg.com/vi/$youtubeId/hqdefault.jpg"
            } else if (!rawThumb.isNullOrBlank() && !rawThumb.startsWith("http")) {
                rawThumb = "$baseUrl$rawThumb"
            }

            val dateEl = card.selectFirst(".date, time, span.text-xs, .fs-6.sinhala")
            val date = dateEl?.text()?.replace(".*\\b(\\d{2}-\\d{2}-\\d{4})\\b.*".toRegex(), "$1")?.trim()
            val cachedDur = durationCache[youtubeId ?: href] ?: 0L

            sermonItems.add(
                Sermon(
                    id = href,
                    title = title,
                    thero = thero,
                    source = SermonSource.JETHAVANARAMA,
                    youtubeVideoId = youtubeId,
                    audioUrl = null,
                    thumbnailUrl = rawThumb,
                    durationSeconds = cachedDur,
                    date = date,
                    language = language
                )
            )
        }

        // Fallback if cards were not selected
        if (sermonItems.isEmpty()) {
            val linkElements = doc.select("a[href*=/sermons/${language.code}/]")
            for (el in linkElements) {
                val href = el.attr("href")
                if (!href.matches(".*/\\d+$".toRegex()) || seenUrls.contains(href)) continue
                seenUrls.add(href)

                val title = el.text().trim()
                if (title.isBlank() || title.length < 2) continue

                val parentCard = el.parents().firstOrNull { it.hasClass("card") || it.tagName() == "article" }
                val imgEl = parentCard?.selectFirst("img") ?: el.selectFirst("img")
                var rawThumb = imgEl?.attr("src")?.trim()

                if (rawThumb != null && (rawThumb.contains("elementor-placeholder-image") || rawThumb.contains("/logo.") || rawThumb.endsWith("logo.png"))) {
                    rawThumb = null
                }

                val youtubeId = extractYouTubeId(rawThumb)
                if (youtubeId != null) {
                    rawThumb = "https://i.ytimg.com/vi/$youtubeId/hqdefault.jpg"
                } else if (!rawThumb.isNullOrBlank() && !rawThumb.startsWith("http")) {
                    rawThumb = "$baseUrl$rawThumb"
                }

                val cachedDur = durationCache[youtubeId ?: href] ?: 0L

                sermonItems.add(
                    Sermon(
                        id = href,
                        title = title,
                        thero = "ජෙතවනාරාම ස්වාමීන් වහන්සේ",
                        source = SermonSource.JETHAVANARAMA,
                        youtubeVideoId = youtubeId,
                        audioUrl = null,
                        thumbnailUrl = rawThumb,
                        durationSeconds = cachedDur,
                        date = null,
                        language = language
                    )
                )
            }
        }

        // Pagination calculation
        var totalPages = page
        val pageLinks = doc.select("a[href*='page='], ul.pagination a, nav a")
        for (pl in pageLinks) {
            val pText = pl.text().trim()
            pText.toIntOrNull()?.let { pNum ->
                if (pNum > totalPages) totalPages = pNum
            }
            val href = pl.attr("href")
            val pageParam = href.substringAfter("page=", "").substringBefore("&").toIntOrNull()
            if (pageParam != null && pageParam > totalPages) {
                totalPages = pageParam
            }
        }

        val hasNext = totalPages > page || sermonItems.size >= 12
        if (hasNext && totalPages <= page) {
            totalPages = page + 1
        }
        val hasPrev = page > 1

        return PaginatedSermons(
            sermons = sermonItems,
            currentPage = page,
            totalPages = totalPages,
            hasNextPage = hasNext,
            hasPreviousPage = hasPrev
        )
    }

    suspend fun resolveSermonAudioUrl(sermon: Sermon): Sermon = withContext(Dispatchers.IO) {
        if (!sermon.audioUrl.isNullOrBlank()) return@withContext sermon
        resolvedCache[sermon.id]?.let { return@withContext it }

        val detailUrl = if (sermon.id.startsWith("http")) sermon.id else "$baseUrl${sermon.id}"

        val request = Request.Builder()
            .url(detailUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
            .build()

        try {
            val response = client.newCall(request).execute()
            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html)

            var audioUrl: String? = null

            // 1. Direct <audio source src="..."> or <audio src="...">
            val audioElement = doc.selectFirst("audio source[src], audio[src]")
            if (audioElement != null) {
                audioUrl = audioElement.attr("src")
            }

            // 2. Direct download mp3 link
            if (audioUrl.isNullOrBlank()) {
                val mp3Link = doc.selectFirst("a[href*='.mp3'], a[download*='.mp3']")
                if (mp3Link != null) {
                    audioUrl = mp3Link.attr("href")
                }
            }

            // 3. Media storage URL in HTML scripts or links
            if (audioUrl.isNullOrBlank()) {
                val mediaRegex = """https?://[^\s"'<>]+\.mp3""".toRegex()
                val match = mediaRegex.find(html)
                if (match != null) {
                    audioUrl = match.value
                }
            }

            // Clean relative URLs
            if (!audioUrl.isNullOrBlank() && !audioUrl.startsWith("http")) {
                audioUrl = "$baseUrl$audioUrl"
            }

            // 4. YouTube fallback if present on page
            var youtubeId = extractYouTubeId(sermon.youtubeVideoId)
            if (youtubeId.isNullOrBlank()) {
                val iframe = doc.selectFirst("iframe[src*='youtube.com'], iframe[src*='youtu.be']")
                if (iframe != null) {
                    youtubeId = extractYouTubeId(iframe.attr("src"))
                }
            }
            if (youtubeId.isNullOrBlank()) {
                val ytImg = doc.selectFirst("img[src*='youtube.com/vi/'], img[src*='ytimg.com/vi/']")
                if (ytImg != null) {
                    youtubeId = extractYouTubeId(ytImg.attr("src"))
                }
            }

            var thumb = sermon.thumbnailUrl
            if (thumb.isNullOrBlank() || thumb.contains("elementor-placeholder-image") || thumb.contains("/logo.") || thumb.endsWith("logo.png")) {
                thumb = if (!youtubeId.isNullOrBlank()) "https://i.ytimg.com/vi/$youtubeId/hqdefault.jpg" else null
            } else {
                val thumbYtId = extractYouTubeId(thumb)
                if (thumbYtId != null) {
                    thumb = "https://i.ytimg.com/vi/$thumbYtId/hqdefault.jpg"
                }
            }

            var dur = sermon.durationSeconds
            if (dur == 0L && !youtubeId.isNullOrBlank()) {
                dur = resolveDurationSeconds(youtubeId)
            }

            val resolved = sermon.copy(
                audioUrl = audioUrl,
                youtubeVideoId = youtubeId,
                thumbnailUrl = thumb,
                durationSeconds = dur
            )
            resolvedCache[sermon.id] = resolved
            resolved
        } catch (e: Exception) {
            e.printStackTrace()
            sermon
        }
    }

    suspend fun resolveDurationSeconds(videoIdOrId: String): Long = withContext(Dispatchers.IO) {
        if (videoIdOrId.isBlank()) return@withContext 0L
        durationCache[videoIdOrId]?.let { return@withContext it }

        // Clean video ID
        val videoId = if (videoIdOrId.startsWith("/sermons/")) {
            resolvedCache[videoIdOrId]?.youtubeVideoId.orEmpty()
        } else {
            extractYouTubeId(videoIdOrId) ?: videoIdOrId
        }

        if (videoId.isNotBlank()) {
            try {
                val req = Request.Builder()
                    .url("https://www.youtube.com/watch?v=$videoId")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                    .build()
                val resp = client.newCall(req).execute()
                val body = resp.body?.string().orEmpty()
                val match = """"lengthSeconds":"(\d+)"""".toRegex().find(body)
                val sec = match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                if (sec > 0L) {
                    durationCache[videoId] = sec
                    durationCache[videoIdOrId] = sec
                    return@withContext sec
                }
            } catch (e: Exception) {
                // Non-fatal fallback
            }
        }

        0L
    }

    companion object {
        private val YOUTUBE_ID_REGEX = Regex("""(?:youtube\.com/vi/|ytimg\.com/vi/|/vi/|/embed/|watch\?v=|youtu\.be/)([a-zA-Z0-9_-]{11})""")

        fun extractYouTubeId(urlOrText: String?): String? {
            if (urlOrText.isNullOrBlank()) return null
            val text = urlOrText.replace("&amp;", "&")
            val match = YOUTUBE_ID_REGEX.find(text)
            if (match != null) return match.groupValues[1]
            if (text.contains("/vi/")) {
                val raw = text.substringAfter("/vi/").substringBefore("/").substringBefore("&").substringBefore("?").trim()
                if (raw.length >= 11) return raw.take(11)
            }
            return null
        }
    }
}
