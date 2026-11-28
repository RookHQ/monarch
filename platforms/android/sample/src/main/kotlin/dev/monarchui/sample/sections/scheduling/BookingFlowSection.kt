package dev.monarchui.sample.sections.scheduling

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.descriptionlist.MonarchDescriptionItem
import dev.monarchui.components.datadisplay.descriptionlist.MonarchDescriptionList
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateKind
import dev.monarchui.components.forms.radiorow.MonarchRadioRow
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.scheduling.slotpicker.MonarchSlotPicker
import dev.monarchui.components.scheduling.slotpicker.MonarchSlotPickerSlot
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.components.workflow.stepper.MonarchStepper
import dev.monarchui.components.workflow.stepper.MonarchStepperCompactMode
import dev.monarchui.components.workflow.stepper.MonarchStepperStep
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** 2026-11-04T00:00Z. */
private const val Nov4 = 1_793_750_400_000L
private const val NineAm = 32_400_000L
private const val HalfHour = 1_800_000L
private const val SlotCount = 4
private const val ScreenHeightSteps = 6
private const val TimeSkeleton = "MMMEdjmm"

private class BookingStep(val id: String, val label: String, val title: String)

private val Steps = listOf(
    BookingStep("service", "Service", "Choose a service"),
    BookingStep("time", "Date and time", "Choose a date and time"),
    BookingStep("details", "Your details", "Enter your details"),
    BookingStep("review", "Review", "Check and confirm"),
)

private val Services = listOf("Intro call · 15 min", "Consultation · 45 min")

/** The catalogue page of BookingFlow: pick a service and a time, enter details, confirm. */
object BookingFlowSection : CataloguePage {
    override val title = "BookingFlow"

    @Composable
    override fun Content() {
        var step by rememberSaveable { mutableIntStateOf(0) }
        var service by rememberSaveable { mutableStateOf<String?>(null) }
        var day by rememberSaveable { mutableStateOf<Long?>(Nov4) }
        var slot by rememberSaveable { mutableStateOf<Long?>(null) }
        var zone by rememberSaveable { mutableStateOf("Europe/London") }
        var name by rememberSaveable { mutableStateOf("") }
        var email by rememberSaveable { mutableStateOf("") }
        var done by rememberSaveable { mutableStateOf(false) }
        val last = Steps.lastIndex
        val complete = BookingDraft(service, day, slot, zone, name, email).completes(step)
        BackHandler(enabled = step > 0 && !done) { step-- }
        CatalogueCaption("A booking, one step per screen")
        if (done) {
            MonarchEmptyState(
                title = "Booking complete",
                kind = MonarchEmptyStateKind.Success,
                message = "We sent the details to $email.",
                actions = {
                    MonarchButton(text = "Book another", onClick = {
                        done = false
                        step = 0
                    })
                },
            )
            return
        }
        Column(Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * ScreenHeightSteps)) {
            MonarchStepper(
                steps = Steps.mapIndexed { i, s -> MonarchStepperStep(s.id, s.label, complete = i < step) },
                current = step,
                compact = MonarchStepperCompactMode.Always,
                // Completed steps are buttons that go back to change them.
                onSelect = { _, index -> if (index < step) step = index },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = MonarchTheme.spacing.space4),
                verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
            ) {
                // A polite live region: each step change is announced.
                MonarchText(
                    text = Steps[step].title,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MonarchTextStyle.Heading,
                    heading = true,
                )
                BookingStepContent(
                    step = step,
                    booking = BookingDraft(service, day, slot, zone, name, email),
                    onChange = { draft ->
                        service = draft.service
                        day = draft.day
                        slot = draft.slot
                        zone = draft.zone
                        name = draft.name
                        email = draft.email
                    },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) MonarchButton(text = "Back", onClick = { step-- }, variant = MonarchButtonVariant.Ghost)
                Spacer(Modifier.weight(1f))
                MonarchButton(
                    text = if (step == last) "Confirm booking" else "Continue",
                    onClick = { if (step == last) done = true else step++ },
                    variant = MonarchButtonVariant.Primary,
                    enabled = complete,
                )
            }
        }
    }
}

/** What the person has chosen so far. */
private data class BookingDraft(
    val service: String?,
    val day: Long?,
    val slot: Long?,
    val zone: String,
    val name: String,
    val email: String,
) {
    /** Whether [step] has what it needs for Continue. */
    fun completes(step: Int): Boolean = when (step) {
        0 -> service != null
        1 -> slot != null
        2 -> name.isNotBlank() && '@' in email
        else -> true
    }
}

@Composable
private fun BookingStepContent(step: Int, booking: BookingDraft, onChange: (BookingDraft) -> Unit) {
    when (step) {
        0 -> Services.forEach { option ->
            MonarchRadioRow(
                selected = booking.service == option,
                onClick = { onChange(booking.copy(service = option)) },
                label = option,
            )
        }
        1 -> MonarchSlotPicker(
            slots = (0 until SlotCount).map {
                MonarchSlotPickerSlot(start = (booking.day ?: Nov4) + NineAm + it * HalfHour)
            },
            selection = booking.slot,
            onSelectionChange = { onChange(booking.copy(slot = it)) },
            day = booking.day,
            onDayChange = { onChange(booking.copy(day = it, slot = null)) },
            timeZone = booking.zone,
            previousMonthIcon = { MonarchIcon(SampleIcons.ChevronLeft, contentDescription = null) },
            nextMonthIcon = { MonarchIcon(SampleIcons.ChevronRight, contentDescription = null) },
            today = Nov4,
            onTimeZoneChange = { onChange(booking.copy(zone = it)) },
        )
        2 -> {
            MonarchTextField(
                value = booking.name,
                onValueChange = { onChange(booking.copy(name = it)) },
                label = "Name",
            )
            MonarchTextField(
                value = booking.email,
                onValueChange = { onChange(booking.copy(email = it)) },
                label = "Email",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
        }
        else -> BookingSummary(booking.service, booking.slot, booking.zone, booking.name, booking.email)
    }
}

@Composable
private fun BookingSummary(service: String?, slot: Long?, zone: String, name: String, email: String) {
    val locale = LocalConfiguration.current.locales[0]
    val time = remember(slot, zone, locale) {
        slot?.let {
            DateFormat.getInstanceForSkeleton(TimeSkeleton, locale)
                .apply { timeZone = TimeZone.getTimeZone(zone) }
                .format(java.util.Date(it))
        }
    }
    MonarchDescriptionList(
        items = listOf(
            MonarchDescriptionItem("Service", service),
            MonarchDescriptionItem("Time", time?.let { "$it ($zone)" }),
            MonarchDescriptionItem("Name", name),
            MonarchDescriptionItem("Email", email),
        ),
    )
}
