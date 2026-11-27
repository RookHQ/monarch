package com.rookhq.monarch.sample.sections.scheduling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursEditor
import com.rookhq.monarch.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursInterval
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

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
