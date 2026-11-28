package com.rookhq.monarch.components.navigation.megamenu

import androidx.compose.runtime.Immutable

/**
 * A top-level entry of a [MonarchMegaMenu]: either a direct destination ([link]) or a group that expands to show
 * [sections] and a [featured] card.
 *
 * @property id identifies the entry for `expandedItem`.
 * @property label the name of the entry.
 * @property link an app-defined destination for an entry without sections.
 * @property sections the groups of links shown when the entry is expanded.
 * @property featured a card shown after the groups.
 */
@Immutable
public class MonarchMegaMenuItem(
    public val id: String,
    public val label: String,
    public val link: String? = null,
    public val sections: List<MonarchMegaMenuSection> = emptyList(),
    public val featured: MonarchMegaMenuFeatured? = null,
) {
    /** Whether the entry expands rather than navigates. */
    public val expandable: Boolean get() = sections.isNotEmpty() || featured != null
}
