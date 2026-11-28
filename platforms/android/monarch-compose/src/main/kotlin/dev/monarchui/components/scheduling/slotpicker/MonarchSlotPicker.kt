package dev.monarchui.components.scheduling.slotpicker

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.datadisplay.tag.MonarchChip
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.feedback.skeleton.MonarchSkeleton
import dev.monarchui.components.scheduling.minimonth.MonarchMiniMonth
import dev.monarchui.components.scheduling.timezoneselect.MonarchTimezoneSelect
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Pick a day on a month calendar, then one of that day's open times, shown
 * in the visitor's time zone with an optional zone picker. Data in, choices
 * out: the app loads [slots] for the chosen [day]; the picker never fetches.
 *
 * Built from `MonarchMiniMonth` for the days, `MonarchChip`s for
 * the times, `MonarchSkeleton` while loading, `MonarchBanner` for errors and
 * a selection that is no longer open, and `MonarchTimezoneSelect` for the
 * zone. Days are epoch milliseconds of their UTC midnight, as in Material 3
 * `DatePickerState`; times are instants shown in [timeZone], so a slot means
 * the same moment for host and visitor.
 *
 * The day's title is a heading; the times are a group named "Available
 * times on …" in which the chosen time is selected and taken times are
 * disabled; loading and the number of times found are announced through a
 * polite live region that is always present.
 *
 * @param slots the times of [day], in display order.
 * @param selection the chosen slot's start, or `null`.
 * @param onSelectionChange called with the chosen slot's start, or `null` when it is cleared.
 * @param day the day whose times are shown, as its UTC midnight, or `null` before one is chosen.
 * @param onDayChange called with the day the person picks. The selection is not cleared for you.
 * @param timeZone the IANA id of the zone the times are shown in, for example the visitor's.
 * @param previousMonthIcon the icon of the calendar's previous-month button; Monarch bundles no icons.
 * @param nextMonthIcon the icon of the calendar's next-month button.
 * @param modifier the modifier of the picker.
 * @param allowDeselect lets a second press on the chosen time clear it.
 * @param visibleMonth any day of the month on screen. `null` lets the calendar keep it.
 * @param onVisibleMonthChange called with the first day of the month the person moves to, for example to load
 * its bookable days.
 * @param availableDays the bookable days; when given, every other day is disabled.
 * @param isDayDisabled returns `true` for more days that cannot be chosen, for example days known to be full.
 * @param minimumDate the first bookable day.
 * @param maximumDate the last bookable day, the booking window.
 * @param today today's date for the calendar's marker. `null` is today on the device; pass a fixed value in tests.
 * @param firstDayOfWeek the first column, `Calendar.SUNDAY` to `Calendar.SATURDAY`. `null` follows the locale.
 * @param is24Hour shows times on a 24-hour clock. `null` follows the device setting.
 * @param onTimeZoneChange shows a time zone picker and is called with the new zone. `null` shows the zone as text.
 * @param pinnedTimeZones zones listed first in the picker, for example the business's offices.
 * @param businessTimeZone the zone the business works in. When it differs from [timeZone], a note says times
 * are converted.
 * @param loading shows placeholders instead of the times and announces that times are loading.
 * @param error a message shown in a danger banner instead of the times.
 * @param onRetry shows a retry button in the error banner.
 * @param onChooseOutside offers a way out when no time fits, in the empty state and in the warning when the
 * selection is not open.
 * @param enabled whether days and times can be chosen.
 * @param labels replacements for the built-in strings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongParameterList", "LongMethod")
