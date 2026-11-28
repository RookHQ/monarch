package dev.monarchui.sample.sections.datadisplay

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.components.layout.sectioncard.MonarchSectionCard
import dev.monarchui.components.scheduling.agendalist.MonarchAgendaEvent
import dev.monarchui.components.scheduling.agendalist.MonarchAgendaList
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val AgendaHeight = 220.dp
private const val Hour = DateUtils.HOUR_IN_MILLIS
private const val Standup = 9 * Hour
private const val Review = 14 * Hour
private const val WeekdayAndDate = DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE

private class SampleTask(val id: String, val title: String, val detail: String, val overdue: Boolean)

private val Tasks = listOf(
    SampleTask("t1", "Review 4 new applicants", "Hiring · Backend engineer", overdue = false),
    SampleTask("t2", "Send the Q3 invoice to Acme", "Billing · 2 days late", overdue = true),
    SampleTask("t3", "Approve the travel budget", "Finance", overdue = false),
)

/** The catalogue page of TodayPanel: MonarchSectionCard with MonarchAgendaList and MonarchCheckboxRow. */
object TodayPanelSection : CataloguePage {
    override val title = "TodayPanel"

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val today = remember { System.currentTimeMillis() / DateUtils.DAY_IN_MILLIS * DateUtils.DAY_IN_MILLIS }
        var done by rememberSaveable { mutableStateOf(listOf<String>()) }
        val events = remember(today) {
            listOf(
                MonarchAgendaEvent("standup", "Stand-up", start = today + Standup, end = today + Standup + Hour),
                MonarchAgendaEvent("review", "Design review", today + Review, today + Review + Hour, detail = "Room 4"),
            )
        }
        CatalogueCaption("A person's day: the agenda, then the tasks due today, overdue first")
        MonarchSectionCard(
            title = "Today",
            meta = DateUtils.formatDateTime(context, today, WeekdayAndDate),
        ) {
            MonarchText(text = "Schedule", style = MonarchTextStyle.Subheading)
            MonarchAgendaList(
                events = events,
                modifier = Modifier.height(AgendaHeight),
                from = today,
                days = 1,
                today = today,
            )
            MonarchText(text = "Due today", style = MonarchTextStyle.Subheading)
            Tasks.sortedByDescending { it.overdue }.forEach { task ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonarchCheckboxRow(
                        checked = task.id in done,
                        onCheckedChange = { on -> done = if (on) done + task.id else done - task.id },
                        label = task.title,
                        modifier = Modifier.weight(1f),
                        description = task.detail,
                    )
                    if (task.overdue) MonarchBadge(text = "Overdue", tone = MonarchTone.Danger)
                }
            }
        }
        CatalogueCaption("Nothing scheduled and nothing due")
        MonarchSectionCard(title = "Today") {
            MonarchText(text = "Nothing scheduled and nothing due today.")
        }
    }
}
