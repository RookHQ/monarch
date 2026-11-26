package com.rookhq.monarch.components.intelligence.aiclaimsources

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiSourceKind
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiSourcePart

/**
 * A source that evidence points at.
 *
 * @property id a stable id, matched against `MonarchAiClaimEvidence.sourceId`.
 * @property title the title shown with every passage from this source.
 * @property url where the source lives. Opened only when it is an absolute http, https or mailto address.
 * @property kind what kind of thing the source is; its name is shown with the title.
 * @property publishedMillis when it was published, in epoch milliseconds (UTC).
 */
@Immutable
public data class MonarchAiClaimSource(
    public val id: String,
    public val title: String,
    public val url: String? = null,
    public val kind: MonarchAiSourceKind? = null,
    public val publishedMillis: Long? = null,
) {
    /**
     * A source from a source part of the message model.
     *
     * @param part the source part.
     * @param publishedMillis when it was published, in epoch milliseconds (UTC).
     */
    public constructor(part: MonarchAiSourcePart, publishedMillis: Long? = null) :
        this(id = part.id, title = part.title, url = part.url, kind = part.kind, publishedMillis = publishedMillis)
}
