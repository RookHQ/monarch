package dev.monarchui.components.workflow.backlogplanner

import androidx.compose.runtime.Immutable

/**
 * A finished move in a `MonarchBacklogPlanner`.
 *
 * @property item the item that moved.
 * @property itemKey the item's key.
 * @property fromBucketId the bucket it left.
 * @property fromIndex its index in that bucket before the move.
 * @property toBucketId the bucket it went to, which may be the same one.
 * @property toIndex its index in that bucket after the move.
 */
@Immutable
public data class MonarchBacklogMove<T>(
    public val item: T,
    public val itemKey: String,
    public val fromBucketId: String,
    public val fromIndex: Int,
    public val toBucketId: String,
    public val toIndex: Int,
)
