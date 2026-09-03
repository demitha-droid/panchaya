package org.jethavanarama.dhammasermons.data.repository

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jethavanarama.dhammasermons.data.model.TripitakaNode
import org.jethavanarama.dhammasermons.data.model.TripitakaParagraph
import org.jethavanarama.dhammasermons.data.model.TripitakaVaggaContent
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.io.File
import java.util.concurrent.TimeUnit

class TripitakaRepository(private val context: Context) {

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private var cachedPitakas: List<TripitakaNode>? = null
    private var allLeavesCache: List<TripitakaNode>? = null

    private val cacheDir: File by lazy {
        File(context.filesDir, "tripitaka_cache").apply { if (!exists()) mkdirs() }
    }

    /**
     * Loads the entire Tripitaka catalog hierarchy from bundled assets
     */
    suspend fun getPitakas(): List<TripitakaNode> = withContext(Dispatchers.IO) {
        cachedPitakas?.let { return@withContext it }

        try {
            context.assets.open("tripitaka/toc.xml").use { inputStream ->
                val doc = Jsoup.parse(inputStream, "UTF-8", "", Parser.xmlParser())
                val rootUl = doc.selectFirst("ul") ?: return@withContext emptyList()
                val pitakas = mutableListOf<TripitakaNode>()

                // Direct children <li> are the 3 Pitakas
                val directLis = rootUl.children().filter { it.tagName().equals("li", ignoreCase = true) }
                for (li in directLis) {
                    val node = parseNode(li)
                    if (node != null) {
                        pitakas.add(node)
                    }
                }

                cachedPitakas = pitakas
                allLeavesCache = pitakas.flatMap { it.allLeaves() }
                pitakas
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseNode(li: Element): TripitakaNode? {
        val aTag = li.selectFirst("> a")
        val title = aTag?.text()?.trim() ?: return null
        val type = aTag.attr("class").ifBlank { "item" }
        val nodeId = li.attr("node-id").ifBlank { title }
        val collections = li.attr("collections")
        val orig = li.attr("orig")

        val childUl = li.selectFirst("> ul")
        val children = mutableListOf<TripitakaNode>()
        if (childUl != null) {
            val childLis = childUl.children().filter { it.tagName().equals("li", ignoreCase = true) }
            for (cLi in childLis) {
                val cNode = parseNode(cLi)
                if (cNode != null) {
                    children.add(cNode)
                }
            }
        }

        return TripitakaNode(
            nodeId = nodeId,
            title = title,
            type = type,
            collections = collections,
            orig = orig,
            children = children
        )
    }

    /**
     * Search suttas/vaggas by query string across all Pitakas
     */
    suspend fun searchSuttas(query: String): List<TripitakaNode> = withContext(Dispatchers.Default) {
        if (query.isBlank()) return@withContext emptyList()
        val leaves = allLeavesCache ?: getPitakas().flatMap { it.allLeaves() }
        val cleanQuery = query.trim().lowercase()
        leaves.filter {
            it.title.lowercase().contains(cleanQuery) || it.nodeId.contains(cleanQuery)
        }
    }

    /**
     * Loads the Sutta/Vagga content:
     * 1. Checks local cache
     * 2. If not cached, fetches Sinhala and/or Pali XML from pitaka.lk, parses, and caches locally
     */
    suspend fun getVaggaContent(node: TripitakaNode): Result<TripitakaVaggaContent> = withContext(Dispatchers.IO) {
        val cacheFile = File(cacheDir, "vagga_${node.nodeId}.json")
        if (cacheFile.exists()) {
            try {
                val cached = gson.fromJson(cacheFile.readText(), TripitakaVaggaContent::class.java)
                if (cached != null && cached.paragraphs.isNotEmpty()) {
                    return@withContext Result.success(cached)
                }
            } catch (e: Exception) {
                // Corrupted cache, re-fetch
                cacheFile.delete()
            }
        }

        try {
            var sinhalaXml: String? = null
            var paliXml: String? = null

            if (node.hasSinhala) {
                val sinhUrl = "https://pitaka.lk/main/text/sinh-aps/vagga_${node.nodeId}.xml"
                sinhalaXml = fetchUrl(sinhUrl)
            }
            if (node.hasPali || !node.hasSinhala) {
                val paliUrl = "https://pitaka.lk/main/text/pali-cs/vagga_${node.nodeId}.xml"
                paliXml = fetchUrl(paliUrl)
            }

            val parsed = parseVaggaXml(node, sinhalaXml, paliXml)
            // Cache to disk
            try {
                cacheFile.writeText(gson.toJson(parsed))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Result.success(parsed)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun fetchUrl(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                return response.body?.string()
            }
        }
        return null
    }

    private fun parseVaggaXml(
        node: TripitakaNode,
        sinhalaXml: String?,
        paliXml: String?
    ): TripitakaVaggaContent {
        val sinhDoc = sinhalaXml?.let { Jsoup.parse(it, "", Parser.xmlParser()) }
        val paliDoc = paliXml?.let { Jsoup.parse(it, "", Parser.xmlParser()) }

        val nikaya = sinhDoc?.selectFirst("div.nikaya > span")?.text()
            ?: paliDoc?.selectFirst("div.nikaya > span")?.text() ?: ""
        val book = sinhDoc?.selectFirst("div.book > span")?.text()
            ?: paliDoc?.selectFirst("div.book > span")?.text() ?: ""
        val title = sinhDoc?.selectFirst("div.chapter > span")?.text()
            ?: paliDoc?.selectFirst("div.chapter > span")?.text() ?: node.title

        // Extract Pali paragraphs mapped by para number
        val paliMap = mutableMapOf<String, String>()
        paliDoc?.select("div.paragraph")?.forEach { pDiv ->
            val pNum = pDiv.attr("para").trim()
            val text = pDiv.select("div.bodytext").text().trim()
            if (pNum.isNotBlank() && text.isNotBlank()) {
                paliMap[pNum] = text
            }
        }

        // Extract Sinhala paragraphs mapped by para number
        val sinhMap = mutableMapOf<String, String>()
        sinhDoc?.select("div.paragraph")?.forEach { pDiv ->
            val pNum = pDiv.attr("para").trim()
            val text = pDiv.select("div.bodytext").text().trim()
            if (pNum.isNotBlank() && text.isNotBlank()) {
                sinhMap[pNum] = text
            }
        }

        // Collect all paragraphs and subheadings
        val paragraphs = mutableListOf<TripitakaParagraph>()

        val driverDoc = sinhDoc ?: paliDoc
        if (driverDoc != null) {
            val elements = driverDoc.select("div.chapter > span.chapter, div.subhead > span.subhead, div.paragraph")
            for (el in elements) {
                if (el.hasClass("chapter")) {
                    val hText = el.text().trim()
                    if (hText.isNotBlank()) {
                        paragraphs.add(
                            TripitakaParagraph(
                                paraNumber = "",
                                headingText = hText,
                                isHeading = true,
                                headingLevel = 1
                            )
                        )
                    }
                } else if (el.hasClass("subhead")) {
                    val hText = el.text().trim()
                    if (hText.isNotBlank()) {
                        paragraphs.add(
                            TripitakaParagraph(
                                paraNumber = "",
                                headingText = hText,
                                isHeading = true,
                                headingLevel = 2
                            )
                        )
                    }
                } else if (el.hasClass("paragraph")) {
                    val pNum = el.attr("para").trim()
                    val sText = sinhMap[pNum] ?: if (sinhDoc == null) "" else el.select("div.bodytext").text().trim()
                    val pText = paliMap[pNum] ?: ""

                    if (sText.isNotBlank() || pText.isNotBlank()) {
                        paragraphs.add(
                            TripitakaParagraph(
                                paraNumber = pNum,
                                sinhalaText = sText,
                                paliText = pText,
                                isHeading = false
                            )
                        )
                    }
                }
            }
        }

        return TripitakaVaggaContent(
            vaggaId = node.nodeId,
            title = title,
            nikaya = nikaya,
            book = book,
            paragraphs = paragraphs
        )
    }
}
