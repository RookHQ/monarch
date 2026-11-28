package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.intelligence.aislashcommands.MonarchAiSlashCommand
import com.rookhq.monarch.components.intelligence.aislashcommands.MonarchAiSlashCommands
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private val SampleCommands = listOf(
    MonarchAiSlashCommand("summarize", "summarize", description = "Shorten the text", keywords = listOf("tl;dr")),
    MonarchAiSlashCommand("translate", "translate", description = "Translate into another language"),
    MonarchAiSlashCommand("clear", "clear", description = "Start a new conversation"),
    MonarchAiSlashCommand("chart", "chart", description = "Needs a table in the reply", enabled = false),
)

/** The catalogue page of AiSlashCommands. */
object MonarchAiSlashCommandsSection : CataloguePage {
    override val title = "AiSlashCommands"

    @Composable
    override fun Content() {
        var prompt by remember { mutableStateOf(TextFieldValue("/", selection = TextRange(1))) }
        var ran by remember { mutableStateOf<String?>(null) }
        CatalogueCaption("Type / at the start of a word; /clear runs at once, the others stay in the prompt")
        Column {
            MonarchAiSlashCommands(
                value = prompt,
                onValueChange = { prompt = it },
                commands = SampleCommands,
                onSelect = { command ->
                    if (command.id == "clear") {
                        ran = "Started a new conversation"
                        null
                    } else {
                        "/${command.name} "
                    }
                },
            )
            MonarchTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Message",
                singleLine = false,
            )
        }
        ran?.let { MonarchText(text = it) }
    }
}
