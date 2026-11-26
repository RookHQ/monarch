package com.rookhq.monarch.components.billing.subscriptionstable

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadgeTokens
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusRegistry
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Date
import java.util.Locale

/** Component tokens and the pure rules of MonarchSubscriptionsTable. */
internal object MonarchSubscriptionsTableTokens {
    /** The separator between the parts of a line. */
    const val SEPARATOR: String = " · "

    /** The dash of an empty cell in the wide table; TalkBack reads the row's words instead. */
    const val EMPTY: String = "\u2014"

    /** The statuses whose missing monthly figure means "not converted" rather than "not counted". */
    val LiveStatuses: Set<String> = setOf("active", "past_due")

    /** The padding above and below the content of a row. */
    val RowPadding: Dp = MonarchTokens.Space.space3.dp

    /** The gap between cells and between the two sides of a compact row. */
    val CellGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the lines of a cell. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** From this width on, the rows are a table with column headings. */
    val WideFrom: Dp = MonarchTokens.Breakpoint.sm.dp

    /** The weight of the name column of the table; the other columns have a weight of 1. */
    const val NAME_WEIGHT: Float = 2f

    /** The colour of secondary text: the plan line, the customer, the renewal. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the column headings. */
    fun headingColor(colors: MonarchColors): Color = colors.textMuted


    /** A date in epoch milliseconds (UTC), written as a medium date in [locale]. */
    fun dateText(millis: Long, locale: Locale): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(millis))

    /**
     * The status of [key]: from [registry], then from the registry provided to
     * the screen, else a neutral status labelled from the key ("past_due" gives "Past due").
     */
    fun status(key: String, registry: MonarchStatusRegistry, provided: MonarchStatusRegistry): MonarchStatusDefinition =
        registry.resolve(key) ?: provided.resolve(key)
            ?: MonarchStatusDefinition(MonarchStatusBadgeTokens.formatLabel(key) ?: key, MonarchTone.Neutral)

    /** Whether the monthly column shows: as asked, or when any subscription has a monthly figure. */
    fun monthlyShown(showMonthly: Boolean?, subscriptions: List<MonarchSubscription>): Boolean =
        showMonthly ?: subscriptions.any { it.monthly != null }

    /** Whether "Not converted" shows: the figure could not be converted and the subscription is live. */
    fun notConverted(subscription: MonarchSubscription): Boolean =
        subscription.monthly == MonarchSubscriptionMonthly.NotConverted && subscription.status in LiveStatuses

    /** The parts that exist, joined with the separator. */
    fun line(parts: List<String?>): String = parts.filterNot { it.isNullOrBlank() }.joinToString(SEPARATOR)
}
