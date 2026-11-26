package com.rookhq.monarch.components.workflow.backlogplanner

import android.content.res.Resources
import com.rookhq.monarch.R
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleDirection

/** The strings of a MonarchBacklogPlanner: each override from [labels], or the built-in resource. */
internal class MonarchBacklogPlannerText(
    private val labels: MonarchBacklogPlannerLabels,
    private val resources: Resources,
) {
    private fun string(override: String?, id: Int): String = override ?: resources.getString(id)

    val unestimated: String get() = string(labels.unestimated, R.string.monarch_backlogplanner_unestimated)
    val notEstimated: String get() = string(labels.notEstimated, R.string.monarch_backlogplanner_not_estimated)
    val move: String get() = string(labels.move, R.string.monarch_backlogplanner_move)
    val empty: String get() = string(labels.empty, R.string.monarch_backlogplanner_empty)
    val expanded: String get() = string(labels.expanded, R.string.monarch_backlogplanner_expanded)
    val collapsed: String get() = string(labels.collapsed, R.string.monarch_backlogplanner_collapsed)
    val actions: Map<MonarchDragHandleDirection, String> get() = labels.actions

    fun summary(count: Int, points: Int?, unestimated: Int): String =
        labels.summary?.invoke(count, points, unestimated) ?: listOfNotNull(
            resources.getQuantityString(R.plurals.monarch_backlogplanner_items, count, count),
            points?.let { resources.getQuantityString(R.plurals.monarch_backlogplanner_points_total, it, it) },
            unestimated.takeIf { it > 0 }?.let {
                resources.getQuantityString(R.plurals.monarch_backlogplanner_unestimated_count, it, it)
            },
        ).joinToString(MonarchBacklogPlannerTokens.SEPARATOR)

    fun capacity(points: Int, capacity: Int): String = labels.capacity?.invoke(points, capacity)
        ?: resources.getQuantityString(R.plurals.monarch_backlogplanner_capacity, capacity, points, capacity)

    fun overCapacity(by: Int): String = labels.overCapacity?.invoke(by)
        ?: resources.getQuantityString(R.plurals.monarch_backlogplanner_over_capacity, by, by)

    fun points(points: Int): String = labels.points?.invoke(points)
        ?: resources.getString(R.string.monarch_backlogplanner_points, points)

    fun handle(label: String): String = labels.handle?.invoke(label)
        ?: resources.getString(R.string.monarch_backlogplanner_handle, label)

    fun moveMenu(label: String): String = labels.moveMenu?.invoke(label)
        ?: resources.getString(R.string.monarch_backlogplanner_move_menu, label)

    fun moveTo(bucket: String): String = labels.moveTo?.invoke(bucket)
        ?: resources.getString(R.string.monarch_backlogplanner_move_to, bucket)

    /** The announcements, with the same overrides. */
    val announce: MonarchBacklogPlannerAnnouncements = MonarchBacklogPlannerAnnouncements(labels, resources)
}
