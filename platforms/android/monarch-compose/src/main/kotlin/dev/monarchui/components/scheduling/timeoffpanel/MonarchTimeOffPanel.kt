package dev.monarchui.components.scheduling.timeoffpanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.feedback.skeleton.MonarchSkeleton
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchRemovalFocus
import dev.monarchui.internal.monarchRemovalFallback
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchTimeZone
import dev.monarchui.internal.rememberMonarchRemovalFocus
import dev.monarchui.theme.MonarchTheme

/**
 * Lists time off (holidays, sickness, training, closures) and adds or
 * removes it: an Include past switch, an add sheet (who, type, all day or
 * timed, dates, reason, validation) and a danger confirmation to remove.
 * Data in, callbacks out: the app saves.
 *
 * Built as a `LazyColumn` of Monarch rows with `MonarchCheckboxRow`,
 * `MonarchBadge`, `MonarchEmptyState`, `MonarchBanner` and `MonarchSkeleton`.
 * Adding opens a Material 3 `ModalBottomSheet` with `MonarchSelect`s, the
 * Material 3 `DatePickerDialog` and `TimePickerDialog`, and a
 * `MonarchTextField`; removing asks in a Material 3 `AlertDialog`. The panel
 * is the screen's scrolling list: give it the height left, never put it in
 * a vertically scrolling parent.
 *
 * All-day entries are days as their UTC midnight, as in Material 3
 * `DatePickerState`; timed entries are instants shown and entered in
 * [timeZone]. The title is a heading; each entry is one TalkBack element
 * with its remove button named after it ("Remove time off for Alex Kim,
 * …"); kinds, states and past entries are badges in words.
 *
 * @param entries the periods to list, in display order.
 * @param modifier the modifier of the list.
 * @param onAdd adds the time off and shows the add button. The sheet stays open and busy while it runs; throw to
 * show the error in the sheet.
 * @param onRemove removes an entry after the confirmation and shows each entry's remove button. The confirmation
 * stays open and busy while it runs; throw to show the error.
 * @param canRemove which entries may be removed. `null` allows all when [onRemove] is given.
 * @param people the people the add sheet offers; with one person (and no closure) Who is hidden.
 * @param allowClosure adds Everyone to Who: a closure that blocks bookings for everyone.
 * @param types the kinds the add sheet offers, for example leave types. Empty hides Type.
 * @param showPast whether past entries are listed. `null` lets the panel keep it, starting with `false`.
 * @param onShowPastChange called when Include past changes, for example to load past entries.
 * @param loading shows placeholders instead of the list.
 * @param error a message shown in a danger banner instead of the list.
 * @param onRetry shows a retry button in the error banner.
 * @param timeZone the zone timed entries are shown and entered in. An IANA id such as `"Europe/Berlin"`. `null`, or an
 * id the device does not know, is the device's zone.
 * @param now the instant that decides which entries are past. `null` is the time of the first composition; pass a
 * fixed value in tests.
 * @param is24Hour shows times on a 24-hour clock. `null` follows the device setting.
 * @param state the scroll state of the list.
 * @param addIcon an icon before the text of the add button.
 * @param removeIcon the glyph of each remove button. `null` shows the buttons as text.
 * @param notice content above the list, for example a `MonarchBanner` saying appointments fall in a period.
 * @param labels replacements for the built-in strings.
 */
