package com.rookhq.monarch.components.workflow.dependencypicker

import androidx.compose.runtime.Immutable

/**
 * One existing link from this item to another.
 *
 * @property id the stable id of the link itself.
 * @property relation how the other item relates to this one.
 * @property item the other item.
 */
@Immutable
public class MonarchDependencyLink(
    public val id: String,
    public val relation: MonarchDependencyRelation,
    public val item: MonarchDependencyItem,
)
