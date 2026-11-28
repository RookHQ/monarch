@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aisubagenttree.MonarchAiSubagent
import com.rookhq.monarch.components.intelligence.aisubagenttree.MonarchAiSubagentStatus
import com.rookhq.monarch.components.intelligence.aisubagenttree.MonarchAiSubagentTree
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private const val SampleStart = 1_772_636_400_000L
private const val LeadEnd = SampleStart + 240_000L
private const val ResearcherEnd = SampleStart + 180_000L

/** A chevron that points to the end; it mirrors in right-to-left layouts. */
private val SubagentChevron: ImageVector by lazy {
    ImageVector.Builder(
        name = "Chevron",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = true,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(8.6f, 7.4f)
            lineTo(10f, 6f)
            lineTo(16f, 12f)
            lineTo(10f, 18f)
            lineTo(8.6f, 16.6f)
            lineTo(13.2f, 12f)
            close()
        }
    }.build()
}

private fun sampleAgents(stopped: Boolean, answered: Boolean) = listOf(
    MonarchAiSubagent(
        id = "lead",
        name = "Lead",
        status = if (stopped) MonarchAiSubagentStatus.Stopped else MonarchAiSubagentStatus.Working,
        role = "Write the market report",
        activity = "Merging the findings",
        stepsDone = 2,
        stepsTotal = 5,
        toolCalls = 6,
        startedAt = SampleStart,
        endedAt = if (stopped) LeadEnd else null,
        summary = if (stopped) "Stopped before the summary." else null,
        children = listOf(
            MonarchAiSubagent(
                id = "researcher",
                name = "Researcher",
                status = MonarchAiSubagentStatus.Done,
                role = "Find the three latest filings",
                startedAt = SampleStart,
                endedAt = ResearcherEnd,
                summary = "Found 3 filings.",
                partId = "call_1",
            ),
            MonarchAiSubagent(
                id = "analyst",
                name = "Analyst",
                status = if (answered) MonarchAiSubagentStatus.Working else MonarchAiSubagentStatus.NeedsInput,
                role = "Compare margins",
                activity = "Comparing margins",
            ),
        ),
    ),
)

/** The catalogue page of AiSubagentTree. */
object MonarchAiSubagentTreeSection : CataloguePage {
    override val title = "AiSubagentTree"

    @Composable
    override fun Content() {
        var stopped by rememberSaveable { mutableStateOf(false) }
        var answered by rememberSaveable { mutableStateOf(false) }
        var shown by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("A run with a lead and two sub-agents; select an agent for its actions")
        MonarchAiSubagentTree(
            agents = sampleAgents(stopped, answered),
            expandIcon = { MonarchIcon(SubagentChevron, contentDescription = null, size = MonarchIconSize.Sm) },
            selection = "analyst",
            onRespond = { answered = true },
            onOpen = { shown = "Transcript of ${it.name}" },
            onOpenPart = { shown = "Part ${it.partId ?: it.messageId}" },
            onStop = { stopped = true },
        )
        shown?.let { MonarchText(text = "Would open: $it") }
    }
}