@ExperimentalMonarchApi
@Suppress("LongParameterList", "LongMethod")
@Composable
public fun MonarchTimeOffPanel(
    entries: List<MonarchTimeOffEntry>,
    modifier: Modifier = Modifier,
    onAdd: (suspend (MonarchTimeOffDraft) -> Unit)? = null,
    onRemove: (suspend (MonarchTimeOffEntry) -> Unit)? = null,
    canRemove: ((MonarchTimeOffEntry) -> Boolean)? = null,
    people: List<MonarchSelectOption<String>> = emptyList(),
    allowClosure: Boolean = false,
    types: List<MonarchSelectOption<String>> = emptyList(),
    showPast: Boolean? = null,
    onShowPastChange: ((Boolean) -> Unit)? = null,
    loading: Boolean = false,
    error: String? = null,
    onRetry: (() -> Unit)? = null,
    timeZone: String? = null,
    now: Long? = null,
    is24Hour: Boolean? = null,
    state: LazyListState = rememberLazyListState(),
    addIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Plus,
    removeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Trash,
    notice: (@Composable () -> Unit)? = null,
    labels: MonarchTimeOffPanelLabels = MonarchTimeOffPanelLabels(),
) {
    val tokens = MonarchTimeOffPanelTokens
    val zone = monarchTimeZone(timeZone)
    val instant = now ?: remember { System.currentTimeMillis() }
    val locale = LocalConfiguration.current.locales[0]
    val twentyFour = is24Hour ?: android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val formats = remember(locale, twentyFour, zone) { TimeOffPanelFormats(locale, twentyFour, zone) }
    var ownShowPast by rememberSaveable { mutableStateOf(false) }
    val past = showPast ?: ownShowPast
    var adding by rememberSaveable { mutableStateOf(false) }
    var removing by remember { mutableStateOf<MonarchTimeOffEntry?>(null) }
    val visible = tokens.visible(entries, past, instant, zone)
    // As on the web: after a removal focus goes to the next entry's remove button, else the previous one's, else
    // the first control of the panel (Add, or the past switch).
    val removalFocus = rememberMonarchRemovalFocus(visible.map { it.id })
    val fallbacks = TimeOffPanelFallbacks.of(removalFocus, hasAdd = onAdd != null)
    val allDayText = monarchString(labels.allDay, R.string.monarch_timeoffpanel_all_day)
    val everyone = monarchString(labels.everyone, R.string.monarch_timeoffpanel_everyone)
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = state,
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        item(key = "header") {
            Header(labels, addIcon, onAdd?.let { { adding = true } }, fallbacks.add)
        }
        if (notice != null) item(key = "notice") { notice() }
        item(key = "past") {
            MonarchCheckboxRow(
                checked = past,
                onCheckedChange = {
                    ownShowPast = it
                    onShowPastChange?.invoke(it)
                },
                label = monarchString(labels.includePast, R.string.monarch_timeoffpanel_include_past),
                modifier = fallbacks.switch,
            )
        }
        if (loading || error != null || visible.isEmpty()) {
            item(key = "status") { Status(loading, error, onRetry, labels) }
        } else {
            items(visible, key = { it.id }) { entry ->
                TimeOffPanelRow(
                    entry = entry,
                    who = timeOffPanelWho(entry, everyone),
                    period = formats.period(entry, allDayText),
                    past = tokens.isPast(entry, instant, zone),
                    onRemove = { removing = entry }.takeIf { tokens.removable(entry, onRemove != null, canRemove) },
                    removeFocus = removalFocus.requester(entry.id),
                    removeIcon = removeIcon,
                    labels = labels,
                )
            }
        }
    }
    if (adding && onAdd != null) {
        TimeOffPanelAddSheet(
            setup = TimeOffPanelSheetSetup(people, allowClosure, types, zone, tokens.dayOf(instant, zone), twentyFour),
            formats = formats,
            labels = labels,
            onAdd = onAdd,
            onDismiss = { adding = false },
        )
    }
    val entry = removing
    if (entry != null && onRemove != null) {
        TimeOffPanelRemoveDialog(
            summary = listOf(timeOffPanelWho(entry, everyone), formats.period(entry, allDayText))
                .filter { it.isNotEmpty() }
                .joinToString(" · "),
            labels = labels,
            onRemove = { removalFocus.remove(visible.map { it.id }, entry.id) { onRemove(entry) } },
            onDismiss = { removing = null },
        )
    }
}

