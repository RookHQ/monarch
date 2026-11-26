package com.rookhq.monarch.components.layout.marketingsection

import androidx.compose.runtime.Immutable

/**
 * One figure of a [MonarchStatStrip].
 *
 * @property value the figure, shown exactly as given, such as "13".
 * @property label what the figure counts, such as "Interface languages".
 */
@Immutable
public class MonarchStatStripItem(
    public val value: String,
    public val label: String,
)