@Composable
public fun MonarchSlotPicker(
    slots: List<MonarchSlotPickerSlot>,
    selection: Long?,
    onSelectionChange: (Long?) -> Unit,
    day: Long?,
    onDayChange: (Long) -> Unit,
    timeZone: String,
    previousMonthIcon: @Composable () -> Unit,
    nextMonthIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    allowDeselect: Boolean = false,
    visibleMonth: Long? = null,
    onVisibleMonthChange: ((Long) -> Unit)? = null,
    availableDays: Set<Long>? = null,
    isDayDisabled: ((Long) -> Boolean)? = null,
    minimumDate: Long? = null,
    maximumDate: Long? = null,
    today: Long? = null,
    firstDayOfWeek: Int? = null,
    is24Hour: Boolean? = null,
    onTimeZoneChange: ((String) -> Unit)? = null,
    pinnedTimeZones: List<String> = emptyList(),
    businessTimeZone: String? = null,
    loading: Boolean = false,
    error: String? = null,
    onRetry: (() -> Unit)? = null,
    onChooseOutside: (() -> Unit)? = null,
    enabled: Boolean = true,
    labels: MonarchSlotPickerLabels = MonarchSlotPickerLabels(),
) {
    val tokens = MonarchSlotPickerTokens
    val locale = LocalConfiguration.current.locales[0]
    val twentyFour = is24Hour ?: android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val formats = remember(locale, twentyFour, timeZone) { SlotPickerFormats(locale, twentyFour, timeZone) }
    val dayLabel = day?.let(formats::day).orEmpty()
    val restricted = availableDays != null || isDayDisabled != null
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        MonarchMiniMonth(
            selection = day,
            onSelectionChange = onDayChange,
            previousMonthIcon = previousMonthIcon,
            nextMonthIcon = nextMonthIcon,
            visibleMonth = visibleMonth,
            onVisibleMonthChange = onVisibleMonthChange,
            minimumDate = minimumDate,
            maximumDate = maximumDate,
            today = today,
            firstDayOfWeek = firstDayOfWeek,
            enabled = enabled,
            isDateDisabled = if (restricted) {
                { !tokens.dayEnabled(it, availableDays, isDayDisabled) }
            } else {
                null
            },
            labels = labels.miniMonth,
        )
        Column(verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
            Status(status(day, loading, error, slots, dayLabel, labels))
            if (day == null) {
                Muted(monarchString(labels.chooseDay, R.string.monarch_slotpicker_choose_day))
            } else {
                Text(
                    text = dayLabel,
                    modifier = Modifier.semantics { heading() },
                    style = MonarchTheme.typography.bodyStrong,
                    color = tokens.heading(MonarchTheme.colors),
                )
                when {
                    error != null -> MonarchBanner(
                        message = error,
                        tone = MonarchTone.Danger,
                        action = retryAction(onRetry, labels),
                    )
                    loading -> Placeholders()
                    slots.isEmpty() -> {
                        Muted(monarchString(labels.empty, R.string.monarch_slotpicker_empty))
                        ChooseOutside(onChooseOutside, labels)
                    }
                    else -> Times(
                        slots = slots,
                        selection = selection,
                        allowDeselect = allowDeselect,
                        enabled = enabled,
                        dayLabel = dayLabel,
                        formats = formats,
                        labels = labels,
                        onSelectionChange = onSelectionChange,
                    )
                }
                if (!loading && error == null) NotOpen(selection, day, slots, formats, onChooseOutside, labels)
            }
        }
        Zone(timeZone, businessTimeZone, onTimeZoneChange, pinnedTimeZones, enabled, day ?: today, formats, labels)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongParameterList")
@Composable
private fun Times(
    slots: List<MonarchSlotPickerSlot>,
    selection: Long?,
    allowDeselect: Boolean,
    enabled: Boolean,
    dayLabel: String,
    formats: SlotPickerFormats,
    labels: MonarchSlotPickerLabels,
    onSelectionChange: (Long?) -> Unit,
) {
    val name = labels.times?.invoke(dayLabel) ?: stringResource(R.string.monarch_slotpicker_times, dayLabel)
    BoxWithConstraints {
        val gap = MonarchSlotPickerTokens.SlotGap
        val slotWidth = (maxWidth - gap * (MonarchSlotPickerTokens.SLOTS_PER_ROW - 1)) /
            MonarchSlotPickerTokens.SLOTS_PER_ROW
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = name }
                .selectableGroup(),
            maxItemsInEachRow = MonarchSlotPickerTokens.SLOTS_PER_ROW,
            horizontalArrangement = Arrangement.spacedBy(MonarchSlotPickerTokens.SlotGap),
            verticalArrangement = Arrangement.spacedBy(MonarchSlotPickerTokens.SlotGap),
        ) {
            slots.forEach { slot ->
                val selected = slot.start == selection
                SlotButton(
                    width = slotWidth,
                    selected = selected,
                    enabled = enabled && slot.enabled,
                    onClick = {
                        val next = MonarchSlotPickerTokens.next(selection, slot, allowDeselect)
                        if (next != selection) onSelectionChange(next)
                    },
                    time = formats.time(slot.start),
                    seats = slot.seatsLeft?.let { seats ->
                        labels.seatsLeft?.invoke(seats)
                            ?: pluralStringResource(R.plurals.monarch_slotpicker_seats_left, seats, seats)
                    },
                )
            }
        }
    }
}

/**
 * One time: a `MonarchChip` with two lines, the time over the seats left, the accent tint and border when chosen,
 * and the time struck through when taken. It is one choice of the group for TalkBack, with a 48dp target.
 */
