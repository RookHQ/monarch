package com.rookhq.monarch.components.layout.siteshell

import androidx.compose.runtime.Immutable

/**
 * A titled group of links of a [MonarchSiteFooter].
 *
 * @property title the title of the group, read as a heading.
 * @property links the destinations, in order.
 */
@Immutable
public class MonarchSiteFooterColumn(
    public val title: String,
    public val links: List<MonarchSiteNavLink>,
)
