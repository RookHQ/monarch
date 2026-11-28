package com.rookhq.monarch.components.scheduling.timeoffpanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.banner.MonarchBanner
import com.rookhq.monarch.components.feedback.banner.MonarchBannerAnnouncement
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.monarchString
import java.util.TimeZone
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

/** The value of Who that stands for everyone (a closure). */
internal const val TimeOffPanelClosure: String = "monarch-time-off-panel-closure"

/** What the add sheet of MonarchTimeOffPanel offers, and today in its zone. */
internal class TimeOffPanelSheetSetup(
    val people: List<MonarchSelectOption<String>>,
    val allowClosure: Boolean,
    val types: List<MonarchSelectOption<String>>,
    val zone: TimeZone,
    val today: Long,
    val is24Hour: Boolean,
)

/** The add sheet of MonarchTimeOffPanel. It cannot be dismissed while adding. */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongMethod")
@Composable
internal fun TimeOffPanelAddSheet(
    setup: TimeOffPanelSheetSetup,
    formats: TimeOffPanelFormats,
    labels: MonarchTimeOffPanelLabels,
    onAdd: suspend (MonarchTimeOffDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MonarchTimeOffPanelTokens
    val everyone = monarchString(labels.everyone, R.string.monarch_timeoffpanel_everyone)
    val options = setup.people +
        listOfNotNull(MonarchSelectOption(TimeOffPanelClosure, everyone).takeIf { setup.allowClosure })
    var person by rememberSaveable { mutableStateOf(options.firstOrNull()?.value) }
    var type by rememberSaveable { mutableStateOf(setup.types.firstOrNull()?.value) }
    var allDay by rememberSaveable { mutableStateOf(true) }
    var fromDay by rememberSaveable { mutableLongStateOf(setup.today) }
    var untilDay by rememberSaveable { mutableLongStateOf(setup.today) }
    var startMinutes by rememberSaveable { mutableIntStateOf(tokens.DEFAULT_START_MINUTES) }
    var endDay by rememberSaveable { mutableLongStateOf(setup.today) }
    var endMinutes by rememberSaveable { mutableIntStateOf(tokens.DEFAULT_END_MINUTES) }
    var reason by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var running by rememberSaveable { mutableStateOf(false) }
    var failure by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val add by rememberUpdatedState(onAdd)
    val failedText = monarchString(labels.failed, R.string.monarch_timeoffpanel_failed)
    val start = if (allDay) fromDay else tokens.wallToInstant(fromDay, startMinutes, setup.zone)
    val end = if (allDay) untilDay else tokens.wallToInstant(endDay, endMinutes, setup.zone)
    val problem = if (attempted && tokens.endBeforeStart(allDay, start, end)) {
        monarchString(labels.endBeforeStart, R.string.monarch_timeoffpanel_end_before_start)
    } else {
        null
    }
    val submit = {
        val personId = person?.takeUnless { it == TimeOffPanelClosure }
        val draft = tokens.draft(personId, allDay, start, end, type.takeIf { setup.types.isNotEmpty() }, reason)
        if (draft == null) {
            attempted = true
        } else if (!running) {
            running = true
            failure = null
            scope.launch {
                val problem = timeOffPanelAttempt(failedText) { add(draft) }
                running = false
                failure = problem
                if (problem == null) onDismiss()
            }
        }
    }
    MonarchBottomSheet(
        onDismissRequest = onDismiss,
        title = monarchString(labels.add, R.string.monarch_timeoffpanel_add),
        description = labels.sheetDescription?.invoke(setup.zone.id)
            ?: stringResource(R.string.monarch_timeoffpanel_sheet_description, setup.zone.id),
        dismissible = !running,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(tokens.Gap),
        ) {
            TimeOffPanelChoices(
                options = options,
                person = person,
                onPersonChange = { person = it },
                types = setup.types,
                type = type,
                onTypeChange = { type = it },
                enabled = !running,
                labels = labels,
            )
            MonarchSwitchRow(
                checked = allDay,
                onCheckedChange = {
                    allDay = it
                    attempted = false
                },
                label = monarchString(labels.allDayLabel, R.string.monarch_timeoffpanel_all_day_label),
                enabled = !running,
            )
            TimeOffPanelDates(
                allDay = allDay,
                from = TimeOffPanelPick(fromDay, { day ->
                    fromDay = day
                    if (untilDay < day) untilDay = day
                }),
                until = TimeOffPanelPick(untilDay, { untilDay = it }, earliest = fromDay),
                start = TimeOffPanelPick(fromDay, { fromDay = it }, startMinutes, { startMinutes = it }),
                end = TimeOffPanelPick(endDay, { endDay = it }, endMinutes, { endMinutes = it }),
                problem = problem,
                is24Hour = setup.is24Hour,
                formats = formats,
                labels = labels,
            )
            MonarchTextField(
                value = reason,
                onValueChange = { reason = it },
                label = monarchString(labels.reason, R.string.monarch_timeoffpanel_reason),
                helperText = monarchString(labels.reasonDescription, R.string.monarch_timeoffpanel_reason_description),
                readOnly = running,
                singleLine = false,
            )
            TimeOffPanelSheetFooter(failure, running, labels, onCancel = onDismiss, onSave = submit)
        }
    }
}

