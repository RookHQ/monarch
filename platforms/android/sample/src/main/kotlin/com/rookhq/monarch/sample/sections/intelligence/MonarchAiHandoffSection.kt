@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.components.intelligence.aihandoff.MonarchAiHandoff
import com.rookhq.monarch.components.intelligence.aihandoff.MonarchAiHandoffTarget
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

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
