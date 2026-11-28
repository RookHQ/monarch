package dev.monarchui.components.scheduling.businesshourseditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.forms.select.MonarchSelect
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursEditorTokens
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchRemoveButton
import dev.monarchui.theme.MonarchTheme

private typealias RowTokens = MonarchBusinessHoursEditorTokens

/**
 * One holiday or special date: its date, its name, closed or open at other times, the opening and closing
 * time, the error message and a remove button. Shown as a stack, the phone's form layout.
 */
@Composable
internal fun BusinessHoursEditorExceptionRow(
    position: Int,
    exception: MonarchBusinessHoursException,
    error: String?,
    setup: BusinessHoursEditorRowSetup,
    onChange: (MonarchBusinessHoursException) -> Unit,
    removeFocus: FocusRequester,
    onRemove: () -> Unit,
) {
    val text = setup.text
    val special = exception.intervals.isNotEmpty()
    val editable = setup.enabled && !setup.readOnly
    Column(verticalArrangement = Arrangement.spacedBy(RowTokens.ControlGap)) {
        BusinessHoursEditorDateField(
            date = BusinessHoursEditorDate(
                label = text.exceptionDate(position),
                value = exception.date,
                onValueChange = { onChange(exception.copy(date = it)) },
            ),
            enabled = setup.enabled,
            readOnly = setup.readOnly,
            labels = text.labels,
        )
        MonarchTextField(
            value = exception.name,
            onValueChange = { onChange(exception.copy(name = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = text.exceptionName(position),
            placeholder = text.exceptionNamePlaceholder,
            enabled = setup.enabled,
            readOnly = setup.readOnly,
        )
        if (setup.allowSpecialHours) {
            MonarchSelect(
                options = listOf(MonarchSelectOption(false, text.closed), MonarchSelectOption(true, text.specialHours)),
                selected = special,
                onSelect = { onChange(RowTokens.withSpecialHours(exception, it)) },
                modifier = Modifier.fillMaxWidth(),
                label = text.exceptionKind(position),
                enabled = editable,
            )
        }
        exception.intervals.firstOrNull()?.let { interval ->
            BusinessHoursEditorSpecialHours(position, interval, setup) { changed ->
                onChange(exception.copy(intervals = listOf(changed) + exception.intervals.drop(1)))
            }
        }
        if (error != null) {
            MonarchText(
                text = error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MonarchTextStyle.Caption,
                color = RowTokens.errorColor(MonarchTheme.colors),
            )
        }
        if (!setup.readOnly) {
            MonarchRemoveButton(
                name = text.removeException(position, exception.name.trim()),
                text = text.remove,
                onClick = onRemove,
                enabled = setup.enabled,
                icon = setup.removeIcon,
                modifier = Modifier.focusRequester(removeFocus).align(Alignment.End),
            )
        }
    }
}

/** The opening and closing time of a date with special hours: two menus side by side, every step minutes. */
@Composable
private fun BusinessHoursEditorSpecialHours(
    position: Int,
    interval: MonarchBusinessHoursInterval,
    setup: BusinessHoursEditorRowSetup,
    onChange: (MonarchBusinessHoursInterval) -> Unit,
) {
    val editable = setup.enabled && !setup.readOnly
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RowTokens.ControlGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchSelect(
            options = MonarchWeeklyHoursEditorTokens.startOptions(setup.step, interval.start)
                .map { MonarchSelectOption(it, setup.formats.time(it)) },
            selected = interval.start,
            onSelect = { onChange(interval.copy(start = it)) },
            modifier = Modifier.weight(1f),
            label = setup.text.exceptionStart(position),
            enabled = editable,
        )
        MonarchSelect(
            options = MonarchWeeklyHoursEditorTokens.endOptions(setup.step, interval.end)
                .map { MonarchSelectOption(it, setup.formats.time(it)) },
            selected = interval.end,
            onSelect = { onChange(interval.copy(end = it)) },
            modifier = Modifier.weight(1f),
            label = setup.text.exceptionEnd(position),
            enabled = editable,
        )
    }
}
