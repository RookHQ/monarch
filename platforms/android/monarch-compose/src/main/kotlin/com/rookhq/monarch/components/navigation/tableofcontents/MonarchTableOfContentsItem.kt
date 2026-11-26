package com.rookhq.monarch.components.navigation.tableofcontents

import androidx.compose.runtime.Immutable

/**
 * One entry of a `MonarchTableOfContents`: a heading of the page.
 *
 * @property title the entry's text, usually the heading's text.
 * @property id the id of the section, passed back to the app to scroll to it.
 * @property level the heading level: 2 for a section, 3 for a subsection. A deeper level is indented one step
 * for each level below the shallowest entry.
 */
@Immutable
public class MonarchTableOfContentsItem(
    public val title: String,
    public val id: String,
    public val level: Int = 2,
)
