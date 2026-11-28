package com.rookhq.monarch.components.intelligence.airelatedentities

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiRelatedEntities`. Every
 * `null` uses the built-in string in `strings_airelatedentities.xml`.
 *
 * @property label the name TalkBack reads for the list or graph: "Related entities".
 * @property kind the visible name of a kind: "Person", "Organization". Unknown kinds show their value.
 * @property weight writes the spoken relevance from a weight from 0 to 1: "Relevance 90%".
 * @property relation writes one relation from the other entity and the relation label: "works at Acme", or
 * "Acme" without a label.
 * @property relatedTo introduces the relations: "Related to".
 * @property empty the text shown when there are no entities: "No related entities".
 */
@Immutable
public class MonarchAiRelatedEntitiesLabels(
    public val label: String? = null,
    public val kind: ((MonarchAiRelatedEntityKind) -> String)? = null,
    public val weight: ((weight: Double) -> String)? = null,
    public val relation: ((other: String, label: String?) -> String)? = null,
    public val relatedTo: String? = null,
    public val empty: String? = null,
)
