package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.datadisplay.periodselector.MonarchPeriodPreset
import com.rookhq.monarch.components.datadisplay.periodselector.MonarchPeriodSelector
import com.rookhq.monarch.components.datadisplay.periodselector.MonarchPeriodSelectorDefaults
import com.rookhq.monarch.components.datadisplay.periodselector.MonarchPeriodSelectorVariant
import com.rookhq.monarch.components.datadisplay.periodselector.MonarchPeriodValue
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of PeriodSelector: segments, a select field and custom dates. */
object MonarchPeriodSelectorSection : CataloguePage {
    override val title = "PeriodSelector"

    private const val ReportDays = 366
    private const val DayMillis = 86_400_000L

    @Composable
    override fun Content() {
        var dashboard by remember { mutableStateOf(MonarchPeriodValue(MonarchPeriodPreset.Last7Days)) }
        var report by remember { mutableStateOf(MonarchPeriodValue(MonarchPeriodPreset.ThisQuarter)) }
        val today = remember { MonarchPeriodSelectorDefaults.today() }
        CatalogueCaption("Segments, with the dates")
        MonarchPeriodSelector(value = dashboard, onValueChange = { dashboard = it }, showRange = true)
        val range = MonarchPeriodSelectorDefaults.resolve(dashboard, today)
        MonarchText(
            text = "Days in the period: ${range?.let { (it.endMillis - it.startMillis) / DayMillis + 1 } ?: 0}",
            color = MonarchTheme.colors.textMuted,
        )
        CatalogueCaption("A select field with every preset, custom up to a year")
        MonarchPeriodSelector(
            value = report,
            onValueChange = { report = it },
            presets = MonarchPeriodPreset.entries,
            variant = MonarchPeriodSelectorVariant.Select,
            showRange = true,
            maxDays = ReportDays,
        )
        CatalogueCaption("Disabled")
        MonarchPeriodSelector(
            value = MonarchPeriodValue(MonarchPeriodPreset.Today),
            onValueChange = {},
            enabled = false,
        )
    }
}
