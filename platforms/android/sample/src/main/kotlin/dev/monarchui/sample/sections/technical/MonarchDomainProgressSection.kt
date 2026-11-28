package dev.monarchui.sample.sections.technical

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.technical.domainprogress.MonarchDomainDnsSnapshot
import dev.monarchui.components.technical.domainprogress.MonarchDomainNotify
import dev.monarchui.components.technical.domainprogress.MonarchDomainProblem
import dev.monarchui.components.technical.domainprogress.MonarchDomainProgress
import dev.monarchui.components.technical.domainprogress.MonarchDomainStage
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of DomainProgress. */
object MonarchDomainProgressSection : CataloguePage {
    override val title = "DomainProgress"

    // A fixed time, so the page and its screenshots do not change from day to day.
    private const val NOW = 1_772_636_400_000L
    private const val TWO_MINUTES = 120_000L

    @Composable
    override fun Content() {
        var stage by rememberSaveable { mutableIntStateOf(0) }
        var problem by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Stage")
        MonarchSegmentedControl {
            MonarchDomainStage.entries.forEachIndexed { index, option ->
                Segment(
                    selected = index == stage,
                    onClick = { stage = index },
                    label = { Text(option.name) },
                )
            }
        }
        MonarchSwitchRow(checked = problem, onCheckedChange = { problem = it }, label = "Blocking problem")
        MonarchDomainProgress(
            host = "book.example.com",
            stage = MonarchDomainStage.entries[stage],
            problems = if (problem) {
                listOf(MonarchDomainProblem("Your domain points somewhere else. Remove the A record for book."))
            } else {
                emptyList()
            },
            dnsFound = MonarchDomainDnsSnapshot(cnames = listOf("domains.example.net")),
            checkedAt = NOW - TWO_MINUTES,
            now = NOW,
            notify = MonarchDomainNotify.Email,
        )
    }
}
