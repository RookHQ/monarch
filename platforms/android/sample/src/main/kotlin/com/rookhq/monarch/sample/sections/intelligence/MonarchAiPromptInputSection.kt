@file:OptIn(com.rookhq.monarch.ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.intelligence.aiattachmenttray.MonarchAiAttachmentTrayFile
import com.rookhq.monarch.components.intelligence.aipromptinput.MonarchAiPromptInput
import com.rookhq.monarch.components.intelligence.aislashcommands.MonarchAiSlashCommand
import com.rookhq.monarch.components.intelligence.aislashcommands.MonarchAiSlashCommands
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private val SectionCommands = listOf(
    MonarchAiSlashCommand("summarize", "summarize", "Summarize the conversation"),
    MonarchAiSlashCommand("translate", "translate", "Translate the last answer"),
)

/** The catalogue page of AiPromptInput. */
object MonarchAiPromptInputSection : CataloguePage {
    override val title = "AiPromptInput"

    @Composable
    override fun Content() {
        var busy by rememberSaveable { mutableStateOf(false) }
        var last by rememberSaveable { mutableStateOf("") }
        var files by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Send, then Stop while the answer runs; type / for commands")
        MonarchAiPromptInput(
            label = "Message the assistant",
            state = rememberTextFieldState(),
            onSubmit = { text ->
                last = text
                busy = true
            },
            busy = busy,
            maxLength = 400,
            countTokens = { it.length / 4 },
            keyboardPadding = false,
            attachments = (1..files).map {
                MonarchAiAttachmentTrayFile("f$it", "file-$it.pdf", sizeBytes = 2_048L * it)
            },
            onStop = { busy = false },
            onAttach = { files++ },
            onRemoveAttachment = { files-- },
            triggerMenu = { value, onValueChange ->
                MonarchAiSlashCommands(
                    value = value,
                    onValueChange = onValueChange,
                    commands = SectionCommands,
                    onSelect = { "/${it.name} " },
                )
            },
        )
        if (last.isNotEmpty()) MonarchText(text = "Last prompt: $last")
        CatalogueCaption("Disabled, with the reason")
        MonarchAiPromptInput(
            label = "Message the assistant",
            state = rememberTextFieldState(),
            onSubmit = {},
            enabled = false,
            keyboardPadding = false,
            disabledReason = "Connect a provider to start.",
        )
    }
}
