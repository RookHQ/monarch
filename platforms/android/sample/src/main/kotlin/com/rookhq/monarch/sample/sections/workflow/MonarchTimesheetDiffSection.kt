package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.workflow.timesheetdiff.MonarchTimesheetDiff
import com.rookhq.monarch.components.workflow.timesheetdiff.MonarchTimesheetDiffEntry
import com.rookhq.monarch.components.workflow.timesheetdiff.MonarchTimesheetDiffResult
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private const val Monday = 1_768_176_000_000L
private const val Tuesday = Monday + 86_400_000L
private const val TwoHours = 120
private const val TwoAndAHalfHours = 150
private const val ThreeQuarters = 45
private const val HalfHour = 30

/** The catalogue page of TimesheetDiff. */
object MonarchTimesheetDiffSection : CataloguePage {
    override val title = "TimesheetDiff"

    @Composable
    override fun Content() {
        var billable by rememberSaveable { mutableStateOf(true) }
        MonarchSwitchRow(checked = billable, onCheckedChange = { billable = it }, label = "Show billable totals")
        CatalogueCaption("Resubmitted week")
        MonarchTimesheetDiff(
            before = listOf(
                MonarchTimesheetDiffEntry("1", Monday, TwoHours, project = "Pulse", activity = "Design"),
                MonarchTimesheetDiffEntry("2", Monday, ThreeQuarters, project = "Pulse", billable = false),
            ),
            after = listOf(
                MonarchTimesheetDiffEntry("1", Tuesday, TwoAndAHalfHours, project = "Atlas", activity = "Design"),
                MonarchTimesheetDiffEntry("3", Tuesday, HalfHour, task = "ENG-142"),
            ),
            subtitle = "since version 3",
            showBillable = billable,
        )
        CatalogueCaption("No changes")
        MonarchTimesheetDiff(diff = MonarchTimesheetDiffResult(), showBillable = billable)
    }
}
