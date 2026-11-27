package com.rookhq.monarch.components.workflow.backlogplanner

import androidx.compose.runtime.Immutable

/**
 * What the content of a `MonarchBacklogPlanner` row knows about its item.
 *
 * @property bucketId the bucket the item is shown in, including a drag in progress.
 * @property index the item's position in that bucket, from zero.
 * @property dragged whether the person is dragging this item.
 * @property pending whether this item's move is waiting for `onMove` to return.
 */
@Immutable
public class MonarchBacklogPlannerItemState(
    public val bucketId: String,
    public val index: Int,
    public val dragged: Boolean,
    public val pending: Boolean,
)
