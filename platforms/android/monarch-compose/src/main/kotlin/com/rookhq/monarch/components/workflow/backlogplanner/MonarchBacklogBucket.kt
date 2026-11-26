package com.rookhq.monarch.components.workflow.backlogplanner

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * One bucket of a `MonarchBacklogPlanner`: an iteration, a sprint, a release
 * or the backlog itself.
 *
 * @property id the stable id, passed back in moves.
 * @property title the heading, such as "Sprint 14" or "Backlog". Also used in the Move to menu and announcements.
 * @property items the items, in priority order.
 * @property subtitle quiet text under the title, such as the dates.
 * @property capacity the points the bucket can hold. Shows a capacity meter and an over-capacity note.
 * @property emptyText shown when the bucket has no items. `null` uses the built-in "Nothing here yet."
 * @property badge content after the title, such as an "Active" `MonarchBadge`.
 * @property actions buttons at the end of the header, such as Start or Edit.
 * @property footer content under the items, such as a quick-add field.
 */
@Immutable
public class MonarchBacklogBucket<T>(
    public val id: String,
    public val title: String,
    public val items: List<T>,
    public val subtitle: String? = null,
    public val capacity: Int? = null,
    public val emptyText: String? = null,
    public val badge: (@Composable () -> Unit)? = null,
    public val actions: (@Composable RowScope.() -> Unit)? = null,
    public val footer: (@Composable () -> Unit)? = null,
) {
    /** A copy of this bucket with [items] in place of its items. */
    public fun withItems(items: List<T>): MonarchBacklogBucket<T> =
        MonarchBacklogBucket(id, title, items, subtitle, capacity, emptyText, badge, actions, footer)
}
