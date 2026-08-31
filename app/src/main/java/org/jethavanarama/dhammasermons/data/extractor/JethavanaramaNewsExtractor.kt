package org.jethavanarama.dhammasermons.data.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jethavanarama.dhammasermons.data.model.NewsItem
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class JethavanaramaNewsExtractor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    private val baseUrl = "https://www.jethavanarama.org"
    private var cachedNews: List<NewsItem> = emptyList()

    private class MutableNewsData(
        val url: String,
        var title: String = "",
        var summary: String = "",
        var imageUrl: String = ""
    )

    suspend fun fetchNews(forceRefresh: Boolean = false): List<NewsItem> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedNews.isNotEmpty()) {
            return@withContext cachedNews
        }

        // Fetch official website articles from jethavanarama.org/news
        val webArticles = fetchWebsiteNews()
        val distinctNews = webArticles.distinctBy { it.title.trim() }
        if (distinctNews.isNotEmpty()) {
            cachedNews = distinctNews
        }
        distinctNews
    }

    private fun fetchWebsiteNews(): List<NewsItem> {
        val request = Request.Builder()
            .url("$baseUrl/news")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        return try {
            val response = client.newCall(request).execute()
            val html = response.body?.string().orEmpty()
            parseNewsHtml(html)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseNewsHtml(html: String): List<NewsItem> {
        val doc = Jsoup.parse(html)
        val newsMap = LinkedHashMap<String, MutableNewsData>()

        // 1. Group all elements associated with each /news/ link
        val allLinks = doc.select("a[href*=/news/]")
        for (link in allLinks) {
            val href = link.attr("href").trim()
            if (href.isBlank() || href == "/news" || href == "/news/") continue
            val fullUrl = if (href.startsWith("http")) href else "$baseUrl$href"
            val item = newsMap.getOrPut(fullUrl) { MutableNewsData(url = fullUrl) }

            // Extract image if inside <a>
            val img = link.selectFirst("img")?.attr("src")?.trim().orEmpty()
            if (img.isNotBlank() && !img.contains("logo", ignoreCase = true)) {
                item.imageUrl = if (img.startsWith("http")) img else "$baseUrl$img"
            }

            // Extract title if non-empty text
            val text = link.text().trim()
            if (text.isNotBlank() && !text.equals("Read more", ignoreCase = true)) {
                if (item.title.isBlank()) item.title = text
            }
        }

        // 2. Scan card / col / section containers to associate headings, images and paragraphs
        val containers = doc.select("div[class*=col], div[class*=card], div[class*=item], section")
        for (container in containers) {
            val link = container.selectFirst("a[href*=/news/]") ?: continue
            val href = link.attr("href").trim()
            if (href.isBlank() || href == "/news" || href == "/news/") continue
            val fullUrl = if (href.startsWith("http")) href else "$baseUrl$href"
            val item = newsMap.getOrPut(fullUrl) { MutableNewsData(url = fullUrl) }

            if (item.imageUrl.isBlank()) {
                val img = container.selectFirst("img")?.attr("src")?.trim().orEmpty()
                if (img.isNotBlank() && !img.contains("logo", ignoreCase = true)) {
                    item.imageUrl = if (img.startsWith("http")) img else "$baseUrl$img"
                }
            }

            if (item.title.isBlank()) {
                val title = container.selectFirst("h1, h2, h3, h4, h5")?.text()?.trim().orEmpty()
                if (title.isNotBlank()) item.title = title
            }

            if (item.summary.isBlank()) {
                val p = container.selectFirst("p")?.text()?.trim().orEmpty()
                if (p.isNotBlank()) item.summary = p
            }
        }

        val defaultBackupImage = "https://as-prod-jethavanarama-deezdkghfmfygade.southeastasia-01.azurewebsites.net/media/0croshjt/aboutus-bnr.webp"

        return newsMap.values
            .filter { it.title.isNotBlank() || it.imageUrl.isNotBlank() || it.summary.isNotBlank() }
            .map {
                val cleanTitle = if (it.title.isNotBlank()) it.title else it.summary.take(50).ifBlank { "ජෙතවනාරාම පුවත්" }
                val id = it.url.removePrefix("$baseUrl/news/").removePrefix("/news/").removePrefix("/")
                NewsItem(
                    id = id.ifBlank { System.currentTimeMillis().toString() },
                    title = cleanTitle,
                    summary = it.summary,
                    imageUrl = it.imageUrl.ifBlank { defaultBackupImage },
                    articleUrl = it.url,
                    dateText = "ජෙතවනාරාමය"
                )
            }
    }

    suspend fun fetchArticleFullContent(articleUrl: String): String = withContext(Dispatchers.IO) {
        val localMatch = cachedNews.firstOrNull { it.articleUrl == articleUrl }
        if (localMatch != null && !localMatch.fullContent.isNullOrBlank()) {
            return@withContext localMatch.fullContent
        }

        if (!articleUrl.startsWith("http")) {
            return@withContext localMatch?.summary.orEmpty()
        }

        try {
            val request = Request.Builder()
                .url(articleUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html)

            val paragraphs = doc.select(".content-page p, .container p, main p, article p")
                .map { it.text().trim() }
                .filter { it.isNotBlank() && !it.contains("Copyright", ignoreCase = true) }

            paragraphs.joinToString("\n\n")
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
