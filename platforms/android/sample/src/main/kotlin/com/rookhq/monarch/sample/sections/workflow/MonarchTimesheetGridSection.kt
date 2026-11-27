@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableRowAction
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.workflow.timesheetgrid.MonarchTimesheetGrid
import com.rookhq.monarch.components.workflow.timesheetgrid.MonarchTimesheetGridDay
import com.rookhq.monarch.components.workflow.timesheetgrid.MonarchTimesheetGridLayout
import com.rookhq.monarch.components.workflow.timesheetgrid.MonarchTimesheetGridRow
import com.rookhq.monarch.components.workflow.timesheetgrid.MonarchTimesheetGridSummaryRow
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private const val Monday = 1_768_176_000_000L
private const val Day = 86_400_000L
private const val WorkDay = 480
private const val WorkDays = 5
private const val WeekDays = 7
private const val FourHours = 240
private const val HourAndHalf = 90
private const val FiveHours = 300

/** The catalogue page of TimesheetGrid. */
object MonarchTimesheetGridSection : CataloguePage {
    override val title = "TimesheetGrid"

    @Composable
    override fun Content() {
        var submitted by rememberSaveable { mutableStateOf(false) }
        var atlas by rememberSaveable { mutableStateOf(mapOf(Monday to FourHours, Monday + Day to HourAndHalf)) }
        var pulse by rememberSaveable { mutableStateOf(mapOf(Monday + 2 * Day to FiveHours)) }
        val days = (0 until WeekDays).map { i ->
            MonarchTimesheetGridDay(
                date = Monday + i * Day,
                today = i == 1,
                nonWorking = i >= WorkDays,
                lockReason = "Approved".takeIf { i == 0 },
                expectedMinutes = WorkDay.takeIf { i < WorkDays },
            )
        }
        val rows = listOf(
            MonarchTimesheetGridRow("atlas", "Atlas", description = "Design · Billable", cells = atlas),
            MonarchTimesheetGridRow("pulse", "Pulse", description = "Development", cells = pulse),
        )
        MonarchSwitchRow(checked = submitted, onCheckedChange = { submitted = it }, label = "Submitted")
        CatalogueCaption("Automatic: one day at a time on a phone")
        MonarchTimesheetGrid(
            days = days,
            rows = rows,
            summaryRows = listOf(
                MonarchTimesheetGridSummaryRow("in", "Attendance", mapOf(Monday to "7h 55m"), total = "7h 55m"),
            ),
            readOnly = submitted,
            rowActions = { row ->
                listOf(
                    MonarchDataTableRowAction("Clear row", danger = true) {
                        if (row.id == "atlas") atlas = emptyMap() else pulse = emptyMap()
                    },
                )
            },
            onCellChange = { rowId, day, minutes ->
                val next = { cells: Map<Long, Int> -> if (minutes == 0) cells - day else cells + (day to minutes) }
                if (rowId == "atlas") atlas = next(atlas) else pulse = next(pulse)
            },
        )
        CatalogueCaption("Grid, scrolled sideways")
        MonarchTimesheetGrid(
            days = days,
            rows = rows,
            layout = MonarchTimesheetGridLayout.Grid,
            readOnly = true,
            onCellActivate = { _, _ -> },
        )
    }
}
