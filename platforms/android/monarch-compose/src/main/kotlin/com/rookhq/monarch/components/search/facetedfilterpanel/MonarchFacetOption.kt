package com.rookhq.monarch.components.search.facetedfilterpanel

import androidx.compose.runtime.Immutable

/**
 * One choice in an options facet of a `MonarchFacetedFilterPanel`.
 *
 * @property id the value kept in the selection when the choice is checked. Unique in its facet.
 * @property label the visible label, already localised.
 * @property count how many items match the choice, shown after the label in the locale's digits and read with it.
 * `null` shows no count.
 * @property enabled whether the choice can be checked or unchecked.
 */
@Immutable
public class MonarchFacetOption(
    public val id: String,
    public val label: String,
    public val count: Int? = null,
    public val enabled: Boolean = true,
)
