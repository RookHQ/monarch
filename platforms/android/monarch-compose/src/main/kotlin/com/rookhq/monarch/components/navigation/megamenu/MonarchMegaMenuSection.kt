package com.rookhq.monarch.components.navigation.megamenu

import androidx.compose.runtime.Immutable

/**
 * A group of links inside an expanded [MonarchMegaMenuItem].
 *
 * @property heading the small heading of the group, such as "Plan". `null` shows the links without a heading.
 * @property links the destinations, in reading order.
 * @property description a muted line under the heading.
 */
@Immutable
public class MonarchMegaMenuSection(
    public val heading: String?,
    public val links: List<MonarchMegaMenuLink>,
    public val description: String? = null,
)
