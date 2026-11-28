@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanStep
import dev.monarchui.components.intelligence.aiplan.MonarchAiPlan
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

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
