package com.rookhq.monarch.components.intelligence.aiconversation

/**
 * Something the reply made that stands on its own: a document, code, a table. Show it with
 * `MonarchAiArtifactCard`.
 *
 * @property id the id of the artifact.
 * @property kind what the artifact is.
 * @property title the title.
 * @property version the version, counted from 1.
 */
public data class MonarchAiArtifactPart(
    public val id: String,
    public val kind: MonarchAiArtifactKind,
    public val title: String,
    public val version: Int,
) : MonarchAiPart {
    override val type: String get() = "artifact"
}
