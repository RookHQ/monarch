package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStep
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStepState
import com.rookhq.monarch.components.intelligence.aitaskprogress.MonarchAiTaskProgress
import com.rookhq.monarch.components.intelligence.aitaskprogress.MonarchAiTaskStatus
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of AiTaskProgress. */
object MonarchAiTaskProgressSection : CataloguePage {
    override val title = "AiTaskProgress"

    private const val FIVE_MINUTES = 300_000L
    private const val ETA = 180_000L

    @Composable
    override fun Content() {
        val startedAt = remember { System.currentTimeMillis() - FIVE_MINUTES }
        var status by rememberSaveable { mutableStateOf(MonarchAiTaskStatus.Running) }
        var notifying by rememberSaveable { mutableStateOf(false) }
        val steps = listOf(
            MonarchAiPlanStep("read", "Read the documents", MonarchAiPlanStepState.Done),
            MonarchAiPlanStep(
                "draft",
                "Draft the summary",
                if (status == MonarchAiTaskStatus.Done) MonarchAiPlanStepState.Done else MonarchAiPlanStepState.Active,
            ),
        )
        CatalogueCaption("Running, with steps and controls")
        MonarchAiTaskProgress(
            title = "Summarising 40 documents",
            status = status,
            startedAt = startedAt,
            steps = steps,
            etaMillis = ETA,
            notifying = notifying,
            onNotifyingChange = { notifying = it },
            onCancel = { status = MonarchAiTaskStatus.Cancelled },
        )
        MonarchButton(text = "Finish the task", onClick = { status = MonarchAiTaskStatus.Done })
        MonarchButton(text = "Fail the task", onClick = { status = MonarchAiTaskStatus.Failed })
        MonarchButton(text = "Restart", onClick = { status = MonarchAiTaskStatus.Running })
        CatalogueCaption("Only a bar")
        MonarchAiTaskProgress(
            title = "Indexing the wiki",
            status = MonarchAiTaskStatus.Paused,
            startedAt = startedAt,
            progress = 0.4f,
            onMinimize = {},
        )
    }
}