@Composable
@Suppress("LongParameterList")
private fun SlotButton(
    width: Dp,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    time: String,
    seats: String?,
) {
    val tokens = MonarchSlotPickerTokens
    MonarchChip(
        selected = selected,
        onSelectedChange = { onClick() },
        modifier = Modifier.width(width).heightIn(min = tokens.SlotMinHeight),
        enabled = enabled,
        singleChoice = true,
    ) {
        Text(text = time, style = tokens.timeStyle(enabled, MonarchTheme.typography))
        if (seats != null) Text(text = seats, style = MonarchTheme.typography.caption)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Placeholders() {
    FlowRow(
        modifier = Modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(MonarchSlotPickerTokens.SlotGap),
        verticalArrangement = Arrangement.spacedBy(MonarchSlotPickerTokens.SlotGap),
    ) {
        repeat(MonarchSlotPickerTokens.PLACEHOLDERS) {
            MonarchSkeleton(
                modifier = Modifier.size(
                    MonarchSlotPickerTokens.PlaceholderWidth,
                    MonarchSlotPickerTokens.PlaceholderHeight,
                ),
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun Zone(
    timeZone: String,
    businessTimeZone: String?,
    onTimeZoneChange: ((String) -> Unit)?,
    pinned: List<String>,
    enabled: Boolean,
    at: Long?,
    formats: SlotPickerFormats,
    labels: MonarchSlotPickerLabels,
) {
    val instant = at ?: remember { System.currentTimeMillis() }
    Column(verticalArrangement = Arrangement.spacedBy(MonarchSlotPickerTokens.SectionGap)) {
        if (onTimeZoneChange != null) {
            MonarchTimezoneSelect(
                label = monarchString(labels.timeZone, R.string.monarch_slotpicker_time_zone),
                selected = timeZone,
                onSelectedChange = { zone -> zone?.let(onTimeZoneChange) },
                enabled = enabled,
                pinned = pinned,
                now = instant,
                labels = labels.timezoneSelect,
            )
        } else {
            val name = formats.zoneName(at = instant)
            Muted(labels.shownIn?.invoke(name) ?: stringResource(R.string.monarch_slotpicker_shown_in, name))
        }
        if (businessTimeZone != null && businessTimeZone != timeZone) {
            val name = formats.zoneName(businessTimeZone, instant)
            Muted(
                labels.convertedFrom?.invoke(name) ?: stringResource(R.string.monarch_slotpicker_converted_from, name),
            )
        }
    }
}

/** The retry button of the error banner, or `null` without [onRetry]. */
private fun retryAction(onRetry: (() -> Unit)?, labels: MonarchSlotPickerLabels): (@Composable () -> Unit)? =
    if (onRetry == null) {
        null
    } else {
        {
            MonarchButton(
                text = monarchString(labels.retry, R.string.monarch_slotpicker_retry),
                onClick = onRetry,
                size = MonarchButtonSize.Sm,
            )
        }
    }

/** The warning shown when [selection] is on [day] but none of its open times. */
@Suppress("LongParameterList")
@Composable
private fun NotOpen(
    selection: Long?,
    day: Long,
    slots: List<MonarchSlotPickerSlot>,
    formats: SlotPickerFormats,
    onChooseOutside: (() -> Unit)?,
    labels: MonarchSlotPickerLabels,
) {
    if (selection == null || !MonarchSlotPickerTokens.offList(selection, formats.dayOf(selection), day, slots)) return
    val time = formats.time(selection)
    MonarchBanner(
        message = labels.notOpen?.invoke(time) ?: stringResource(R.string.monarch_slotpicker_not_open, time),
        tone = MonarchTone.Warning,
        action = if (onChooseOutside != null) {
            { ChooseOutside(onChooseOutside, labels) }
        } else {
            null
        },
    )
}

@Composable
private fun ChooseOutside(onChooseOutside: (() -> Unit)?, labels: MonarchSlotPickerLabels) {
    if (onChooseOutside == null) return
    MonarchButton(
        text = monarchString(labels.chooseOutside, R.string.monarch_slotpicker_choose_outside),
        onClick = onChooseOutside,
        size = MonarchButtonSize.Sm,
    )
}

@Composable
private fun Muted(text: String) {
    Text(text = text, style = MonarchTheme.typography.body, color = MonarchSlotPickerTokens.muted(MonarchTheme.colors))
}

/** Always present, so loading and the number of times found are announced as changes of this live region. */
@Composable
private fun Status(text: String) {
    MonarchLiveStatus(text = text.takeIf { it.isNotEmpty() }, visible = false)
}

@Suppress("LongParameterList")
@Composable
private fun status(
    day: Long?,
    loading: Boolean,
    error: String?,
    slots: List<MonarchSlotPickerSlot>,
    dayLabel: String,
    labels: MonarchSlotPickerLabels,
): String {
    val open = MonarchSlotPickerTokens.openCount(slots)
    return when {
        day == null || error != null -> ""
        loading -> monarchString(labels.loading, R.string.monarch_slotpicker_loading)
        else -> labels.loaded?.invoke(open, dayLabel) ?: if (open == 0) {
            stringResource(R.string.monarch_slotpicker_loaded_none, dayLabel)
        } else {
            pluralStringResource(R.plurals.monarch_slotpicker_loaded, open, open, dayLabel)
        }
    }
}
