package dev.monarchui.components.billing.savingspanel

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.billing.money.MonarchMoneyFormat
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadgeTokens
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusRegistry
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Date
import java.util.Locale

/** Component tokens and the pure rules of MonarchSavingsPanel. */
internal object MonarchSavingsPanelTokens {
    /** The separator between the parts of a line. */
    const val SEPARATOR: String = " · "

    /** The separator between amounts in different currencies. */
    const val PLUS: String = " + "

    /** The gap between the headline, categories, decisions, savings and the list, and between rows. */
    val SectionGap: Dp = MonarchTokens.Space.space4.dp

    /** The gap between badges and between the parts of a row. */
    val ItemGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the lines of a row. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The shape of a pressable category or row. */
    val PressShape = MonarchShapeDefaults.Control

    /** The colour of secondary text. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the savings line, which is also written in words. */
    fun savingsColor(colors: MonarchColors): Color = colors.textDefault


    /** A date in epoch milliseconds (UTC), written as a medium date in [locale]. */
    fun dateText(millis: Long, locale: Locale): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(millis))

    /** Amounts in their own currencies joined with plus signs, or `null` when there are none: "$120.00 + €80.00". */
    fun join(amounts: List<MonarchSavingsAmount>, locale: Locale): String? =
        amounts.takeIf { it.isNotEmpty() }
            ?.joinToString(PLUS) { MonarchMoneyFormat.format(it.amount, it.currencyCode, locale) }

    /** The categories that are shown: those with items. */
    fun shownCategories(categories: List<MonarchSavingsCategory>): List<MonarchSavingsCategory> =
        categories.filter { it.count > 0 }

    /** The items listed and the number left out: at most [maxItems], never below zero. */
    fun shownItems(items: List<MonarchSavingsItem>, maxItems: Int): Pair<List<MonarchSavingsItem>, Int> {
        val shown = items.take(maxItems.coerceAtLeast(0))
        return shown to items.size - shown.size
    }

    /**
     * The status of [key]: from [registry], then from the registry provided to
     * the screen, else a neutral status labelled from the key.
     */
    fun status(
        key: String,
        registry: MonarchStatusRegistry?,
        provided: MonarchStatusRegistry,
    ): MonarchStatusDefinition =
        registry?.resolve(key) ?: provided.resolve(key)
            ?: MonarchStatusDefinition(MonarchStatusBadgeTokens.formatLabel(key) ?: key, MonarchTone.Neutral)

    /** The decisions as one line: "Keep 3 · Review 2". */
    fun decisionLine(decisions: List<MonarchSavingsDecisionCount>): String =
        decisions.joinToString(SEPARATOR) { "${it.label} ${it.count}" }
}
