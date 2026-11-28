package dev.monarchui.components.workflow.kanbanboard

import android.content.res.Resources
import dev.monarchui.R

/** What a MonarchKanbanBoard announces: each override from [labels], or the built-in resource. */
internal class MonarchKanbanBoardAnnouncements(
    private val labels: MonarchKanbanBoardLabels,
    private val resources: Resources,
) {
    fun pickedUp(card: String, column: String, position: Int, total: Int): String =
        labels.pickedUp?.invoke(card, column, position, total)
            ?: resources.getString(R.string.monarch_kanbanboard_picked_up, card, column, position, total)

    fun moved(card: String, column: String, position: Int, total: Int): String =
        labels.moved?.invoke(card, column, position, total)
            ?: resources.getString(R.string.monarch_kanbanboard_moved, card, column, position, total)

    fun dropped(card: String, column: String, position: Int, total: Int): String =
        labels.dropped?.invoke(card, column, position, total)
            ?: resources.getString(R.string.monarch_kanbanboard_dropped, card, column, position, total)

    fun cancelled(card: String, column: String): String = labels.cancelled?.invoke(card, column)
        ?: resources.getString(R.string.monarch_kanbanboard_cancelled, card, column)

    fun notAllowed(card: String, column: String): String = labels.notAllowed?.invoke(card, column)
        ?: resources.getString(R.string.monarch_kanbanboard_not_allowed, card, column)

    fun failed(card: String, column: String): String = labels.failed?.invoke(card, column)
        ?: resources.getString(R.string.monarch_kanbanboard_failed, card, column)
}
