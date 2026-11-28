@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationMessage
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationStatus
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPartState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiRole
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTextPart
import dev.monarchui.components.intelligence.aipromptinput.MonarchAiPromptInput
import dev.monarchui.components.intelligence.aithread.MonarchAiThread
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

/**
 * The catalogue page of AiThread: a conversation over two days with older
 * messages to load, a prompt input under it, and a canned reply that streams
 * word by word, so pinning, the jump button and the thinking indicator can be
 * tried. The offline switch shows the notice.
 */
object MonarchAiThreadSection : CataloguePage {
    override val title = "AiThread"

    // A fixed time, so the page and its screenshots do not change from day to day.
    private const val NOW = 1_794_664_800_000L
    private const val MINUTE = 60_000L
    private const val DAY = 24 * 60 * MINUTE
    private const val HISTORY = 12
    private const val PAGE = 4
    private const val THINK_MILLIS = 900L
    private const val WORD_MILLIS = 120L
    private const val LOAD_MILLIS = 800L
    private const val LIST_HEIGHT_STEPS = 5
    private const val REPLY = "I checked the billing records. Invoice 42 is due on 4 December, and a reminder " +
        "goes out a week before. Would you like me to send one now?"

    private fun text(id: String, role: MonarchAiRole, body: String, at: Long, streaming: Boolean = false) =
        MonarchAiConversationMessage(
            id = id,
            role = role,
            parts = listOf(
                MonarchAiTextPart(body, if (streaming) MonarchAiPartState.Streaming else MonarchAiPartState.Done),
            ),
            createdAt = MonarchAiConversationMessage.timestamp(at),
        )

    private fun history(index: Int): MonarchAiConversationMessage {
        val at = NOW - DAY + index * MINUTE
        return if (index % 2 == 0) {
            text("h$index", MonarchAiRole.User, "Question $index about the March invoices.", at)
        } else {
            text("h$index", MonarchAiRole.Assistant, "Answer $index: the March invoices are all paid.", at)
        }
    }

    @Composable
    override fun Content() {
        var oldest by rememberSaveable { mutableIntStateOf(HISTORY - PAGE) }
        var loading by rememberSaveable { mutableStateOf(false) }
        var offline by rememberSaveable { mutableStateOf(false) }
        var recent by remember { mutableStateOf(listOf<MonarchAiConversationMessage>()) }
        var status by remember { mutableStateOf(MonarchAiConversationStatus.Idle) }
        val prompt = rememberTextFieldState()
        val stop = {
            status = MonarchAiConversationStatus.Stopped
            recent = recent.map { message ->
                message.copy(
                    parts = message.parts.map { part ->
                        (part as? MonarchAiTextPart)?.copy(state = MonarchAiPartState.Done) ?: part
                    },
                )
            }
        }

        LaunchedEffect(loading) {
            if (loading) {
                delay(LOAD_MILLIS)
                oldest = maxOf(0, oldest - PAGE)
                loading = false
            }
        }
        LaunchedEffect(status) {
            if (status != MonarchAiConversationStatus.Submitted) return@LaunchedEffect
            delay(THINK_MILLIS)
            val id = "r${recent.size}"
            val words = REPLY.split(" ")
            status = MonarchAiConversationStatus.Streaming
            for (count in 1..words.size) {
                val partial = words.take(count).joinToString(" ")
                val reply = text(id, MonarchAiRole.Assistant, partial, NOW, streaming = count < words.size)
                recent = recent.filter { it.id != id } + reply
                delay(WORD_MILLIS)
            }
            status = MonarchAiConversationStatus.Idle
        }

        MonarchAiThread(
            messages = (oldest until HISTORY).map(::history) + recent,
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * LIST_HEIGHT_STEPS),
            status = status,
            offline = offline,
            hasEarlier = oldest > 0,
            loadingEarlier = loading,
            nowMillis = NOW,
            onStop = stop,
            onLoadEarlier = { loading = true },
            empty = { MonarchText("Ask anything about your invoices.") },
        )
        MonarchAiPromptInput(
            label = "Message the assistant",
            state = prompt,
            onSubmit = { sent ->
                recent = recent + text("u${recent.size}", MonarchAiRole.User, sent, NOW)
                status = MonarchAiConversationStatus.Submitted
            },
            busy = status == MonarchAiConversationStatus.Submitted || status == MonarchAiConversationStatus.Streaming,
            onStop = stop,
            keyboardPadding = false,
        )
        CatalogueCaption("Connection")
        MonarchSwitchRow(checked = offline, onCheckedChange = { offline = it }, label = "Offline")
    }
}
