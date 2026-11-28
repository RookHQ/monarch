package dev.monarchui.components.scheduling.weeklyhourseditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.forms.select.MonarchSelect
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchRemoveButton
import dev.monarchui.theme.MonarchTheme

private typealias DayTokens = MonarchWeeklyHoursEditorTokens

/**
 * One day of a MonarchWeeklyHoursEditor: the day's switch (or its name and hours as text when read-only), its
 * intervals, its problem and its add and copy buttons, read by TalkBack as one traversal group.
 */
@Composable
@Suppress("LongParameterList")
internal fun MonarchWeeklyHoursEditorDay(
    name: String,
    ranges: List<MonarchWeeklyHoursInterval>,
    error: MonarchWeeklyHoursError?,
    step: Int,
    maxIntervals: Int,
    enabled: Boolean,
    readOnly: Boolean,
    formats: MonarchWeeklyHoursEditorFormats,
    text: MonarchWeeklyHoursEditorText,
    removeIcon: (@Composable () -> Unit)?,
    onToggle: (Boolean) -> Unit,
    onRangesChange: (List<MonarchWeeklyHoursInterval>) -> Unit,
    onAdd: () -> Unit,
    onCopy: () -> Unit,
) {
    BoxWithConstraints {
        if (maxWidth >= DayTokens.WideWidth && !readOnly) {
            MonarchWeeklyHoursEditorWideDay(
                name,
                ranges,
                error,
                step,
                maxIntervals,
                enabled,
                formats,
                text,
                removeIcon,
                onToggle,
                onRangesChange,
                onAdd,
                onCopy,
            )
        } else {
            MonarchWeeklyHoursEditorStackedDay(
                name,
                ranges,
                error,
                step,
                maxIntervals,
                enabled,
                readOnly,
                formats,
                text,
                removeIcon,
                onToggle,
                onRangesChange,
                onAdd,
                onCopy,
            )
        }
    }
}

/**
 * The day of a wide window, as on the web: the switch and the day's name, the intervals with their add and remove
 * buttons in the middle (or the word Unavailable), and Copy to all at the end.
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
private fun MonarchWeeklyHoursEditorWideDay(
    name: String,
    ranges: List<MonarchWeeklyHoursInterval>,
    error: MonarchWeeklyHoursError?,
    step: Int,
    maxIntervals: Int,
    enabled: Boolean,
    formats: MonarchWeeklyHoursEditorFormats,
    text: MonarchWeeklyHoursEditorText,
    removeIcon: (@Composable () -> Unit)?,
    onToggle: (Boolean) -> Unit,
    onRangesChange: (List<MonarchWeeklyHoursInterval>) -> Unit,
    onAdd: () -> Unit,
    onCopy: () -> Unit,
) {
    val on = ranges.isNotEmpty()
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true },
        horizontalArrangement = Arrangement.spacedBy(DayTokens.ControlGap),
        verticalAlignment = Alignment.Top,
    ) {
        MonarchSwitchRow(
            checked = on,
            onCheckedChange = onToggle,
            label = name,
            modifier = Modifier.width(DayTokens.WideNameWidth),
            enabled = enabled,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DayTokens.ControlGap)) {
            if (!on) {
                MonarchText(
                    text = text.unavailable,
                    modifier = Modifier.padding(top = DayTokens.ControlGap),
                    color = DayTokens.noteColor(colors),
                )
            }
            ranges.forEachIndexed { index, range ->
                MonarchWeeklyHoursEditorInterval(
                    range = range,
                    step = step,
                    enabled = enabled,
                    formats = formats,
                    text = text,
                    removeName = text.removeItem(name, index + 1),
                    removeIcon = removeIcon,
                    onChange = { next -> onRangesChange(ranges.mapIndexed { i, r -> if (i == index) next else r }) },
                    onRemove = { onRangesChange(ranges.filterIndexed { i, _ -> i != index }) },
                )
            }
            if (error != null && on) {
                MonarchText(
                    text = text.error(error),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MonarchTextStyle.Caption,
                    color = DayTokens.errorColor(colors),
                )
            }
            if (on && ranges.size < maxIntervals) {
                val addName = text.addItem(name)
                MonarchIconButton(
                    label = addName,
                    onClick = onAdd,
                    enabled = enabled,
                    size = MonarchButtonSize.Sm,
                ) { MonarchDefaultIcons.Plus() }
            }
        }
        if (on) {
            val copyName = text.copyToAllItem(name)
            MonarchButton(
                text = text.copyToAll,
                leadingIcon = MonarchDefaultIcons.Copy,
                onClick = onCopy,
                modifier = Modifier.semantics { contentDescription = copyName },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                enabled = enabled,
            )
        }
    }
}

/**
 * The stacked day of a narrow window, one day of a MonarchWeeklyHoursEditor: the day's switch (or its name and
 * hours as text when read-only), its
 * intervals, its problem and its add and copy buttons, read by TalkBack as one traversal group.
 */
