package com.rookhq.monarch.components.identity.consenthistory

import android.icu.text.DateFormat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Date
import java.util.Locale

/** One category of a record, in display order. */
internal data class MonarchConsentHistoryEntry(val label: String, val allowed: Boolean)

/** Component tokens and the pure logic of MonarchConsentHistory. */
internal object MonarchConsentHistoryTokens {
    /** The gap between the header, the records and the button. */
    val SectionGap: Dp = MonarchTokens.Space.space4.dp

    /** The gap between records. */
    val RecordGap: Dp = MonarchTokens.Space.space3.dp

    /** The padding inside a record. */
    val RecordPadding: Dp = MonarchTokens.Space.space4.dp

    /** The gap between the lines of a record. */
    val LineGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap inside a line. */
    val TextGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The width of the label column of a record. */
    val LabelWidth: Dp = MonarchTokens.Space.space24.dp

    /** The colours of a record: the surface with a subtle border. */
    fun recordColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.borderSubtle)

    /** The colour of a category's state: the success colour when allowed, muted when not. */
    fun categoryColor(allowed: Boolean, colors: MonarchColors): Color =
        if (allowed) colors.successStrong else colors.textMuted

    /** The records newest first. Records with the same date keep their order. */
    fun sorted(records: List<MonarchConsentRecord>): List<MonarchConsentRecord> =
        records.sortedByDescending { it.dateMillis }

    /**
     * The categories of a record in display order: the listed [categories]
     * first, in their order and with their names, then ids that are not
     * listed, sorted, shown as the raw id.
     */
    fun entries(
        record: MonarchConsentRecord,
        categories: List<MonarchConsentCategory>,
    ): List<MonarchConsentHistoryEntry> {
        val listed = categories.mapNotNull { category ->
            record.categories[category.id]?.let { MonarchConsentHistoryEntry(category.label, it) }
        }
        val known = categories.map { it.id }.toSet()
        val unlisted = record.categories.keys.filterNot { it in known }.sorted().map {
            MonarchConsentHistoryEntry(it, record.categories.getValue(it))
        }
        return listed + unlisted
    }

    /** The default date format: medium date and short time in [locale] and the device's time zone. */
    fun formatDate(millis: Long, locale: Locale): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(millis))

    /** [date] relative to [now] by the shared Monarch rule (the web rule): "30 seconds ago", "5 minutes ago". */
    fun relativeTime(date: Long, now: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(date, now, locale)

    /** The built-in name of a method. */
    fun methodName(method: MonarchConsentMethod): Int = when (method) {
        MonarchConsentMethod.AcceptAll -> R.string.monarch_consenthistory_method_accept_all
        MonarchConsentMethod.RejectAll -> R.string.monarch_consenthistory_method_reject_all
        MonarchConsentMethod.Custom -> R.string.monarch_consenthistory_method_custom
        MonarchConsentMethod.GlobalPrivacyControl -> R.string.monarch_consenthistory_method_global_privacy_control
        MonarchConsentMethod.Acknowledged -> R.string.monarch_consenthistory_method_acknowledged
        MonarchConsentMethod.Withdrawn -> R.string.monarch_consenthistory_method_withdrawn
    }

    /** The built-in name of a regime. */
    fun regimeName(regime: MonarchConsentRegime): Int = when (regime) {
        MonarchConsentRegime.OptIn -> R.string.monarch_consenthistory_regime_opt_in
        MonarchConsentRegime.OptOut -> R.string.monarch_consenthistory_regime_opt_out
        MonarchConsentRegime.Notice -> R.string.monarch_consenthistory_regime_notice
    }

    /** What TalkBack reads for a line: its name, its value and any note. */
    fun spokenLine(name: String, value: String, note: String?): String =
        listOfNotNull(name, value, note).joinToString(", ")
}
