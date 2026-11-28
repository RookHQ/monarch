package dev.monarchui.sample.sections.scheduling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.monarchui.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursEditor
import dev.monarchui.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursInterval
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of WeeklyHoursEditor: support desk hours with a lunch break. */
object MonarchWeeklyHoursEditorSection : CataloguePage {
    override val title = "WeeklyHoursEditor"

    private val weekdays = (1..5).flatMap { day ->
        listOf(MonarchWeeklyHoursInterval(day, 540, 750), MonarchWeeklyHoursInterval(day, 810, 1050))
    }

    private const val MINUTES_PER_HOUR = 60.0

    @Composable
    override fun Content() {
        CatalogueCaption("Editable, with a summary")
        var hours by remember { mutableStateOf(weekdays) }
        val invalid = MonarchWeeklyHoursInterval.validate(hours).isNotEmpty()
        val total = hours.sumOf { (it.end - it.start).coerceAtLeast(0) } / MINUTES_PER_HOUR
        MonarchWeeklyHoursEditor(
            value = hours,
            onValueChange = { hours = it },
            timeZoneLabel = {
                MonarchText(
                    text = "Times in Europe/London",
                    style = MonarchTextStyle.Caption,
                    color = MonarchTheme.colors.textMuted,
                )
            },
        )
        MonarchText(text = if (invalid) "Fix the highlighted days to save." else "$total hours a week")
        CatalogueCaption("Read-only")
        MonarchWeeklyHoursEditor(value = hours, onValueChange = {}, readOnly = true)
    }
}
