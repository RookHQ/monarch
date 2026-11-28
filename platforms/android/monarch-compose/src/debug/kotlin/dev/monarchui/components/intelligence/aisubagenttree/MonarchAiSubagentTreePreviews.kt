@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aisubagenttree

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import java.util.Locale
import java.util.TimeZone

private const val T0 = 1_700_000_000_000L

@MonarchPreviews
@Composable
internal fun MonarchAiSubagentTreePreview() {
    val agents = listOf(
        MonarchAiSubagent(
            id = "lead",
            name = "Lead",
            status = MonarchAiSubagentStatus.Working,
            role = "Write the market report",
            stepsDone = 2,
            stepsTotal = 5,
            startedAt = T0,
            children = listOf(
                MonarchAiSubagent("researcher", "Researcher", MonarchAiSubagentStatus.Done, summary = "Found 3."),
                MonarchAiSubagent("analyst", "Analyst", MonarchAiSubagentStatus.NeedsInput, toolCalls = 4),
            ),
        ),
    )
    PreviewFrame {
        MonarchAiSubagentTree(
            agents = agents,
            expandIcon = { Text("›") },
            selection = "analyst",
            locale = Locale.US,
            timeZone = "UTC",
            onRespond = {},
            onStop = {},
        )
        MonarchAiSubagentTree(agents = emptyList(), expandIcon = { Text("›") })
    }
}
