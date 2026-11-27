package com.rookhq.monarch.components.intelligence.aiconversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.rookhq.monarch.R
import com.rookhq.monarch.internal.monarchString

/**
 * A [MonarchAiConversation] that lives as long as the composition, with the built-in strings and a scope
 * from `rememberCoroutineScope()`.
 *
 * The conversation is created once and not re-created when the parameters
 * change. It does not survive a configuration change; to keep a conversation
 * across rotation, create it in a `ViewModel` with `viewModelScope` and pass
 * the strings with `getString(R.string.monarch_aiconversation_connection_lost)`.
 *
 * @param transport connects the conversation to your backend.
 * @param initialMessages messages to start with, for example a stored conversation.
 * @param conversationId the id from an earlier session.
 * @param queuesWhileOffline whether messages sent while offline wait in the queue.
 * @param slowAfterMillis the time without any event before a reply counts as slow. `null` turns it off.
 * @param connectionLostMessage replaces the built-in "The connection was lost."
 * (`monarch_aiconversation_connection_lost`).
 * @param toolResultFailedMessage replaces the built-in "The tool result could not be sent."
 * (`monarch_aiconversation_tool_result_failed`).
 */
@Composable
public fun rememberMonarchAiConversation(
    transport: MonarchAiTransport,
    initialMessages: List<MonarchAiConversationMessage> = emptyList(),
    conversationId: String? = null,
    queuesWhileOffline: Boolean = true,
    slowAfterMillis: Long? = MonarchAiConversation.DEFAULT_SLOW_AFTER_MILLIS,
    connectionLostMessage: String? = null,
    toolResultFailedMessage: String? = null,
): MonarchAiConversation {
    val scope = rememberCoroutineScope()
    val lost = monarchString(connectionLostMessage, R.string.monarch_aiconversation_connection_lost)
    val failed = monarchString(toolResultFailedMessage, R.string.monarch_aiconversation_tool_result_failed)
    return remember {
        MonarchAiConversation(
            transport = transport,
            scope = scope,
            connectionLostMessage = lost,
            toolResultFailedMessage = failed,
            initialMessages = initialMessages,
            conversationId = conversationId,
            queuesWhileOffline = queuesWhileOffline,
            slowAfterMillis = slowAfterMillis,
        )
    }
}
