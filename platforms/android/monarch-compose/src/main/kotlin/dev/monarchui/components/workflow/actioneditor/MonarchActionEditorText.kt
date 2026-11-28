package dev.monarchui.components.workflow.actioneditor

import android.content.res.Resources
import dev.monarchui.R

/** The strings of a MonarchActionEditor: each override from [labels], or the built-in resource. */
internal class MonarchActionEditorText(
    private val labels: MonarchActionEditorLabels,
    private val resources: Resources,
) {
    private fun string(override: String?, id: Int): String = override ?: resources.getString(id)

    val label: String get() = string(labels.label, R.string.monarch_actioneditor_label)
    val type: String get() = string(labels.type, R.string.monarch_actioneditor_type)
    val remove: String get() = string(labels.remove, R.string.monarch_actioneditor_remove)
    val moveUp: String get() = string(labels.moveUp, R.string.monarch_actioneditor_move_up)
    val moveDown: String get() = string(labels.moveDown, R.string.monarch_actioneditor_move_down)
    val add: String get() = string(labels.add, R.string.monarch_actioneditor_add)
    val empty: String get() = string(labels.empty, R.string.monarch_actioneditor_empty)
    val choose: String get() = string(labels.choose, R.string.monarch_actioneditor_choose)
    val unavailable: String get() = string(labels.unavailable, R.string.monarch_actioneditor_unavailable)

    private fun numbered(override: ((Int) -> String)?, id: Int, position: Int): String =
        override?.invoke(position) ?: resources.getString(id, position)

    fun action(position: Int): String = numbered(labels.action, R.string.monarch_actioneditor_action, position)
    fun removeItem(position: Int): String =
        numbered(labels.removeItem, R.string.monarch_actioneditor_remove_item, position)
    fun moveUpItem(position: Int): String =
        numbered(labels.moveUpItem, R.string.monarch_actioneditor_move_up_item, position)
    fun moveDownItem(position: Int): String =
        numbered(labels.moveDownItem, R.string.monarch_actioneditor_move_down_item, position)

    fun moved(type: String, position: Int, count: Int): String = labels.moved?.invoke(type, position, count)
        ?: resources.getString(R.string.monarch_actioneditor_moved, type, position, count)

    fun required(label: String): String =
        labels.required?.invoke(label) ?: resources.getString(R.string.monarch_actioneditor_required, label)

    fun outOfRange(label: String): String =
        labels.outOfRange?.invoke(label) ?: resources.getString(R.string.monarch_actioneditor_out_of_range, label)

    fun unknownType(key: String): String =
        labels.unknownType?.invoke(key) ?: resources.getString(R.string.monarch_actioneditor_unknown_type, key)

    fun maxReached(max: Int): String = labels.maxReached?.invoke(max)
        ?: resources.getQuantityString(R.plurals.monarch_actioneditor_max_reached, max, max)
}
