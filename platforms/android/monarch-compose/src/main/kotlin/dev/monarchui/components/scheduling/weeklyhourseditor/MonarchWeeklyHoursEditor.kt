package dev.monarchui.components.scheduling.weeklyhourseditor

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchLiveStatus
import java.util.Calendar

private typealias Tokens = MonarchWeeklyHoursEditorTokens

/**
 * Edits weekly availability: each day switched on or off, one or more time intervals per day, copying a day to
 * the whole week, and overlap checks. The value is a plain list of [MonarchWeeklyHoursInterval]s in minutes.
 *
 * Built by Monarch as a `Column` of days. Each day is a `MonarchSwitchRow` named by the weekday; while it is on,
 * each interval is a pair of `MonarchSelect` menus (start and end, every [step] minutes, so there is nothing to
 * parse) with a remove button, followed by "Add interval" and "Copy to all" buttons. Switching a day on adds the
 * default interval, or restores the intervals it had when it was switched off. Problems found by
 * [MonarchWeeklyHoursInterval.validate] show as text under the day, announced through a polite live region; the
 * app decides whether to block saving.
 *
 * The editor is not scrollable and is about a screen tall per few days: put it in a scrolling screen.
 *
 * @param value the intervals. Days are ISO weekdays (1 Monday … 7 Sunday).
 * @param onValueChange called with the whole new value after every change, ordered by ISO weekday.
 * @param modifier the modifier of the editor.
 * @param firstDayOfWeek the first day shown, `Calendar.SUNDAY` to `Calendar.SATURDAY`. `null` follows the locale,
 * as the system calendar does (Monday in Germany, Sunday in the US).
 * @param is24Hour shows times on a 24-hour clock. `null` follows the device setting.
 * @param step the minutes between time options, 1 to 60.
 * @param defaultStart the start, in minutes, of a day switched on for the first time.
 * @param defaultEnd the end, in minutes, of a day switched on for the first time.
 * @param maxIntervals the most intervals per day; the add button is hidden at this count.
 * @param enabled whether the hours can be changed.
 * @param readOnly shows each day's hours as text, without controls.
 * @param timeZoneLabel content under the heading for the zone the hours are in: a text such as "Times in
 * Europe/London", or the app's time zone picker.
 * @param removeIcon the icon of each remove button. Monarch bundles no icons; without it the button reads
 * "Remove".
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchWeeklyHoursEditor(
    value: List<MonarchWeeklyHoursInterval>,
    onValueChange: (List<MonarchWeeklyHoursInterval>) -> Unit,
    modifier: Modifier = Modifier,
    firstDayOfWeek: Int? = null,
    is24Hour: Boolean? = null,
    step: Int = Tokens.DEFAULT_STEP,
    defaultStart: Int = Tokens.DEFAULT_START,
    defaultEnd: Int = Tokens.DEFAULT_END,
    maxIntervals: Int = Tokens.DEFAULT_MAX,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    timeZoneLabel: (@Composable () -> Unit)? = null,
    removeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchWeeklyHoursEditorLabels = MonarchWeeklyHoursEditorLabels(),
) {
    val resources = LocalResources.current
    val text = remember(labels, resources) { MonarchWeeklyHoursEditorText(labels, resources) }
    val locale = LocalConfiguration.current.locales[0]
    val weekStart = firstDayOfWeek ?: remember(locale) { Calendar.getInstance(locale).firstDayOfWeek }
    val context = LocalContext.current
    val twentyFour = is24Hour ?: DateFormat.is24HourFormat(context)
    val midnight = text.midnight
    val formats = remember(locale, twentyFour, midnight) {
        MonarchWeeklyHoursEditorFormats(locale, twentyFour, midnight)
    }
    val errors = remember(value) { MonarchWeeklyHoursInterval.validate(value) }
    // The intervals of a day switched off, restored if it is switched back on.
    val remembered = remember { HashMap<Int, List<MonarchWeeklyHoursInterval>>() }
    var announcement by remember { mutableStateOf("") }
    val stepMinutes = Tokens.step(step)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Tokens.DayGap)) {
        MonarchText(text = text.title, style = MonarchTextStyle.Subheading, heading = true)
        timeZoneLabel?.invoke()
        Tokens.order(weekStart).forEachIndexed { index, day ->
            if (index > 0) MonarchDivider(variant = MonarchDividerVariant.Subtle)
            key(day) {
                val ranges = Tokens.dayOf(value, day)
                val name = formats.day(day)
                MonarchWeeklyHoursEditorDay(
                    name = name,
                    ranges = ranges,
                    error = errors[day],
                    step = stepMinutes,
                    maxIntervals = maxIntervals,
                    enabled = enabled,
                    readOnly = readOnly,
                    formats = formats,
                    text = text,
                    removeIcon = removeIcon,
                    onToggle = { on ->
                        val next = if (on) {
                            remembered[day]?.takeIf { it.isNotEmpty() }
                                ?: listOf(MonarchWeeklyHoursInterval(day, defaultStart, defaultEnd))
                        } else {
                            remembered[day] = ranges
                            emptyList()
                        }
                        onValueChange(Tokens.withDay(value, day, next))
                    },
                    onRangesChange = { next ->
                        if (next.isEmpty()) remembered[day] = ranges
                        onValueChange(Tokens.withDay(value, day, next))
                    },
                    onAdd = {
                        val added = ranges + Tokens.nextInterval(ranges, day, stepMinutes)
                        onValueChange(Tokens.withDay(value, day, added))
                    },
                    onCopy = {
                        onValueChange(Tokens.copiedToAll(value, day))
                        val message = text.copied(name)
                        announcement = if (message == announcement) "$message " else message
                    },
                )
            }
        }
        // Always present, so each copy is announced once as a change of this live region.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}