/** Where focus goes when the last entry is removed: Add when there is one, else the past switch. */
private class TimeOffPanelFallbacks(val add: FocusRequester?, val switch: Modifier) {
    companion object {
        fun of(focus: MonarchRemovalFocus<String>, hasAdd: Boolean): TimeOffPanelFallbacks =
            if (hasAdd) {
                TimeOffPanelFallbacks(focus.fallback, Modifier)
            } else {
                TimeOffPanelFallbacks(null, Modifier.monarchRemovalFallback(focus))
            }
    }
}

/** The name shown for whose time off [entry] is: the person, or [everyone] for a closure. */
private fun timeOffPanelWho(entry: MonarchTimeOffEntry, everyone: String): String =
    if (entry.personId == null) everyone else entry.personName.orEmpty()

/** Placeholders while loading, the error with a retry button, or the empty state. */
@Composable
private fun Status(loading: Boolean, error: String?, onRetry: (() -> Unit)?, labels: MonarchTimeOffPanelLabels) {
    when {
        loading -> Placeholders(labels)
        error != null -> MonarchBanner(
            message = error,
            tone = MonarchTone.Danger,
            action = retryAction(onRetry, labels),
        )
        else -> MonarchEmptyState(
            title = monarchString(labels.emptyTitle, R.string.monarch_timeoffpanel_empty_title),
            size = MonarchEmptyStateSize.Sm,
            message = monarchString(labels.emptyDescription, R.string.monarch_timeoffpanel_empty_description),
        )
    }
}

@Composable
private fun Header(
    labels: MonarchTimeOffPanelLabels,
    addIcon: (@Composable () -> Unit)?,
    onAdd: (() -> Unit)?,
    addFocus: FocusRequester?,
) {
    val colors = MonarchTheme.colors
    // As the web: the title and the description at the start, Add time off at the end of the header.
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchTimeOffPanelTokens.RowGap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MonarchTimeOffPanelTokens.RowGap)) {
            Text(
                text = monarchString(labels.title, R.string.monarch_timeoffpanel_title),
                modifier = Modifier.semantics { heading() },
                style = MonarchTheme.typography.title,
                color = MonarchTimeOffPanelTokens.text(colors),
            )
            Text(
                text = monarchString(labels.description, R.string.monarch_timeoffpanel_description),
                style = MonarchTheme.typography.body,
                color = MonarchTimeOffPanelTokens.muted(colors),
            )
        }
        if (onAdd != null) {
            MonarchButton(
                text = monarchString(labels.add, R.string.monarch_timeoffpanel_add),
                onClick = onAdd,
                modifier = if (addFocus != null) Modifier.focusRequester(addFocus) else Modifier,
                variant = MonarchButtonVariant.Primary,
                size = MonarchButtonSize.Sm,
                leadingIcon = addIcon,
            )
        }
    }
}

@Composable
private fun Placeholders(labels: MonarchTimeOffPanelLabels) {
    val name = monarchString(labels.loading, R.string.monarch_timeoffpanel_loading)
    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = name },
        verticalArrangement = Arrangement.spacedBy(MonarchTimeOffPanelTokens.Gap),
    ) {
        repeat(MonarchTimeOffPanelTokens.PLACEHOLDERS) {
            MonarchSkeleton(modifier = Modifier.fillMaxWidth().height(MonarchTimeOffPanelTokens.PlaceholderHeight))
        }
    }
}

/** The retry button of the error banner, or `null` without [onRetry]. */
private fun retryAction(onRetry: (() -> Unit)?, labels: MonarchTimeOffPanelLabels): (@Composable () -> Unit)? =
    if (onRetry == null) {
        null
    } else {
        {
            MonarchButton(
                text = monarchString(labels.retry, R.string.monarch_timeoffpanel_retry),
                onClick = onRetry,
                size = MonarchButtonSize.Sm,
            )
        }
    }
