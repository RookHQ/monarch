@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStep
import com.rookhq.monarch.components.intelligence.aiplan.MonarchAiPlan
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private val SamplePlan = MonarchAiPlanPart(
    listOf(
        MonarchAiPlanStep(id = "1", label = "Find the three open invoices"),
        MonarchAiPlanStep(id = "2", label = "Draft a reminder for each customer"),
        MonarchAiPlanStep(id = "3", label = "Send the reminders from billing@"),
    ),
)

/** The catalogue page of AiPlan. */
object MonarchAiPlanSection : CataloguePage {
    override val title = "AiPlan"

    @Composable
    override fun Content() {
        var round by rememberSaveable { mutableIntStateOf(0) }
        var result by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Edit, reorder (long press or the handle's actions), add or remove, then decide")
        key(round) {
            MonarchAiPlan(
                part = SamplePlan,
                onApprove = { steps -> result = "Approved ${steps.size} steps" },
                onReject = { result = "Rejected" },
            )
        }
        result?.let { MonarchText(text = it) }
        MonarchButton(
            text = "Propose again",
            onClick = {
                round++
                result = null
            },
        )
        CatalogueCaption("Read-only")
        MonarchAiPlan(part = SamplePlan, editable = false, onApprove = {}, onReject = {})
    }
}
