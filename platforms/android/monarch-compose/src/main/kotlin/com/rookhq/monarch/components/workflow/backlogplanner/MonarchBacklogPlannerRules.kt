package com.rookhq.monarch.components.workflow.backlogplanner

/**
 * The app's rules a MonarchBacklogPlanner moves items by: how items are
 * keyed, which buckets are closed and which moves [canMove] refuses.
 */
internal class MonarchBacklogPlannerRules<T>(
    val keyOf: (T) -> String,
    val collapsed: Set<String>,
    private val canMove: ((T, String, String) -> Boolean)?,
) {
    /** Whether [item], which started in [origin], may go to the bucket [to]. Its own bucket is always allowed. */
    fun allowed(item: T, to: String, origin: String): Boolean =
        to == origin || canMove?.invoke(item, to, origin) != false

    /** The buckets [item] cannot be dragged or stepped into: closed ones and refused ones. */
    fun blocked(item: T, origin: String): (String) -> Boolean = { id -> id in collapsed || !allowed(item, id, origin) }

    /** The keys of the lazy list for [buckets]. */
    fun entries(buckets: List<MonarchBacklogBucket<T>>): List<String> =
        MonarchBacklogPlannerMoves.entries(buckets, collapsed, keyOf)
}
