@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.scheduling

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.scheduling.timeoffpanel.MonarchTimeOffEntry
import com.rookhq.monarch.components.scheduling.timeoffpanel.MonarchTimeOffPanel
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme
import java.util.TimeZone

private const val SampleNow = 1_793_793_600_000L
private const val PanelSteps = 6
private val SampleEntries = listOf(
    MonarchTimeOffEntry(
        id = "1",
        personId = "p1",
        allDay = true,
        start = 1_794_182_400_000L,
        end = 1_794_528_000_000L,
        personName = "Alex Kim",
        type = "Annual leave",
    ),
    MonarchTimeOffEntry(
        id = "2",
        personId = null,
        allDay = true,
        start = 1_798_156_800_000L,
        end = 1_798_243_200_000L,
        reason = "Office closed",
    ),
    MonarchTimeOffEntry(
        id = "3",
        personId = "p2",
        allDay = false,
        start = 1_793_869_200_000L,
        end = 1_793_883_600_000L,
        personName = "Sam Lee",
        reason = "Training",
    ),
    MonarchTimeOffEntry(
        id = "4",
        personId = "p2",
        allDay = true,
        start = 1_791_763_200_000L,
        end = 1_791_763_200_000L,
        personName = "Sam Lee",
    ),
)

/** The catalogue page of TimeOffPanel. */
object MonarchTimeOffPanelSection : CataloguePage {
    override val title = "TimeOffPanel"

    @Composable
    override fun Content() {
        var entries by remember { mutableStateOf(SampleEntries) }
        var next by rememberSaveable { mutableIntStateOf(SampleEntries.size + 1) }
        val people = listOf(MonarchSelectOption("p1", "Alex Kim"), MonarchSelectOption("p2", "Sam Lee"))
        CatalogueCaption("Add, remove and show past time off")
        MonarchTimeOffPanel(
            entries = entries,
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * PanelSteps),
            onAdd = { draft ->
                val name = people.firstOrNull { it.value == draft.personId }?.label
                entries = entries + MonarchTimeOffEntry(
                    id = (next++).toString(),
                    personId = draft.personId,
                    allDay = draft.allDay,
                    start = draft.start,
                    end = draft.end,
                    personName = name,
                    type = draft.type,
                    reason = draft.reason,
                )
            },
            onRemove = { entry -> entries = entries - entry },
            people = people,
            allowClosure = true,
            types = listOf(MonarchSelectOption("Annual leave", "Annual leave"), MonarchSelectOption("Sick", "Sick")),
            timeZone = "Europe/London",
            now = SampleNow,
            addIcon = { MonarchIcon(SampleIcons.Plus, contentDescription = null) },
        )
    }
}
