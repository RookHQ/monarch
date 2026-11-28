package com.rookhq.monarch.components.intelligence.aiconversation

/**
 * A place in the text that cites a source.
 *
 * @property id the id of the citation.
 * @property sourceId the id of the source part it cites.
 * @property quote the passage of the source that backs the text.
 */
public data class MonarchAiCitationPart(
    public val id: String,
    public val sourceId: String,
    public val quote: String? = null,
) : MonarchAiPart {
    override val type: String get() = "citation"
}