/** Who (with the closure notice) and Type of the add sheet; each is hidden when there is nothing to choose. */
@Suppress("LongParameterList")
@Composable
private fun TimeOffPanelChoices(
    options: List<MonarchSelectOption<String>>,
    person: String?,
    onPersonChange: (String) -> Unit,
    types: List<MonarchSelectOption<String>>,
    type: String?,
    onTypeChange: (String) -> Unit,
    enabled: Boolean,
    labels: MonarchTimeOffPanelLabels,
) {
    if (options.size > 1) {
        MonarchSelect(
            options = options,
            selected = person,
            onSelect = onPersonChange,
            label = monarchString(labels.who, R.string.monarch_timeoffpanel_who),
            enabled = enabled,
        )
    }
    if (person == TimeOffPanelClosure) {
        MonarchBanner(
            message = monarchString(labels.closureNotice, R.string.monarch_timeoffpanel_closure_notice),
            tone = MonarchTone.Info,
        )
    }
    if (types.isNotEmpty()) {
        MonarchSelect(
            options = types,
            selected = type,
            onSelect = onTypeChange,
            label = monarchString(labels.type, R.string.monarch_timeoffpanel_type),
            enabled = enabled,
        )
    }
}

/** The failure of the last attempt and the Cancel and Add time off buttons of the add sheet. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeOffPanelSheetFooter(
    failure: String?,
    running: Boolean,
    labels: MonarchTimeOffPanelLabels,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    if (failure != null) {
        MonarchBanner(message = failure, tone = MonarchTone.Danger, announcement = MonarchBannerAnnouncement.Assertive)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTimeOffPanelTokens.RowGap)) {
        MonarchButton(
            text = monarchString(labels.cancel, R.string.monarch_timeoffpanel_cancel),
            onClick = onCancel,
            variant = MonarchButtonVariant.Ghost,
            enabled = !running,
        )
        MonarchButton(
            text = monarchString(labels.add, R.string.monarch_timeoffpanel_add),
            onClick = onSave,
            variant = MonarchButtonVariant.Primary,
            loading = running,
        )
    }
}

/** The date fields of the add sheet: From and Until for all-day time off, Start and End with times otherwise. */
@Suppress("LongParameterList")
@Composable
private fun TimeOffPanelDates(
    allDay: Boolean,
    from: TimeOffPanelPick,
    until: TimeOffPanelPick,
    start: TimeOffPanelPick,
    end: TimeOffPanelPick,
    problem: String?,
    is24Hour: Boolean,
    formats: TimeOffPanelFormats,
    labels: MonarchTimeOffPanelLabels,
) {
    val fields = if (allDay) {
        listOf(
            monarchString(labels.from, R.string.monarch_timeoffpanel_from) to from,
            monarchString(labels.until, R.string.monarch_timeoffpanel_until) to until,
        )
    } else {
        listOf(
            monarchString(labels.start, R.string.monarch_timeoffpanel_start) to start,
            monarchString(labels.end, R.string.monarch_timeoffpanel_end) to end,
        )
    }
    fields.forEach { (label, pick) ->
        TimeOffPanelDateField(label, pick, problem, is24Hour, formats, labels)
    }
}

/** Runs [action] and returns `null` when it succeeds, or the message to show when it throws. */
internal suspend fun timeOffPanelAttempt(failedText: String, action: suspend () -> Unit): String? {
    val error = runCatching { action() }.exceptionOrNull() ?: return null
    if (error is CancellationException) throw error
    return error.message?.ifBlank { null } ?: failedText
}
