package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationStatus
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiStreamEvent
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTransport
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTransportRequest
import com.rookhq.monarch.components.intelligence.aiconversation.rememberMonarchAiConversation
import com.rookhq.monarch.components.intelligence.aierrormessage.MonarchAiErrorKind
import com.rookhq.monarch.components.intelligence.aierrormessage.MonarchAiErrorMessage
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val WordDelayMillis = 120L

/** A transport that echoes the prompt word by word, standing in for a backend. Monarch does no networking. */
private class EchoTransport : MonarchAiTransport {
    override fun stream(request: MonarchAiTransportRequest): Flow<MonarchAiStreamEvent> = flow {
        val prompt = request.messages.lastOrNull()?.text.orEmpty()
        "You asked: $prompt".split(" ").forEach { word ->
            delay(WordDelayMillis)
            emit(MonarchAiStreamEvent.TextDelta("$word "))
        }
        emit(MonarchAiStreamEvent.Done())
    }
}

/** The catalogue page of useAiConversation. */
object MonarchAiConversationSection : CataloguePage {
    override val title = "useAiConversation"

    @Composable
    override fun Content() {
        val conversation = rememberMonarchAiConversation(EchoTransport())
        val messages by conversation.messages.collectAsState()
        val status by conversation.status.collectAsState()
        val error by conversation.error.collectAsState()
        CatalogueCaption("Status: ${status.name}")
        messages.forEach { message ->
            val branch = message.branch?.let { " (${it.index + 1} of ${it.count})" }.orEmpty()
            MonarchText(text = "${message.role.name}$branch", style = MonarchTextStyle.Label)
            MonarchText(text = message.text)
        }
        error?.let { failure ->
            MonarchAiErrorMessage(kind = MonarchAiErrorKind.fromCode(failure.code), onRetry = { conversation.retry() })
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            MonarchButton(
                text = "Ask",
                onClick = { conversation.send("What changed since yesterday?") },
                variant = MonarchButtonVariant.Primary,
                size = MonarchButtonSize.Sm,
            )
            MonarchButton(
                text = "Stop",
                onClick = { conversation.stop() },
                size = MonarchButtonSize.Sm,
                enabled = conversation.busy,
            )
            MonarchButton(
                text = "Regenerate",
                onClick = { conversation.regenerate() },
                size = MonarchButtonSize.Sm,
                enabled = status == MonarchAiConversationStatus.Idle && messages.isNotEmpty(),
            )
        }
    }
}
