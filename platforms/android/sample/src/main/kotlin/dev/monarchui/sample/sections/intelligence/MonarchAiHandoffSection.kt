@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aihandoff.MonarchAiHandoff
import dev.monarchui.components.intelligence.aihandoff.MonarchAiHandoffTarget
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private const val ThreeMinutes = 180_000L
private const val OneHour = 3_600_000L

/** The catalogue page of AiHandoff. */
object MonarchAiHandoffSection : CataloguePage {
    override val title = "AiHandoff"

    @Composable
    override fun Content() {
        val now = remember { System.currentTimeMillis() }
        CatalogueCaption("From one agent to another")
        MonarchAiHandoff(
            from = MonarchAiAgentRef(name = "Research agent", id = "research"),
            to = MonarchAiHandoffTarget.Agent(MonarchAiAgentRef(name = "Writing agent", id = "writer")),
            reason = "Sources are ready to draft from",
            dateMillis = now - ThreeMinutes,
            nowMillis = now,
        )
        CatalogueCaption("From an agent to a person")
        MonarchAiHandoff(
            from = MonarchAiAgentRef(name = "Billing agent", id = "billing"),
            to = MonarchAiHandoffTarget.Person("Sam Lee"),
            reason = "Needs a person to approve the refund",
            dateMillis = now - OneHour,
            nowMillis = now,
        )
    }
}
