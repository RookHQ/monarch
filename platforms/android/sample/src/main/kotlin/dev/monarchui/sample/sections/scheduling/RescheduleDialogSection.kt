package dev.monarchui.sample.sections.scheduling

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.feedback.banner.MonarchBannerAnnouncement
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.overlays.responsivedialog.MonarchResponsiveDialog
import dev.monarchui.components.scheduling.slotpicker.MonarchSlotPicker
import dev.monarchui.components.scheduling.slotpicker.MonarchSlotPickerSlot
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 2026-11-04T00:00Z. */
private const val Nov4 = 1_793_750_400_000L
private const val NineAm = 32_400_000L
private const val HalfHour = 1_800_000L
private const val SaveMillis = 800L
private const val MinuteMillis = 60_000L
private const val HourMinutes = 60
private const val OutsideHour = 18
private const val SlotCount = 4

/** The catalogue page of RescheduleDialog: a responsive dialog with the open slots, outside hours and a reason. */
object RescheduleDialogSection : CataloguePage {
    override val title = "RescheduleDialog"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var day by rememberSaveable { mutableStateOf<Long?>(Nov4) }
        var selection by rememberSaveable { mutableStateOf<Long?>(null) }
        var outside by rememberSaveable { mutableStateOf(false) }
        var reason by rememberSaveable { mutableStateOf("") }
        var pending by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var pickingDate by rememberSaveable { mutableStateOf(false) }
        var moved by rememberSaveable { mutableStateOf("") }
        val dateState = rememberDatePickerState(initialSelectedDateMillis = Nov4)
        val timeState = rememberTimePickerState(initialHour = OutsideHour, initialMinute = 0)
        val scope = rememberCoroutineScope()
        val slots = (0 until SlotCount).map { MonarchSlotPickerSlot(start = (day ?: Nov4) + NineAm + it * HalfHour) }
        CatalogueCaption("A booking")
        MonarchText(text = "Consultation with Sam Lee, Tue 3 Nov at 10:00")
        MonarchButton(text = "Reschedule", onClick = { open = true })
        if (moved.isNotEmpty()) MonarchText(text = moved)
        if (!open) return
        MonarchResponsiveDialog(
            onDismissRequest = { open = false },
            title = "Reschedule",
            description = "Consultation with Sam Lee · Currently Tue 3 Nov, 10:00",
            dismissible = !pending,
            actions = {
                MonarchButton(
                    text = "Cancel",
                    onClick = { open = false },
                    variant = MonarchButtonVariant.Ghost,
                    enabled = !pending,
                )
                MonarchButton(
                    text = "Reschedule",
                    onClick = {
                        val start = if (outside) {
                            dateState.selectedDateMillis?.let {
                                it + (timeState.hour * HourMinutes + timeState.minute) * MinuteMillis
                            }
                        } else {
                            selection
                        }
                        if (start == null) {
                            error = "Choose a new time first."
                        } else {
                            error = null
                            scope.launch {
                                pending = true
                                delay(SaveMillis)
                                pending = false
                                moved = "Moved. Reason: ${reason.ifBlank { "none" }}"
                                open = false
                            }
                        }
                    },
                    variant = MonarchButtonVariant.Primary,
                    loading = pending,
                )
            },
        ) {
            MonarchBanner(message = "The customer is emailed the new time.", tone = MonarchTone.Info)
            MonarchSwitchRow(
                checked = outside,
                onCheckedChange = { outside = it },
                label = "Choose a time outside working hours",
                description = "Pick any time. Clashes are still checked when you save.",
            )
            if (outside) {
                MonarchButton(text = "Choose the date", onClick = { pickingDate = true })
                TimeInput(state = timeState)
            } else {
                MonarchSlotPicker(
                    slots = slots,
                    selection = selection,
                    onSelectionChange = { selection = it },
                    day = day,
                    onDayChange = {
                        day = it
                        selection = null
                    },
                    timeZone = "Europe/London",
                    previousMonthIcon = { MonarchIcon(SampleIcons.ChevronLeft, contentDescription = null) },
                    nextMonthIcon = { MonarchIcon(SampleIcons.ChevronRight, contentDescription = null) },
                    today = Nov4,
                )
            }
            MonarchTextField(
                value = reason,
                onValueChange = { reason = it },
                label = "Reason (optional)",
                helperText = "Kept in the history.",
                singleLine = false,
            )
            error?.let {
                MonarchBanner(message = it, tone = MonarchTone.Danger, announcement = MonarchBannerAnnouncement.Polite)
            }
        }
        if (pickingDate) {
            DatePickerDialog(
                onDismissRequest = { pickingDate = false },
                confirmButton = {
                    MonarchButton(text = "OK", onClick = { pickingDate = false }, variant = MonarchButtonVariant.Ghost)
                },
            ) { DatePicker(state = dateState) }
        }
    }
}
