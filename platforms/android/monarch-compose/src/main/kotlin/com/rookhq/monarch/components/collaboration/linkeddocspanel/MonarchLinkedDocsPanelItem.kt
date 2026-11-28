package com.rookhq.monarch.components.collaboration.linkeddocspanel

import androidx.compose.runtime.Immutable

/**
 * One doc or link attached to a record, shown as a row of a `MonarchLinkedDocsPanel`.
 *
 * @property id the link's id, unique per row.
 * @property title the doc's or the page's title.
 * @property kind a doc or an external link.
 * @property url where the row leads. For a link without [meta] its host name is shown.
 * @property docId for a doc: the document's id, used to leave linked docs out of the picker.
 * @property meta secondary text, such as "Updated 2 hours ago". Links show their host name without it.
 * @property status a short status, such as "Draft", shown as a badge.
 * @property spaceId the id of the Space the doc lives in; drives the filter chips. `null` for no Space.
 * @property spaceName the name of that Space.
 * @property fileName the doc's file name, which picks the file type icon, such as "notes.pdf".
 * @property unlinkable whether this row can be unlinked.
 */
@Immutable
public class MonarchLinkedDocsPanelItem(
    public val id: String,
    public val title: String,
    public val kind: MonarchLinkedDocsPanelKind = MonarchLinkedDocsPanelKind.Doc,
    public val url: String? = null,
    public val docId: String? = null,
    public val meta: String? = null,
    public val status: String? = null,
    public val spaceId: String? = null,
    public val spaceName: String? = null,
    public val fileName: String? = null,
    public val unlinkable: Boolean = true,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchLinkedDocsPanelItem &&
            other.id == id &&
            other.title == title &&
            other.kind == kind &&
            other.url == url &&
            other.docId == docId &&
            other.meta == meta &&
            other.status == status &&
            other.spaceId == spaceId &&
            other.spaceName == spaceName &&
            other.fileName == fileName &&
            other.unlinkable == unlinkable

    override fun hashCode(): Int = listOf(id, title, kind, url, docId, meta, status, spaceId, spaceName, fileName)
        .fold(unlinkable.hashCode()) { hash, value -> HASH * hash + (value?.hashCode() ?: 0) }

    override fun toString(): String = "MonarchLinkedDocsPanelItem(id=$id, title=$title, kind=$kind)"

    private companion object {
        const val HASH = 31
    }
}
