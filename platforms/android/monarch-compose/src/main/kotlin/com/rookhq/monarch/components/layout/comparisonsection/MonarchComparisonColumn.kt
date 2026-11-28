package com.rookhq.monarch.components.layout.comparisonsection

import androidx.compose.runtime.Immutable

/**
 * One option compared by a [MonarchComparisonSection]: the product or an alternative.
 *
 * @property id identifies the column: the key of row values and of best-for cards.
 * @property name the name of the option.
 * @property highlighted draws the column with the accent, for the product being presented.
 * @property badge a short badge under the name, such as "This product".
 */
@Immutable
public class MonarchComparisonColumn(
    public val id: String,
    public val name: String,
    public val highlighted: Boolean = false,
    public val badge: String? = null,
)
