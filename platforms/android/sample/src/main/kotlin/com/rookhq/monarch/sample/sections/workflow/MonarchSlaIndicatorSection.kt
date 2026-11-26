package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.components.workflow.slaindicator.MonarchSlaIndicator
import com.rookhq.monarch.components.workflow.slaindicator.MonarchSlaIndicatorState
import com.rookhq.monarch.components.workflow.slaindicator.MonarchSlaIndicatorVariant
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of SlaIndicator. */
object MonarchSlaIndicatorSection : CataloguePage {
    override val title = "SlaIndicator"

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val now = remember { System.currentTimeMillis() }
        CatalogueCaption("Chips, counting down live")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
        ) {
            MonarchSlaIndicator(
                MonarchSlaIndicatorState.OnTrack,
                dueAt = now + 2 * HOUR + 15 * MINUTE,
                warnBeforeMillis = HOUR,
                target = "First response",
            )
            MonarchSlaIndicator(MonarchSlaIndicatorState.AtRisk, dueAt = now + 2 * MINUTE, target = "Next response")
            MonarchSlaIndicator(MonarchSlaIndicatorState.Breached, dueAt = now - 40 * MINUTE)
            MonarchSlaIndicator(MonarchSlaIndicatorState.Paused)
            MonarchSlaIndicator(MonarchSlaIndicatorState.Met)
            MonarchSlaIndicator(MonarchSlaIndicatorState.NotApplicable)
        }
        CatalogueCaption("Text, in a list of targets")
        Target("First response") { MonarchSlaIndicator(MonarchSlaIndicatorState.Met, variant = it) }
        Target("Next response") {
            MonarchSlaIndicator(MonarchSlaIndicatorState.AtRisk, variant = it, dueAt = now + 40 * MINUTE)
        }
        Target("Resolution") {
            MonarchSlaIndicator(MonarchSlaIndicatorState.NotApplicable, variant = it, showNotApplicable = true)
        }
    }

    @Composable
    private fun Target(name: String, indicator: @Composable (MonarchSlaIndicatorVariant) -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(text = name)
            indicator(MonarchSlaIndicatorVariant.Text)
        }
    }
}
