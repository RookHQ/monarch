package com.rookhq.monarch.components.identity.consentpreferences

import android.icu.text.DateFormat
import android.text.format.DateUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.identity.consenthistory.MonarchConsentMethod
import com.rookhq.monarch.components.identity.consenthistory.MonarchConsentRecord
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Date
import java.util.Locale

/** Component tokens and the pure logic of MonarchConsentPreferences. */
internal object MonarchConsentPreferencesTokens {
    /** How long a saved decision shows a relative time before it shows its date. */
    const val RELATIVE_FOR_MILLIS: Long = DateUtils.WEEK_IN_MILLIS

    /** The separator between the parts of the record line. */
    const val SEPARATOR: String = " · "

    /** The gap between the sections. */
    val SectionGap: Dp = MonarchTokens.Space.space4.dp

    /** The gap between the title and the description, and between the lines of a category. */
    val TextGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between buttons. */
    val ButtonGap: Dp = MonarchTokens.Space.space2.dp

    /** Whether the person cannot change [category]: it is required, or a privacy signal keeps it off. */
    fun locked(category: MonarchConsentPreferencesCategory, globalPrivacyControl: Boolean): Boolean =
        category.required || keptOffBySignal(category, globalPrivacyControl)

    /** Whether a Global Privacy Control signal keeps [category] off. */
    fun keptOffBySignal(category: MonarchConsentPreferencesCategory, globalPrivacyControl: Boolean): Boolean =
        globalPrivacyControl && category.saleOrSharing && !category.required

    /** The state of [category] given the person's [wanted] state: required ones on, signal-locked ones off. */
    fun effective(
        category: MonarchConsentPreferencesCategory,
        wanted: Boolean,
        globalPrivacyControl: Boolean,
    ): Boolean = when {
        category.required -> true
        keptOffBySignal(category, globalPrivacyControl) -> false
        else -> wanted
    }

    /**
     * Every category's state for a decision made with [method]: everything on
     * for accept all, everything off for reject all and withdrawn, and the
     * switches in [draft] (off when missing) for a custom choice, always with
     * the locks applied.
     */
    fun decision(
        method: MonarchConsentMethod,
        categories: List<MonarchConsentPreferencesCategory>,
        draft: Map<String, Boolean>,
        globalPrivacyControl: Boolean,
    ): Map<String, Boolean> = categories.associate { category ->
        val wanted = when (method) {
            MonarchConsentMethod.AcceptAll -> true
            MonarchConsentMethod.RejectAll, MonarchConsentMethod.Withdrawn -> false
            else -> draft[category.id] ?: false
        }
        category.id to effective(category, wanted, globalPrivacyControl)
    }

    /** Whether Withdraw consent is offered: categories mode, allowed, and a saved decision not already withdrawn. */
    fun offersWithdraw(
        mode: MonarchConsentPreferencesMode,
        allowWithdraw: Boolean,
        record: MonarchConsentRecord?,
    ): Boolean {
        val withdrawable = record != null && record.method != MonarchConsentMethod.Withdrawn
        return mode == MonarchConsentPreferencesMode.Categories && allowWithdraw && withdrawable
    }

    /**
     * When the decision was saved: a relative time up to a week from [now], the date after that, as the web
     * (`absoluteAfter` of a week).
     */
    fun savedTime(date: Long, now: Long, locale: Locale): String =
        if (MonarchRelativeTime.showsAbsolute(date, now, RELATIVE_FOR_MILLIS)) {
            DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(date))
        } else {
            MonarchRelativeTime.format(date, now, locale)
        }

    /** The record line, "Policy version 3 · Saved 2 days ago". */
    fun recordLine(policyVersionLabel: String, version: String, savedLabel: String, time: String): String =
        "$policyVersionLabel $version$SEPARATOR$savedLabel $time"

    /** The colour of a category's status word in choice mode: success strong when on, muted otherwise. */
    fun statusColor(on: Boolean, colors: MonarchColors): Color = if (on) colors.successStrong else colors.textMuted
}
