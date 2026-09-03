package org.jethavanarama.dhammasermons.data.model

/**
 * Represents a node in the Tripitaka hierarchy (Pitaka -> Nikaya/Book -> Vagga/Sutta)
 */
data class TripitakaNode(
    val nodeId: String,
    val title: String,
    val type: String, // "pitaka", "nikaya", "book", "vagga"
    val collections: String = "", // e.g. "pali-cs,sinh-aps"
    val orig: String = "",
    val children: List<TripitakaNode> = emptyList()
) {
    val isLeaf: Boolean
        get() = children.isEmpty()

    val hasSinhala: Boolean
        get() = collections.contains("sinh-aps")

    val hasPali: Boolean
        get() = collections.contains("pali-cs")

    /**
     * Recursively counts all readable leaf vaggas under this node
     */
    val leafCount: Int by lazy {
        if (isLeaf) 1 else children.sumOf { it.leafCount }
    }

    /**
     * Flattens all leaf vaggas under this node
     */
    fun allLeaves(): List<TripitakaNode> {
        if (isLeaf) return listOf(this)
        return children.flatMap { it.allLeaves() }
    }
}

/**
 * Represents an individual paragraph or section heading inside a Sutta/Vagga
 */
data class TripitakaParagraph(
    val paraNumber: String,
    val sinhalaText: String = "",
    val paliText: String = "",
    val headingText: String = "",
    val isHeading: Boolean = false,
    val headingLevel: Int = 0 // 1: Main Sutta title, 2: Subheading/Section
)

/**
 * Represents the full content of a loaded Vagga/Sutta
 */
data class TripitakaVaggaContent(
    val vaggaId: String,
    val title: String,
    val nikaya: String = "",
    val book: String = "",
    val paragraphs: List<TripitakaParagraph> = emptyList()
)
