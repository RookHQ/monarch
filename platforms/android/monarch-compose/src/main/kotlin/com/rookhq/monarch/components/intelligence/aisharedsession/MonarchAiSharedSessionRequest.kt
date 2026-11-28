package com.rookhq.monarch.components.intelligence.aisharedsession

import androidx.compose.runtime.Immutable

/**
 * A request waiting for its turn in a `MonarchAiSharedSession`.
 *
 * @property id a stable id, passed back through the queue callbacks.
 * @property participantId who asked.
 * @property preview the start of the request.
 */
@Immutable
public class MonarchAiSharedSessionRequest(
    public val id: String,
    public val participantId: String,
    public val preview: String,
)
