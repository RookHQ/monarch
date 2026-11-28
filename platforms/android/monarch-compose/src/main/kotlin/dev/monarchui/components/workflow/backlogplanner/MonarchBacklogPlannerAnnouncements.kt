package dev.monarchui.components.workflow.backlogplanner

import android.content.res.Resources
import dev.monarchui.R

/** What a MonarchBacklogPlanner announces: each override from [labels], or the built-in resource. */
internal class MonarchBacklogPlannerAnnouncements(
    private val labels: MonarchBacklogPlannerLabels,
    private val resources: Resources,
) {
    fun pickedUp(label: String, bucket: String, position: Int, total: Int): String =
        labels.pickedUp?.invoke(label, bucket, position, total)
            ?: resources.getString(R.string.monarch_backlogplanner_picked_up, label, bucket, position, total)

    fun moved(label: String, bucket: String, position: Int, total: Int): String =
        labels.moved?.invoke(label, bucket, position, total)
            ?: resources.getString(R.string.monarch_backlogplanner_moved, label, bucket, position, total)

    fun dropped(label: String, bucket: String, position: Int, total: Int): String =
        labels.dropped?.invoke(label, bucket, position, total)
            ?: resources.getString(R.string.monarch_backlogplanner_dropped, label, bucket, position, total)

    fun cancelled(label: String): String = labels.cancelled?.invoke(label)
        ?: resources.getString(R.string.monarch_backlogplanner_cancelled, label)

    fun failed(label: String): String = labels.failed?.invoke(label)
        ?: resources.getString(R.string.monarch_backlogplanner_failed, label)
}
