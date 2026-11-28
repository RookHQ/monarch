package dev.monarchui.sample.sections.scheduling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.scheduling.businesshourseditor.MonarchBusinessHoursEditor
import dev.monarchui.components.scheduling.businesshourseditor.MonarchBusinessHoursException
import dev.monarchui.components.scheduling.businesshourseditor.MonarchBusinessHoursInterval
import dev.monarchui.components.scheduling.businesshourseditor.MonarchBusinessHoursValue
import dev.monarchui.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursInterval
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

private val Start = MonarchBusinessHoursValue(
    timeZone = "Europe/London",
    hours = (1..5).map { MonarchWeeklyHoursInterval(it, 540, 1020) },
    exceptions = listOf(
        MonarchBusinessHoursException(1_798_156_800_000L, "Christmas Day"),
        MonarchBusinessHoursException(
            1_798_070_400_000L,
            "Christmas Eve",
            listOf(MonarchBusinessHoursInterval(600, 960)),
        ),
    ),
    name = "Support hours",
)

/** The catalogue page of BusinessHoursEditor: a calendar to edit, with Save showing whether it is valid. */
object MonarchBusinessHoursEditorSection : CataloguePage {
    override val title = "BusinessHoursEditor"

    @Composable
    override fun Content() {
        var value by remember { mutableStateOf(Start) }
        var saveTried by rememberSaveable { mutableStateOf(false) }
        var readOnly by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Name, zone, weekly hours, effective dates, holidays and default")
        MonarchBusinessHoursEditor(
            value = value,
            onValueChange = { value = it },
            showDefault = true,
            showEffectiveDates = true,
            requireName = true,
            showErrors = saveTried,
            readOnly = readOnly,
            addIcon = { MonarchIcon(SampleIcons.Plus, contentDescription = null) },
        )
        MonarchButton(text = "Save", onClick = { saveTried = true })
        if (saveTried) {
            val valid = value.validate(requireName = true).valid
            MonarchText(text = if (valid) "Ready to save." else "Fix the problems above to save.")
        }
        MonarchButton(
            text = if (readOnly) "Edit" else "Show as text",
            onClick = { readOnly = !readOnly },
            variant = MonarchButtonVariant.Ghost,
        )
    }
}
