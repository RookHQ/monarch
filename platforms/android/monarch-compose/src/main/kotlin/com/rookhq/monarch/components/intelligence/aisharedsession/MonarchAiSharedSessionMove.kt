package com.rookhq.monarch.components.intelligence.aisharedsession

import androidx.compose.runtime.Immutable

/**
 * One move in the queue of a `MonarchAiSharedSession`, passed to `onReorder` beside the new order.
 *
 * @property id the request that moved.
 * @property from its old index.
 * @property to its new index.
 */
@Immutable
public data class MonarchAiSharedSessionMove(
    public val id: String,
    public val from: Int,
    public val to: Int,
)
