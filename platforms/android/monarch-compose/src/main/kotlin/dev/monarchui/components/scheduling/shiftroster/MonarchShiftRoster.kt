package dev.monarchui.components.scheduling.shiftroster

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.feedback.skeleton.MonarchSkeleton
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchTimeZone

/**
 * Who works which shift on which day: each person's shift, day off or nothing for every day of a range, with
 * the hours per person and the number of people working per day. Data in, changes out: the app saves each change.
 *
 * Built by Monarch from Monarch and Material 3 parts, because Material 3 has no roster. A grid of people by days
 * does not fit a phone, so the [Automatic][MonarchShiftRosterLayout.Automatic] layout shows the grid only when
 * every day column fits the width and the text is not enlarged much (a tablet); otherwise it shows one day at a
 * time, picked with a Material 3 `PrimaryScrollableTabRow`, with a row per person. A shift is a
 * `MonarchEventChip` bar in the shift's colour; a day off and a day with no assignment are written. Editable,
 * each cell is a button named "<person>, <day>: <assignment>" that opens a Material 3 `DropdownMenu` with the
 * shifts, Off and Clear.
 *
 * The roster does not scroll vertically: place it in a scrolling screen. Days are dates as day keys (the epoch
 * millisecond of their midnight in UTC, as in the Material 3 `DatePickerState`), formatted in the locale of the
 * configuration.
 *
 * @param members the people, in display order.
 * @param shifts the shifts that can be assigned, in menu order.
 * @param from the first day shown, any time on that date in UTC (a day key).
 * @param value the assignments: by member id, then by day key. A missing day is not assigned.
 * @param onValueChange called with the whole new value and the one change when a person picks a shift, Off or
 * Clear.
 * @param modifier the modifier of the roster.
 * @param days how many days to show from [from], 1 to 62. Seven by default.
 * @param layout the grid, one day at a time, one person at a time, or whichever fits.
 * @param enabled whether the cells can be used, for example `false` while saving. Tabs keep working.
 * @param readOnly shows the assignments as text, without menus.
 * @param loading shows a placeholder, read as "Loading roster", instead of the roster.
 * @param showTotals shows the hours per person and the number of people working per day.
 * @param today any time on the day marked Today, in epoch milliseconds, read in [timeZone], or `null` for no
 * marker. Pass a fixed value in tests and previews.
 * @param timeZone the zone [today] is read in. An IANA id such as `"Europe/Berlin"`. `null`, or an id the device does
 * not know, is the device's zone.
 * @param is24Hour shows times on a 24-hour clock. `null` follows the device setting.
 * @param error a message shown in a danger `MonarchBanner` instead of the roster.
 * @param onMemberClick makes each person a button, for example to open their schedule.
 * @param labels replacements for the built-in strings.
 * @param empty replaces the built-in empty state shown when [members] is empty.
 */
@ExperimentalMonarchApi
@Suppress("LongParameterList", "LongMethod")
@Composable
public fun MonarchShiftRoster(
    members: List<MonarchShiftRosterMember>,
    shifts: List<MonarchShiftRosterShift>,
    from: Long,
    value: Map<String, Map<Long, MonarchShiftRosterAssignment>>,
    onValueChange: (Map<String, Map<Long, MonarchShiftRosterAssignment>>, MonarchShiftRosterChange) -> Unit,
    modifier: Modifier = Modifier,
    days: Int = 7,
    layout: MonarchShiftRosterLayout = MonarchShiftRosterLayout.Automatic,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    loading: Boolean = false,
    showTotals: Boolean = true,
    today: Long? = System.currentTimeMillis(),
    timeZone: String? = null,
    is24Hour: Boolean? = null,
    error: String? = null,
    onMemberClick: ((MonarchShiftRosterMember) -> Unit)? = null,
    labels: MonarchShiftRosterLabels = MonarchShiftRosterLabels(),
    empty: (@Composable () -> Unit)? = null,
) {
    when {
        loading -> {
            val name = monarchString(labels.loading, R.string.monarch_shiftroster_loading)
            Box(modifier = modifier.fillMaxWidth().semantics { contentDescription = name }) {
                MonarchSkeleton(modifier = Modifier.height(MonarchShiftRosterTokens.PlaceholderHeight))
            }
            return
        }
        error != null -> {
            MonarchBanner(message = error, modifier = modifier.fillMaxWidth(), tone = MonarchTone.Danger)
            return
        }
        members.isEmpty() -> {
            Box(modifier = modifier.fillMaxWidth()) {
                if (empty != null) {
                    empty()
                } else {
                    MonarchEmptyState(
                        title = monarchString(labels.emptyTitle, R.string.monarch_shiftroster_empty_title),
                        size = MonarchEmptyStateSize.Sm,
                        message = monarchString(
                            labels.emptyDescription,
                            R.string.monarch_shiftroster_empty_description,
                        ),
                    )
                }
            }
            return
        }
    }
    val zone = monarchTimeZone(timeZone)
    val locale = LocalConfiguration.current.locales[0]
    val context = LocalContext.current
    val twentyFour = is24Hour ?: android.text.format.DateFormat.is24HourFormat(context)
    val formats = remember(locale, twentyFour) { ShiftRosterFormats(locale, twentyFour) }
    val range = remember(from, days) { ShiftRosterRules.range(from, days) }
    val model = ShiftRosterModel(
        members = members,
        shifts = shifts,
        range = range,
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        showTotals = showTotals,
        today = today?.let { ShiftRosterRules.localDay(it, zone) },
        onMemberClick = onMemberClick,
        text = shiftRosterText(labels),
        formats = formats,
    )
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val tokens = MonarchShiftRosterTokens
        val fits = tokens.fitsGrid(maxWidth, range.size, showTotals, LocalDensity.current.fontScale)
        when (tokens.resolve(layout, fits)) {
            MonarchShiftRosterLayout.Grid ->
                ShiftRosterGrid(model, tokens.personColumnWidth(maxWidth, range.size, showTotals))
            MonarchShiftRosterLayout.ByPerson -> ShiftRosterByPerson(model)
            else -> ShiftRosterByDay(model)
        }
    }
}
