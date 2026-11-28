package com.rookhq.monarch.components.collaboration.linkeddocspanel

import androidx.compose.runtime.Immutable

/**
 * A doc the person can link from the Link doc menu of a `MonarchLinkedDocsPanel`.
 *
 * @property docId the document's id, passed to `onLinkDoc`.
 * @property label the document's title.
 * @property description a second line, such as the Space it lives in.
 */
@Immutable
public class MonarchLinkedDocsPanelOption(
    public val docId: String,
    public val label: String,
    public val description: String? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchLinkedDocsPanelOption &&
            other.docId == docId &&
            other.label == label &&
            other.description == description

    override fun hashCode(): Int = (docId.hashCode() * HASH + label.hashCode()) * HASH + (description?.hashCode() ?: 0)

    override fun toString(): String = "MonarchLinkedDocsPanelOption(docId=$docId, label=$label)"

    private companion object {
        const val HASH = 31
    }
}