@Composable
@Suppress("LongParameterList")
private fun MonarchWeeklyHoursEditorStackedDay(
    name: String,
    ranges: List<MonarchWeeklyHoursInterval>,
    error: MonarchWeeklyHoursError?,
    step: Int,
    maxIntervals: Int,
    enabled: Boolean,
    readOnly: Boolean,
    formats: MonarchWeeklyHoursEditorFormats,
    text: MonarchWeeklyHoursEditorText,
    removeIcon: (@Composable () -> Unit)?,
    onToggle: (Boolean) -> Unit,
    onRangesChange: (List<MonarchWeeklyHoursInterval>) -> Unit,
    onAdd: () -> Unit,
    onCopy: () -> Unit,
) {
    val on = ranges.isNotEmpty()
    val colors = MonarchTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(DayTokens.ControlGap),
    ) {
        if (readOnly) {
            Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                MonarchText(text = name, style = MonarchTextStyle.BodyStrong)
                MonarchText(
                    text = if (on) {
                        ranges.joinToString(", ") { text.range(formats.time(it.start), formats.time(it.end)) }
                    } else {
                        text.unavailable
                    },
                    color = DayTokens.noteColor(colors),
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonarchSwitchRow(
                    checked = on,
                    onCheckedChange = onToggle,
                    label = name,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    description = if (on) null else text.unavailable,
                )
                if (on) {
                    val copyName = text.copyToAllItem(name)
                    MonarchButton(
                        text = text.copyToAll,
                        leadingIcon = MonarchDefaultIcons.Copy,
                        onClick = onCopy,
                        modifier = Modifier.semantics { contentDescription = copyName },
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                        enabled = enabled,
                    )
                }
            }
            ranges.forEachIndexed { index, range ->
                MonarchWeeklyHoursEditorInterval(
                    range = range,
                    step = step,
                    enabled = enabled,
                    formats = formats,
                    text = text,
                    removeName = text.removeItem(name, index + 1),
                    removeIcon = removeIcon,
                    onChange = { next -> onRangesChange(ranges.mapIndexed { i, r -> if (i == index) next else r }) },
                    onRemove = { onRangesChange(ranges.filterIndexed { i, _ -> i != index }) },
                )
            }
        }
        if (error != null && on) {
            val message = text.error(error)
            MonarchText(
                text = message,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MonarchTextStyle.Caption,
                color = DayTokens.errorColor(colors),
            )
        }
        if (!readOnly && on && ranges.size < maxIntervals) {
            val addName = text.addItem(name)
            MonarchIconButton(
                label = addName,
                onClick = onAdd,
                enabled = enabled,
                size = MonarchButtonSize.Sm,
            ) { MonarchDefaultIcons.Plus() }
        }
    }
}

/** One interval: the start and end menus side by side, and its remove button under them at the end. */
@Composable
@Suppress("LongParameterList")
private fun MonarchWeeklyHoursEditorInterval(
    range: MonarchWeeklyHoursInterval,
    step: Int,
    enabled: Boolean,
    formats: MonarchWeeklyHoursEditorFormats,
    text: MonarchWeeklyHoursEditorText,
    removeName: String,
    removeIcon: (@Composable () -> Unit)?,
    onChange: (MonarchWeeklyHoursInterval) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DayTokens.ControlGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchSelect(
            options = DayTokens.startOptions(step, range.start).map { MonarchSelectOption(it, formats.time(it)) },
            selected = range.start,
            onSelect = { onChange(range.copy(start = it)) },
            modifier = Modifier.weight(1f),
            contentDescription = text.start,
            enabled = enabled,
        )
        MonarchText(text = "–", color = DayTokens.noteColor(MonarchTheme.colors))
        MonarchSelect(
            options = DayTokens.endOptions(step, range.end).map { MonarchSelectOption(it, formats.time(it)) },
            selected = range.end,
            onSelect = { onChange(range.copy(end = it)) },
            modifier = Modifier.weight(1f),
            contentDescription = text.end,
            enabled = enabled,
        )
        MonarchRemoveButton(
            name = removeName,
            text = text.remove,
            onClick = onRemove,
            enabled = enabled,
            icon = removeIcon,
        )
    }
}
