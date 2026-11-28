package com.rookhq.monarch.components.intelligence.aicomputerview

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Three always-present live regions: the status, polite; the status when it
 * is a failure or a wait for the person, assertive; and new actions, polite
 * and at most once per [intervalMillis]. The actions shown at first are
 * history, not news.
 */
@Composable
internal fun AiComputerViewAnnouncer(
    status: MonarchAiComputerViewStatus,
    actions: List<MonarchAiComputerAction>,
    intervalMillis: Long,
    texts: AiComputerViewTexts,
) {
    val tokens = MonarchAiComputerViewTokens
    val currentTexts by rememberUpdatedState(texts)
    var previous by remember { mutableStateOf<MonarchAiComputerViewStatus?>(null) }
    var statusNews by remember { mutableStateOf("") }
    var urgentNews by remember { mutableStateOf("") }
    LaunchedEffect(status) {
        val before = previous
        previous = status
        if (before == null || before == status) return@LaunchedEffect
        val text = when (AiComputerViewRules.news(before, status)) {
            AiComputerViewNews.TakenOver -> currentTexts.announceTakenOver
            AiComputerViewNews.Released -> currentTexts.announceReleased
            AiComputerViewNews.Status -> currentTexts.announceStatus(currentTexts.status(status))
        }
        if (AiComputerViewRules.urgent(status)) {
            urgentNews = text
            statusNews = ""
        } else {
            statusNews = text
            urgentNews = ""
        }
    }
    val scope = rememberCoroutineScope()
    val pending = remember { AiComputerViewPending() }
    var actionNews by remember { mutableStateOf("") }
    fun flush() {
        if (pending.count == 0) return
        actionNews = currentTexts.announceActions(pending.count, pending.latest)
        pending.count = 0
        pending.cooling = scope.launch {
            delay(intervalMillis)
            pending.cooling = null
            flush()
        }
    }
    LaunchedEffect(actions) {
        if (!pending.started) {
            pending.started = true
            actions.mapTo(pending.seen) { it.id }
            return@LaunchedEffect
        }
        val fresh = actions.filter { it.id !in pending.seen }
        if (fresh.isEmpty()) return@LaunchedEffect
        fresh.mapTo(pending.seen) { it.id }
        if (pending.seen.size > maxOf(tokens.MAX_SEEN, actions.size * 2)) {
            pending.seen.clear()
            actions.mapTo(pending.seen) { it.id }
        }
        pending.count += fresh.size
        pending.latest = AiComputerViewContent.actionLabel(fresh.last(), currentTexts.sensitiveLabel)
        if (pending.cooling == null) flush()
    }
    Box {
        AiComputerViewLiveRegion(statusNews, LiveRegionMode.Polite)
        AiComputerViewLiveRegion(urgentNews, LiveRegionMode.Assertive)
        AiComputerViewLiveRegion(actionNews, LiveRegionMode.Polite)
    }
}

@Composable
private fun AiComputerViewLiveRegion(text: String, mode: LiveRegionMode) {
    Box(
        Modifier.semantics {
            liveRegion = mode
            if (text.isNotEmpty()) contentDescription = text
        },
    )
}
