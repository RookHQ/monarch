package com.rookhq.monarch.components.workflow.gantttimeline

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.internal.MonarchDateRangePicker
import com.rookhq.monarch.internal.MonarchPickerDialog
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The themed date range picker in its dialog, opened by Change dates. Save reports the picked range;
 * picking only a first day makes a one-day item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GanttTimelineDatesDialog(
    model: GanttTimelineModel,
    item: MonarchGanttTimelineItem,
    onDismiss: () -> Unit,
) {
    val span = GanttTimelineRules.span(item.start, item.end)
    val first = span?.start?.let(GanttTimelineDays::millis)
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = first,
        initialSelectedEndDateMillis = span?.end?.let(GanttTimelineDays::millis),
        initialDisplayedMonthMillis = first ?: model.today?.let(GanttTimelineDays::millis),
    )
    val text = model.text
    MonarchPickerDialog(
        onDismissRequest = onDismiss,
        confirmLabel = text.save,
        onConfirm = {
            val start = state.selectedStartDateMillis
            if (start != null) {
                model.commit(item, MonarchGanttTimelineDates(start, state.selectedEndDateMillis ?: start))
            }
            onDismiss()
        },
        cancelLabel = text.cancel,
        confirmEnabled = state.selectedStartDateMillis != null,
    ) {
        MonarchDateRangePicker(
            state = state,
            title = {
                Text(
                    text = item.label,
                    modifier = Modifier.padding(
                        start = MonarchTheme.spacing.space6,
                        end = MonarchTheme.spacing.space3,
                        top = MonarchTheme.spacing.space4,
                    ),
                )
            },
        )
    }
}
