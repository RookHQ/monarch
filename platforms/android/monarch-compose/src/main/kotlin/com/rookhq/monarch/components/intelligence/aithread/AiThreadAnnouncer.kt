package com.rookhq.monarch.components.intelligence.aithread

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationMessage
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Two always-present live regions for the whole conversation. The polite
 * one says "Thinking…" once when a reply is submitted, then reads a reply
 * that arrives while the thread is shown in whole sentences, at most once
 * per [intervalMillis], then the rest and "Response complete" or "Response
 * stopped". The assertive one reads [errorText]. The messages themselves do
 * not announce, so streamed text is not read token by token.
 */
@Composable
internal fun AiThreadAnnouncer(
    last: MonarchAiConversationMessage?,
    status: MonarchAiConversationStatus,
    intervalMillis: Long,
    texts: AiThreadAnnouncerTexts,
    errorText: String?,
) {
    val speech = AiThreadSpeech
    val currentTexts by rememberUpdatedState(texts)
    val scope = rememberCoroutineScope()
    val memory = remember { AiThreadAnnouncerMemory() }
    var polite by remember { mutableStateOf("") }

    fun flush() {
        val text = speech.tidy(memory.queue)
        memory.queue = ""
        if (text.isEmpty()) return
        polite = text
        memory.cooling = scope.launch {
            delay(intervalMillis)
            memory.cooling = null
            flush()
        }
    }

    LaunchedEffect(status) {
        if (status != MonarchAiConversationStatus.Submitted) return@LaunchedEffect
        // Anything still queued from an earlier reply is dropped.
        memory.cooling?.cancel()
        memory.cooling = null
        memory.queue = ""
        polite = currentTexts.thinking
    }

    LaunchedEffect(last, status) {
        val spoken = last?.let { speech.spoken(it.text, currentTexts.codeBlock) }.orEmpty()
        val step = speech.speak(memory.speech, last, spoken, status)
        memory.speech = step.state
        if (step.text.isNotBlank()) memory.queue += " " + step.text
        if (step.finished) {
            step.ending?.let { ending ->
                memory.queue += " " + when (ending) {
                    AiThreadEnding.Complete -> currentTexts.responseComplete
                    AiThreadEnding.Stopped -> currentTexts.responseStopped
                }
            }
            memory.cooling?.cancel()
            memory.cooling = null
            flush()
        } else if (memory.cooling == null) {
            flush()
        }
    }

    Box {
        AiThreadLiveRegion(polite, LiveRegionMode.Polite)
        AiThreadLiveRegion(errorText.orEmpty(), LiveRegionMode.Assertive)
    }
}

@Composable
private fun AiThreadLiveRegion(text: String, mode: LiveRegionMode) {
    Box(
        Modifier.semantics {
            liveRegion = mode
            if (text.isNotEmpty()) contentDescription = text
        },
    )
}
