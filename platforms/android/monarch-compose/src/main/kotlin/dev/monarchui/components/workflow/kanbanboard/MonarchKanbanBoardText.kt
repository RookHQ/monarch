package dev.monarchui.components.workflow.kanbanboard

import android.content.res.Resources
import dev.monarchui.R
import dev.monarchui.components.interaction.draghandle.MonarchDragHandleDirection

/** The strings of a MonarchKanbanBoard: each override from [labels], or the built-in resource. */
internal class MonarchKanbanBoardText(
    private val labels: MonarchKanbanBoardLabels,
    private val resources: Resources,
) {
    private fun string(override: String?, id: Int): String = override ?: resources.getString(id)

    val move: String get() = string(labels.move, R.string.monarch_kanbanboard_move)
    val empty: String get() = string(labels.empty, R.string.monarch_kanbanboard_empty)
    val blocked: String get() = string(labels.blocked, R.string.monarch_kanbanboard_blocked)
    val actions: Map<MonarchDragHandleDirection, String> get() = labels.actions

    fun handle(card: String): String = labels.handle?.invoke(card)
        ?: resources.getString(R.string.monarch_kanbanboard_handle, card)

    fun moveMenu(card: String): String = labels.moveMenu?.invoke(card)
        ?: resources.getString(R.string.monarch_kanbanboard_move_menu, card)

    fun moveTo(column: String): String = labels.moveTo?.invoke(column)
        ?: resources.getString(R.string.monarch_kanbanboard_move_to, column)

    fun countText(count: Int, limit: Int?): String = labels.countText?.invoke(count, limit) ?: if (limit == null) {
        resources.getString(R.string.monarch_kanbanboard_count, count)
    } else {
        resources.getString(R.string.monarch_kanbanboard_count_limit, count, limit)
    }

    fun count(count: Int, limit: Int?): String {
        labels.count?.let { return it(count, limit) }
        val cards = resources.getQuantityString(R.plurals.monarch_kanbanboard_cards, count, count)
        if (limit == null) return cards
        val withLimit = resources.getString(R.string.monarch_kanbanboard_limit, cards, limit)
        return when {
            count > limit -> resources.getString(R.string.monarch_kanbanboard_over_limit, withLimit)
            count == limit -> resources.getString(R.string.monarch_kanbanboard_at_limit, withLimit)
            else -> withLimit
        }
    }

    /** The announcements, with the same overrides. */
    val announce: MonarchKanbanBoardAnnouncements = MonarchKanbanBoardAnnouncements(labels, resources)
}
