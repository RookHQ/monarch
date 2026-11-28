package com.rookhq.monarch.components.intelligence.aiconversation

/**
 * A source the reply used.
 *
 * @property id the id of the source.
 * @property title the title.
 * @property url where the source is.
 * @property kind what kind of thing the source is.
 * @property product the product the source lives in, for example `dossier`.
 * @property favicon the URL of the source's favicon.
 */
public data class MonarchAiSourcePart(
    public val id: String,
    public val title: String,
    public val url: String? = null,
    public val kind: MonarchAiSourceKind,
    public val product: String? = null,
    public val favicon: String? = null,
) : MonarchAiPart {
    override val type: String get() = "source"
}
