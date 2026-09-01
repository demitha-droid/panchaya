package org.jethavanarama.dhammasermons.data.repository

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jethavanarama.dhammasermons.data.model.DhammapadaDataset
import org.jethavanarama.dhammasermons.data.model.DhammapadaKatha
import org.jethavanarama.dhammasermons.data.model.DhammapadaVagga
import java.io.InputStreamReader

class DhammapadaRepository(private val context: Context) {

    private val gson = Gson()
    private var cachedDataset: DhammapadaDataset? = null

    suspend fun getDataset(): DhammapadaDataset = withContext(Dispatchers.IO) {
        cachedDataset?.let { return@withContext it }

        try {
            context.assets.open("dhammapada/dhammapada.json").use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                    val data = gson.fromJson(reader, DhammapadaDataset::class.java)
                    cachedDataset = data
                    data
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            DhammapadaDataset()
        }
    }

    suspend fun getVaggas(): List<DhammapadaVagga> {
        return getDataset().vaggas
    }

    suspend fun getVaggaById(vaggaId: Int): DhammapadaVagga? {
        return getDataset().vaggas.find { it.id == vaggaId }
    }

    suspend fun search(query: String): List<DhammapadaKatha> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()
        val allKathas = getDataset().vaggas.flatMap { it.kathas }
        allKathas.filter { katha ->
            katha.title.contains(q, ignoreCase = true) ||
            katha.kathaNum.contains(q, ignoreCase = true) ||
            katha.gathas.any { g ->
                g.gathaNum.toString() == q ||
                g.sinhalaText.contains(q, ignoreCase = true) ||
                g.paliText.any { line -> line.contains(q, ignoreCase = true) }
            } ||
            katha.storyParagraphs.any { it.contains(q, ignoreCase = true) }
        }
    }
}