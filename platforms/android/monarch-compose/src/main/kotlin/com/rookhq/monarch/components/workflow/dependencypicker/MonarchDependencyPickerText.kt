package com.rookhq.monarch.components.workflow.dependencypicker

import android.content.res.Resources
import com.rookhq.monarch.R

/** The strings of a MonarchDependencyPicker: each override from [labels], or the built-in resource. */
internal class MonarchDependencyPickerText(
    private val labels: MonarchDependencyPickerLabels,
    private val resources: Resources,
) {
    private fun string(override: String?, id: Int): String = override ?: resources.getString(id)

    val relationLabel: String
        get() = string(labels.relationLabel, R.string.monarch_dependencypicker_relation_label)
    val searchLabel: String get() = string(labels.searchLabel, R.string.monarch_dependencypicker_search_label)
    val searchPlaceholder: String
        get() = string(labels.searchPlaceholder, R.string.monarch_dependencypicker_search_placeholder)
    val noResults: String get() = string(labels.noResults, R.string.monarch_dependencypicker_no_results)
    val empty: String get() = string(labels.empty, R.string.monarch_dependencypicker_empty)
    val remove: String get() = string(labels.remove, R.string.monarch_dependencypicker_remove)

    fun relation(relation: MonarchDependencyRelation): String = labels.relations[relation] ?: resources.getString(
        when (relation) {
            MonarchDependencyRelation.BlockedBy -> R.string.monarch_dependencypicker_blocked_by
            MonarchDependencyRelation.Blocks -> R.string.monarch_dependencypicker_blocks
            MonarchDependencyRelation.Relates -> R.string.monarch_dependencypicker_relates
            MonarchDependencyRelation.Duplicates -> R.string.monarch_dependencypicker_duplicates
            MonarchDependencyRelation.DuplicatedBy -> R.string.monarch_dependencypicker_duplicated_by
        },
    )

    fun removeItem(relation: String, item: String): String = labels.removeItem?.invoke(relation, item)
        ?: resources.getString(R.string.monarch_dependencypicker_remove_item, relation, item)

    fun added(relation: String, item: String): String = labels.added?.invoke(relation, item)
        ?: resources.getString(R.string.monarch_dependencypicker_added, relation, item)

    fun removed(relation: String, item: String): String = labels.removed?.invoke(relation, item)
        ?: resources.getString(R.string.monarch_dependencypicker_removed, relation, item)

    fun status(name: String): String =
        labels.status?.invoke(name) ?: resources.getString(R.string.monarch_dependencypicker_status, name)
}
