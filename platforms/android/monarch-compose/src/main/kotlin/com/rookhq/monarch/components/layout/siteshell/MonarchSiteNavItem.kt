package com.rookhq.monarch.components.layout.siteshell

import androidx.compose.runtime.Immutable

/**
 * A top-level entry of the navigation of a [MonarchSiteShell]: a destination ([link]) or a group of [items] that
 * expands in the navigation drawer.
 *
 * @property label the name of the entry.
 * @property link an app-defined destination for an entry without items.
 * @property items the destinations of a group.
 */
@Immutable
public class MonarchSiteNavItem(
    public val label: String,
    public val link: String? = null,
    public val items: List<MonarchSiteNavLink> = emptyList(),
)
