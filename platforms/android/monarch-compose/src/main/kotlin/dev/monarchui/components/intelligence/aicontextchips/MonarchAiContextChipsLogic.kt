package dev.monarchui.components.intelligence.aicontextchips

import android.icu.text.CompactDecimalFormat
import dev.monarchui.R
import dev.monarchui.internal.MonarchNumberText
import java.util.Locale

/** The pure logic of MonarchAiContextChips, apart from its colours and sizes. */
internal object MonarchAiContextChipsLogic {
    /** A token count in compact form in [locale]: 950, 1.2K, 18K, 1.1M. Negative counts are 0. */
    fun formatTokens(count: Int, locale: Locale): String {
        val format = CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT)
        format.maximumFractionDigits = 1
        return MonarchNumberText.upperScale(format.format(maxOf(0, count).toLong()))
    }

    /** The sum of the known token counts. */
    fun total(items: List<MonarchAiContextItem>): Int = items.sumOf { maxOf(0, it.tokens ?: 0) }

    /** Whether the total is shown: by default when any item has a count, and never with no items. */
    fun showsTotal(items: List<MonarchAiContextItem>, override: Boolean?): Boolean =
        items.isNotEmpty() && (override ?: items.any { it.tokens != null })

    /** The weight of an item against the budget, or against the total without one. `null` without a count. */
    fun weight(item: MonarchAiContextItem, total: Int, budget: Int?): MonarchAiContextWeight? =
        item.tokens?.let { MonarchAiContextWeight.of(it, budget ?: total) }

    /** Whether the item gets a remove action. */
    fun removable(item: MonarchAiContextItem, hasOnRemove: Boolean): Boolean = hasOnRemove && item.removable

    /** The built-in name of a kind. */
    fun kindName(kind: MonarchAiContextKind): Int = when (kind) {
        MonarchAiContextKind.File -> R.string.monarch_aicontextchips_kind_file
        MonarchAiContextKind.Selection -> R.string.monarch_aicontextchips_kind_selection
        MonarchAiContextKind.Page -> R.string.monarch_aicontextchips_kind_page
        MonarchAiContextKind.Record -> R.string.monarch_aicontextchips_kind_record
        MonarchAiContextKind.App -> R.string.monarch_aicontextchips_kind_app
        MonarchAiContextKind.Person -> R.string.monarch_aicontextchips_kind_person
    }

    /** What TalkBack reads for a chip: "File: contract.pdf, 1.2K tokens", then the description. */
    fun spokenLabel(kind: String, label: String, tokens: String?, description: String?): String {
        val named = "$kind: $label"
        return listOfNotNull(named, tokens, description?.takeIf { it.isNotBlank() }).joinToString(", ")
    }
}
