package com.rookhq.monarch.components.files.documentrelations

import androidx.compose.runtime.Immutable

/**
 * A document at the other end of a relation, or one that can be linked.
 *
 * @property id the document's stable id, passed to `onAdd`.
 * @property title the document's title. Already localised.
 * @property description a second line, such as its space or folder.
 * @property fileName a file name, such as "plan.pdf", for the `MonarchFileTypeIcon` at the start of the row.
 * `null` shows no icon.
 */
@Immutable
public class MonarchDocumentRelationTarget(
    public val id: String,
    public val title: String,
    public val description: String? = null,
    public val fileName: String? = null,
)
