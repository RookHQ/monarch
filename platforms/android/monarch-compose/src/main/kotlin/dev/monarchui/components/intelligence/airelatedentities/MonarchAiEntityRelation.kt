package dev.monarchui.components.intelligence.airelatedentities

import androidx.compose.runtime.Immutable

/**
 * A link between two entities of a `MonarchAiRelatedEntities`, by id.
 *
 * @property from the id of one entity.
 * @property to the id of the other entity.
 * @property label how they relate, for example "works at". Read as "works at Acme".
 */
@Immutable
public class MonarchAiEntityRelation(
    public val from: String,
    public val to: String,
    public val label: String? = null,
)
