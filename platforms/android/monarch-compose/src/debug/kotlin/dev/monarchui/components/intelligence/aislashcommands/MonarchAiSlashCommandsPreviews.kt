package dev.monarchui.components.intelligence.aislashcommands

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewCommands = listOf(
    MonarchAiSlashCommand("summarize", "summarize", description = "Shorten the selected text"),
    MonarchAiSlashCommand("translate", "translate", description = "Translate into another language"),
    MonarchAiSlashCommand("chart", "chart", description = "Needs a table in the reply", enabled = false),
)

@MonarchPreviews
@Composable
internal fun MonarchAiSlashCommandsPreview() {
    PreviewFrame {
        MonarchAiSlashCommands(
            value = TextFieldValue("/", selection = TextRange(1)),
            onValueChange = {},
            commands = PreviewCommands,
            onSelect = { null },
        )
        MonarchAiSlashCommands(
            value = TextFieldValue("/zzz", selection = TextRange(4)),
            onValueChange = {},
            commands = PreviewCommands,
            onSelect = { null },
        )
    }
}
