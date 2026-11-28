package dev.monarchui.components.scheduling.weeklyhourseditor

import android.content.res.Resources
import dev.monarchui.R

/** The strings of a MonarchWeeklyHoursEditor: each override from [labels], or the built-in resource. */
internal class MonarchWeeklyHoursEditorText(
    private val labels: MonarchWeeklyHoursEditorLabels,
    private val resources: Resources,
) {
    private fun string(override: String?, id: Int): String = override ?: resources.getString(id)

    val title: String get() = string(labels.title, R.string.monarch_weeklyhourseditor_title)
    val unavailable: String get() = string(labels.unavailable, R.string.monarch_weeklyhourseditor_unavailable)
    val start: String get() = string(labels.start, R.string.monarch_weeklyhourseditor_start)
    val end: String get() = string(labels.end, R.string.monarch_weeklyhourseditor_end)
    val add: String get() = string(labels.add, R.string.monarch_weeklyhourseditor_add)
    val remove: String get() = string(labels.remove, R.string.monarch_weeklyhourseditor_remove)
    val copyToAll: String get() = string(labels.copyToAll, R.string.monarch_weeklyhourseditor_copy_to_all)
    val midnight: String get() = string(labels.midnight, R.string.monarch_weeklyhourseditor_midnight)

    fun addItem(day: String): String =
        labels.addItem?.invoke(day) ?: resources.getString(R.string.monarch_weeklyhourseditor_add_item, day)

    fun removeItem(day: String, position: Int): String = labels.removeItem?.invoke(day, position)
        ?: resources.getString(R.string.monarch_weeklyhourseditor_remove_item, day, position)

    fun copyToAllItem(day: String): String = labels.copyToAllItem?.invoke(day)
        ?: resources.getString(R.string.monarch_weeklyhourseditor_copy_to_all_item, day)

    fun copied(day: String): String =
        labels.copied?.invoke(day) ?: resources.getString(R.string.monarch_weeklyhourseditor_copied, day)

    fun range(start: String, end: String): String = labels.range?.invoke(start, end)
        ?: resources.getString(R.string.monarch_weeklyhourseditor_range, start, end)

    fun error(error: MonarchWeeklyHoursError): String = when (error) {
        MonarchWeeklyHoursError.Range -> string(labels.rangeError, R.string.monarch_weeklyhourseditor_range_error)
        MonarchWeeklyHoursError.Order -> string(labels.orderError, R.string.monarch_weeklyhourseditor_order_error)
        MonarchWeeklyHoursError.Overlap ->
            string(labels.overlapError, R.string.monarch_weeklyhourseditor_overlap_error)
    }
}
