@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence.aisubagenttree

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
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
