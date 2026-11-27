package com.rookhq.monarch.components.billing.savingspanel

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchSavingsPanel`. Every
 * `null` uses the built-in string named after it in `strings_savingspanel.xml`.
 *
 * @property title the card title when the app gives none: "Savings".
 * @property headline the headline, given the unused count, the formatted cost (or `null`) and the unpriced
 * count: "3 unused items cost $120.00 to renew (1 without a price).".
 * @property categories the name TalkBack gives the categories: "Unused by reason".
 * @property decisions read before the decision summary: "Decisions".
 * @property potentialSavings the savings line, given the formatted amounts:
 * "Letting the marked items lapse saves $120.00.".
 * @property items the name TalkBack gives the item list: "Unused items".
 * @property more the line after the list, given the number not shown: "and 4 more".
 * @property unpriced an item without a price: "No price".
 * @property emptyTitle the title of the empty state: "Nothing unused".
 * @property emptyMessage the message of the empty state: "Everything renewing is in use.".
 * @property errorTitle the title of the error state: "Savings could not be loaded.".
 * @property loading what TalkBack reads while loading: "Loading savings".
 */
@Immutable
public class MonarchSavingsPanelLabels(
    public val title: String? = null,
    public val headline: ((Int, String?, Int) -> String)? = null,
    public val categories: String? = null,
    public val decisions: String? = null,
    public val potentialSavings: ((String) -> String)? = null,
    public val items: String? = null,
    public val more: ((Int) -> String)? = null,
    public val unpriced: String? = null,
    public val emptyTitle: String? = null,
    public val emptyMessage: String? = null,
    public val errorTitle: String? = null,
    public val loading: String? = null,
)
