package com.rookhq.monarch.components.layout.comparisonsection

import androidx.compose.runtime.Immutable

/**
 * A card under the table of a [MonarchComparisonSection] that says who an option suits.
 *
 * @property id the column the card is about.
 * @property title the title, such as "Product A suits".
 * @property description who it suits, in a sentence.
 * @property highlighted draws the card on the subtle accent, for the product being presented.
 */
@Immutable
public class MonarchComparisonBestFor(
    public val id: String,
    public val title: String,
    public val description: String,
    public val highlighted: Boolean = false,
)
