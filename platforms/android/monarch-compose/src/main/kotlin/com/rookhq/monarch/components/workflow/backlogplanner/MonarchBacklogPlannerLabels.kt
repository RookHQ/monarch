package com.rookhq.monarch.components.workflow.backlogplanner

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleDirection

/**
 * Replacements for the built-in strings of a `MonarchBacklogPlanner`. Every
 * `null` keeps the built-in string `monarch_backlogplanner_<key>`. Labels are
 * the items' plain-text names; positions count from one.
 *
 * @property summary the line under a bucket's title, from the item count, the points (`null` without
 * `itemPoints`) and the unestimated count: "4 items · 13 points · 1 unestimated".
 * @property capacity the capacity text, from the points and the capacity: "18 of 20 points".
 * @property overCapacity the note of a bucket over capacity, from the excess: "Planned work is over capacity by
 * 3 points."
 * @property points the points on a row: "3 pt".
 * @property unestimated the mark on a row with no estimate, "–".
 * @property notEstimated what TalkBack reads for that mark, "Not estimated".
 * @property handle the TalkBack name of a row's drag handle: "Move ABC-12".
 * @property moveMenu the TalkBack name of a row's Move to button: "Move ABC-12 to another bucket".
 * @property move the visible text of the Move to button, "Move".
 * @property moveTo the title of the Move to menu and the prefix of its TalkBack actions: "Move to Sprint 14".
 * @property empty the text of an empty bucket, "Nothing here yet."
 * @property expanded the state TalkBack reads for an open bucket.
 * @property collapsed the state TalkBack reads for a closed bucket.
 * @property pickedUp announced when a drag starts: "Picked up ABC-12. Sprint 14, position 2 of 5."
 * @property moved announced after a move by an action: "ABC-12 moved to Backlog, position 1 of 9."
 * @property dropped announced when a drag ends or the menu moves an item: "ABC-12 dropped in Backlog, position 1
 * of 9."
 * @property cancelled announced when a drag is cancelled: "Move cancelled. ABC-12 returned to where it was."
 * @property failed announced when `onMove` throws: "ABC-12 could not be moved. It is back where it was."
 * @property actions replacements for the names of the handle's TalkBack actions, "Move up" and "Move down".
 */
@Immutable
public class MonarchBacklogPlannerLabels(
    public val summary: ((count: Int, points: Int?, unestimated: Int) -> String)? = null,
    public val capacity: ((points: Int, capacity: Int) -> String)? = null,
    public val overCapacity: ((by: Int) -> String)? = null,
    public val points: ((points: Int) -> String)? = null,
    public val unestimated: String? = null,
    public val notEstimated: String? = null,
    public val handle: ((label: String) -> String)? = null,
    public val moveMenu: ((label: String) -> String)? = null,
    public val move: String? = null,
    public val moveTo: ((bucket: String) -> String)? = null,
    public val empty: String? = null,
    public val expanded: String? = null,
    public val collapsed: String? = null,
    public val pickedUp: ((label: String, bucket: String, position: Int, total: Int) -> String)? = null,
    public val moved: ((label: String, bucket: String, position: Int, total: Int) -> String)? = null,
    public val dropped: ((label: String, bucket: String, position: Int, total: Int) -> String)? = null,
    public val cancelled: ((label: String) -> String)? = null,
    public val failed: ((label: String) -> String)? = null,
    public val actions: Map<MonarchDragHandleDirection, String> = emptyMap(),
)
