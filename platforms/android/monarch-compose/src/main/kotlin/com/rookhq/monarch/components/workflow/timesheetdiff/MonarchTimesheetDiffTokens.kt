package com.rookhq.monarch.components.workflow.timesheetdiff

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** The kind of a line in a timesheet diff. */
internal enum class TimesheetDiffKind { Added, Removed, Changed }

/**
 * The colours of one line of a timesheet diff.
 *
 * @property container the tinted fill.
 * @property bar the bar at the start edge.
 * @property delta the change of time.
 * @property text the entry, or its new line.
 * @property struck the struck-through old or removed line.
 */
@Immutable
internal data class TimesheetDiffColors(
    val container: Color,
    val bar: Color,
    val delta: Color,
    val text: Color,
    val struck: Color,
)

/** Component tokens of MonarchTimesheetDiff, built from primitives and roles only. */
internal object MonarchTimesheetDiffTokens {
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val ItemGap: Dp = MonarchTokens.Space.space2.dp
    val LineGap: Dp = MonarchTokens.Space.space1.dp
    val ItemPadding: Dp = MonarchTokens.Space.space3.dp
    val BarWidth: Dp = (MonarchTokens.Space.space0_5 + MonarchTokens.Space.px).dp

    private const val MINUTES_PER_HOUR = 60
    private const val SEPARATOR = " · "
    private const val SPOKEN_SEPARATOR = ", "
    private const val MINUS = '−'

    /** The tone of each kind: success for added, danger for removed, warning for changed. */
    fun tone(kind: TimesheetDiffKind): MonarchTone = when (kind) {
        TimesheetDiffKind.Added -> MonarchTone.Success
        TimesheetDiffKind.Removed -> MonarchTone.Danger
        TimesheetDiffKind.Changed -> MonarchTone.Warning
    }

    /**
     * The colours of a line: the tone's subtle fill and solid bar, the
     * tone's strong colour for the change of time, the default text colour
     * and the muted colour for struck-through text.
     */
    fun colors(colors: MonarchColors, kind: TimesheetDiffKind): TimesheetDiffColors {
        val palette = MonarchTonePalette.of(tone(kind), colors)
        return TimesheetDiffColors(
            container = palette.subtle,
            bar = palette.solid,
            delta = palette.strong,
            text = colors.textDefault,
            struck = colors.textMuted,
        )
    }

    /** "1h 30m", "45m", "2h" or "0m", the web's compact form. */
    fun compact(minutes: Int): String {
        val total = kotlin.math.abs(minutes)
        val hours = total / MINUTES_PER_HOUR
        val rest = total % MINUTES_PER_HOUR
        return when {
            hours == 0 -> "${rest}m"
            rest == 0 -> "${hours}h"
            else -> "${hours}h ${rest}m"
        }
    }

    /** "+1h 30m", "−45m" or "0m". */
    fun signed(minutes: Int): String = when {
        minutes > 0 -> "+${compact(minutes)}"
        minutes < 0 -> "$MINUS${compact(minutes)}"
        else -> compact(0)
    }

    /** A duration in words in [locale]: "1 hour, 30 minutes". */
    fun spoken(minutes: Int, locale: Locale): String {
        val total = kotlin.math.abs(minutes)
        val hours = total / MINUTES_PER_HOUR
        val rest = total % MINUTES_PER_HOUR
        val format = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE)
        val hourMeasure = Measure(hours, MeasureUnit.HOUR)
        val minuteMeasure = Measure(rest, MeasureUnit.MINUTE)
        return when {
            hours == 0 -> format.formatMeasures(minuteMeasure)
            rest == 0 -> format.formatMeasures(hourMeasure)
            else -> format.formatMeasures(hourMeasure, minuteMeasure)
        }
    }

    /**
     * The parts of an entry's line, with the time written by [duration], joined by " · " for the eye or by
     * commas when [spoken]. Empty parts are left out.
     */
    fun line(
        entry: MonarchTimesheetDiffEntry,
        date: String,
        duration: (Int) -> String,
        noProject: String,
        nonBillable: String,
        spoken: Boolean = false,
    ): String = listOfNotNull(
        date,
        duration(entry.minutes),
        entry.project?.takeIf { it.isNotEmpty() } ?: noProject,
        entry.activity,
        entry.task,
        nonBillable.takeIf { entry.billable == false },
    ).filter { it.isNotEmpty() }.joinToString(if (spoken) SPOKEN_SEPARATOR else SEPARATOR)

    /** The visible names of [fields]: the app's name, else the built-in one, else the raw name. */
    fun fieldNames(fields: List<String>, names: Map<String, String>, builtIn: Map<String, String>): List<String> =
        fields.map { names[it] ?: builtIn[it] ?: it }
}
