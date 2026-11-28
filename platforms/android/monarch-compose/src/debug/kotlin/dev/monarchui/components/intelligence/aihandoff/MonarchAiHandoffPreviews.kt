@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aihandoff

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private const val PreviewNow = 1_700_000_000_000L
private const val ThreeMinutes = 180_000L

@MonarchPreviews
@Composable
internal fun MonarchAiHandoffPreview() {
    PreviewFrame {
        MonarchAiHandoff(
            from = MonarchAiAgentRef(name = "Research agent", id = "research"),
            to = MonarchAiHandoffTarget.Agent(MonarchAiAgentRef(name = "Writing agent", id = "writer")),
            reason = "Sources are ready to draft from",
            dateMillis = PreviewNow - ThreeMinutes,
            nowMillis = PreviewNow,
        )
        MonarchAiHandoff(
            from = MonarchAiAgentRef(name = "Billing agent", id = "billing"),
            to = MonarchAiHandoffTarget.Person("Sam Lee"),
            reason = "Needs a person to approve the refund",
        )
    }
}
