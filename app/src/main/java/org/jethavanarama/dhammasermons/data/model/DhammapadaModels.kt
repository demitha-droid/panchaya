package org.jethavanarama.dhammasermons.data.model

import com.google.gson.annotations.SerializedName

data class DhammapadaDataset(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("totalVaggas") val totalVaggas: Int = 26,
    @SerializedName("totalKathas") val totalKathas: Int = 305,
    @SerializedName("totalGathas") val totalGathas: Int = 423,
    @SerializedName("vaggas") val vaggas: List<DhammapadaVagga> = emptyList()
)

data class DhammapadaVagga(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("englishName") val englishName: String = "",
    @SerializedName("gathaRange") val gathaRange: String = "",
    @SerializedName("kathaCount") val kathaCount: Int = 0,
    @SerializedName("kathas") val kathas: List<DhammapadaKatha> = emptyList()
)

data class DhammapadaKatha(
    @SerializedName("id") val id: String,
    @SerializedName("kathaNum") val kathaNum: String = "",
    @SerializedName("title") val title: String,
    @SerializedName("gathas") val gathas: List<DhammapadaGatha> = emptyList(),
    @SerializedName("storyParagraphs") val storyParagraphs: List<String> = emptyList()
)

data class DhammapadaGatha(
    @SerializedName("gathaNum") val gathaNum: Int,
    @SerializedName("paliText") val paliText: List<String> = emptyList(),
    @SerializedName("sinhalaText") val sinhalaText: String = "",
    @SerializedName("audioUrl") val audioUrl: String = "",
    @SerializedName("thumbnailUrl") val thumbnailUrl: String = "",
    @SerializedName("paintingUrl") val paintingUrl: String = ""
)