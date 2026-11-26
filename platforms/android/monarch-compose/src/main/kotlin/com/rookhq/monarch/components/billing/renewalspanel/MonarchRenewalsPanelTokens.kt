package com.rookhq.monarch.components.billing.renewalspanel

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import android.text.format.DateUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadgeTokens
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusRegistry
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

/** Component tokens and the pure rules of MonarchRenewalsPanel. */
internal object MonarchRenewalsPanelTokens {
    /** The separator between the parts of a line. */
    const val SEPARATOR: String = " · "

    /** The statuses that count as open, so they can be due soon. */
    val OpenStatuses: Set<String> = setOf("open", "in_progress")

    /** The gap between the filters, the totals and the list, and between rows. */
    val SectionGap: Dp = MonarchTokens.Space.space4.dp

    /** The gap between the lines of a row and inside a total tile. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between badges, tiles and the parts of a line. */
    val ItemGap: Dp = MonarchTokens.Space.space2.dp

    /** The padding inside a total tile. */
    val TilePadding: Dp = MonarchTokens.Space.space3.dp

    /** The shape of a total tile and of a pressable row. */
    val TileShape = MonarchShapeDefaults.Control

    /** The colour of secondary text. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of an overdue next step; it also carries the "Overdue" prefix. */
    fun overdueColor(colors: MonarchColors): Color = colors.dangerStrong

    /** The fill of a total tile. */
    fun tileColor(colors: MonarchColors): Color = colors.bgSubtle


    /** A date in epoch milliseconds (UTC), written as a medium date in [locale]. */
    fun dateText(millis: Long, locale: Locale): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(millis))

    /** Whole days from [now] to the renewal: the item's own count, else worked out, else `null`. */
    fun daysUntil(item: MonarchRenewalItem, now: Long): Int? =
        item.daysUntil ?: item.renewalDateMillis?.let {
            ((it - now).toDouble() / DateUtils.DAY_IN_MILLIS).roundToLong().toInt()
        }

    /** Whether an item is open and its renewal is [urgentWithinDays] or fewer days away, or past. */
    fun urgent(item: MonarchRenewalItem, days: Int?, urgentWithinDays: Int): Boolean =
        item.status in OpenStatuses && days != null && days <= urgentWithinDays

    /** Whether any item has an owner or a next step, so every row shows the owner line. */
    fun ownerShown(items: List<MonarchRenewalItem>): Boolean =
        items.any { it.owner != null || it.nextAction != null || it.nextActionDateMillis != null }

    /**
     * The status of [key]: from [registry], then from the registry provided to
     * the screen, else a neutral status labelled from the key.
     */
    fun status(key: String, registry: MonarchStatusRegistry, provided: MonarchStatusRegistry): MonarchStatusDefinition =
        registry.resolve(key) ?: provided.resolve(key)
            ?: MonarchStatusDefinition(MonarchStatusBadgeTokens.formatLabel(key) ?: key, MonarchTone.Neutral)

    /** The parts that exist, joined with the separator, or `null` when there are none. */
    fun line(parts: List<String?>): String? =
        parts.filterNot { it.isNullOrBlank() }.takeIf { it.isNotEmpty() }?.joinToString(SEPARATOR)
}
