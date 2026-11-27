package com.rookhq.monarch.components.files.documentrelations

import androidx.compose.runtime.Immutable

/**
 * One relation of this document.
 *
 * @property id the relation's stable id, passed to `onRemove`.
 * @property type the `value` of a `MonarchDocumentRelationType`.
 * @property direction which way the relation points; an incoming relation is read as the type's inverse.
 * @property target the document at the other end.
 */
@Immutable
public class MonarchDocumentRelation(
    public val id: String,
    public val type: String,
    public val target: MonarchDocumentRelationTarget,
    public val direction: MonarchDocumentRelationDirection = MonarchDocumentRelationDirection.Outgoing,
)
