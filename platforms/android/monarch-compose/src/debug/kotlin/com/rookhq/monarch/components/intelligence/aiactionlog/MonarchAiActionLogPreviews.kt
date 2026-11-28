@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence.aiactionlog

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private const val Now = 1_793_750_400_000L
private const val Hour = 3_600_000L
private val Agent = MonarchAiAgentRef(name = "Research agent", id = "research")

@MonarchPreviews
@Composable
internal fun MonarchAiActionLogPreview() {
    PreviewFrame {
        MonarchAiActionLog(
            entries = listOf(
                MonarchAiActionLogEntry(
                    id = "1",
                    date = Now - Hour,
                    agent = Agent,
                    action = "added a summary to",
                    target = "Quarterly report",
                    url = "https://example.com/report",
                    undoable = true,
                ),
                MonarchAiActionLogEntry(
                    id = "2",
                    date = Now - 26 * Hour,
                    agent = Agent,
                    action = "closed",
                    target = "Ticket 4821",
                    undone = true,
                ),
            ),
            now = Now,
            onUndo = {},
        )
        MonarchAiActionLog(entries = emptyList(), now = Now)
    }
}
