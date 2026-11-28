package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationMessage
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiRole
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTextPart
import com.rookhq.monarch.components.intelligence.aivoicemode.MonarchAiVoiceMode
import com.rookhq.monarch.components.intelligence.aivoicemode.MonarchAiVoiceModeSize
import com.rookhq.monarch.components.intelligence.aivoicemode.MonarchAiVoiceModeState
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of AiVoiceMode. The buttons move a pretend call through its states; no audio is used. */
object MonarchAiVoiceModeSection : CataloguePage {
    override val title = "AiVoiceMode"

    private val captions = listOf(
        MonarchAiConversationMessage(
            id = "1",
            role = MonarchAiRole.User,
            parts = listOf(MonarchAiTextPart("What is on my calendar today?")),
            createdAt = "2025-03-10T12:00:00.000Z",
        ),
        MonarchAiConversationMessage(
            id = "2",
            role = MonarchAiRole.Assistant,
            parts = listOf(MonarchAiTextPart("You have two meetings this afternoon.")),
            createdAt = "2025-03-10T12:00:02.000Z",
        ),
    )

    @Composable
    override fun Content() {
        var state by rememberSaveable { mutableStateOf(MonarchAiVoiceModeState.Speaking) }
        var restarts by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Compact, in a panel")
        MonarchAiVoiceMode(
            state = state,
            level = SampleLevel,
            agent = MonarchAiAgentRef(name = "Ada"),
            captions = captions,
            elapsedSeconds = SampleSeconds + restarts,
            size = MonarchAiVoiceModeSize.Compact,
            onMuteChange = { muted ->
                state = if (muted) MonarchAiVoiceModeState.Muted else MonarchAiVoiceModeState.Listening
            },
            onInterrupt = { state = MonarchAiVoiceModeState.Listening },
            onEnd = { state = MonarchAiVoiceModeState.Ended },
            onRestart = {
                restarts++
                state = MonarchAiVoiceModeState.Connecting
            },
        )
        CatalogueCaption("Failed")
        MonarchAiVoiceMode(state = MonarchAiVoiceModeState.Error, size = MonarchAiVoiceModeSize.Compact, onRetry = {})
    }
}

private const val SampleLevel = 0.6f
private const val SampleSeconds = 65
