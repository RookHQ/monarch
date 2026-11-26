package com.rookhq.monarch.components.files.versionhistorypanel

import android.icu.text.DateFormat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.cancellation.CancellationException

/** Component tokens and pure logic of MonarchVersionHistoryPanel, built from primitives and roles only. */
internal object MonarchVersionHistoryPanelTokens {
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val SmallGap: Dp = MonarchTokens.Space.space2.dp
    val Inset: Dp = MonarchTokens.Space.space4.dp
    val PlaceholderGap: Dp = MonarchTokens.Space.space2.dp

    /** The longest name a version can be saved with, as on iOS. */
    const val NOTE_MAX_LENGTH: Int = 200

    /** The widths of the placeholder lines, as fractions of the panel. */
    val PlaceholderLines: List<Float> = listOf(0.8f, 0.64f, 0.72f)


    /** The version that matches the content now: as given, else the newest (the first). */
    fun currentId(currentId: String?, versions: List<MonarchVersionHistoryPanelVersion>): String? =
        currentId ?: versions.firstOrNull()?.id

    /** The version on screen, or `null` for the list (also when the selection is not a version). */
    fun selected(
        selection: String?,
        versions: List<MonarchVersionHistoryPanelVersion>,
    ): MonarchVersionHistoryPanelVersion? = selection?.let { id -> versions.firstOrNull { it.id == id } }

    /** The name to save with: trimmed and at most [NOTE_MAX_LENGTH] characters. */
    fun note(text: String): String = text.trim().take(NOTE_MAX_LENGTH)

    /** The version's name in the list: its note, "Saved automatically", or nothing. */
    fun name(version: MonarchVersionHistoryPanelVersion, automatic: String): String? =
        version.note?.takeIf { it.isNotBlank() } ?: automatic.takeIf { version.automatic }

    /** What TalkBack reads for a row or the byline: the parts that exist, joined by commas. */
    fun spoken(parts: List<String?>): String = parts.filterNot { it.isNullOrBlank() }.joinToString(", ")

    /** Whether a version's lines match the content now. */
    fun identical(selectedLines: List<String>, currentLines: List<String>): Boolean = selectedLines == currentLines

    /** The message of a failure, or [fallback] when it has none. A cancellation is passed on. */
    fun failureText(error: Throwable, fallback: String): String {
        if (error is CancellationException) throw error
        return error.message?.takeIf { it.isNotBlank() } ?: fallback
    }

    /** [time] relative to [now] in [locale], by the shared Monarch rule (the web rule): "20 minutes ago". */
    fun relativeTime(time: Long, now: Long, locale: Locale): String = MonarchRelativeTime.format(time, now, locale)

    /** The full date and time written on a version's screen: "Nov 3, 2026, 2:00 PM". */
    fun date(time: Long, locale: Locale, timeZone: TimeZone): String {
        val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        format.timeZone = android.icu.util.TimeZone.getTimeZone(timeZone.id)
        return format.format(time)
    }
}

/** The colour roles of MonarchVersionHistoryPanel. */
internal object MonarchVersionHistoryPanelColors {
    fun titleColor(colors: MonarchColors): Color = colors.textDefault

    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong
}
