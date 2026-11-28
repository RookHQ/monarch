@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rookhq.monarch.sample.sections.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.collaboration.activitytimeline.MonarchActivityTimeline
import com.rookhq.monarch.components.collaboration.activitytimeline.MonarchActivityTimelineActor
import com.rookhq.monarch.components.collaboration.activitytimeline.MonarchActivityTimelineEvent
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionItem
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionList
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadge
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

private const val SampleNow = 1_772_636_400_000L
private const val PageHeightSteps = 4
private const val OneHourMillis = 3_600_000L

/** The catalogue page of RecordPage: a recipe of existing components. */
object RecordPageSection : CataloguePage {
    override val title = "RecordPage"

    @Composable
    override fun Content() {
        CatalogueCaption("A record page on a phone")
        run {
            var tab by rememberSaveable { mutableIntStateOf(0) }
            val now = SampleNow
            val sections = listOf("Overview", "Details", "Activity")
            Column(modifier = Modifier.height(MonarchTheme.spacing.space24 * PageHeightSteps)) {
                TopAppBar(
                    title = { Text(text = "Acme renewal", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    actions = {
                        MonarchButton(
                            text = "Edit",
                            onClick = {},
                            variant = MonarchButtonVariant.Ghost,
                            size = MonarchButtonSize.Sm,
                        )
                    },
                )
                MonarchStatusBadge(
                    definition = MonarchStatusDefinition(label = "Negotiation", tone = MonarchTone.Info),
                    modifier = Modifier.padding(horizontal = MonarchTheme.spacing.space4),
                )
                PrimaryScrollableTabRow(selectedTabIndex = tab) {
                    sections.forEachIndexed { index, title ->
                        Tab(selected = index == tab, onClick = { tab = index }, text = { Text(title) })
                    }
                }
                when (tab) {
                    0 -> Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(MonarchTheme.spacing.space4),
                    ) {
                        MonarchText(text = "Renewal of the enterprise plan for 250 seats.")
                    }
                    1 -> Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(MonarchTheme.spacing.space4),
                    ) {
                        MonarchDescriptionList(
                            items = listOf(
                                MonarchDescriptionItem("Owner", "Ben Okafor"),
                                MonarchDescriptionItem("Value", "$48,000"),
                            ),
                        )
                    }
                    else -> MonarchActivityTimeline(
                        events = listOf(
                            MonarchActivityTimelineEvent(
                                id = "1",
                                verb = "moved the deal to",
                                time = now - OneHourMillis,
                                actor = MonarchActivityTimelineActor("Ben Okafor"),
                                target = "Negotiation",
                            ),
                        ),
                        modifier = Modifier.weight(1f),
                        now = now,
                        contentPadding = PaddingValues(MonarchTheme.spacing.space4),
                    )
                }
            }
        }
    }
}
