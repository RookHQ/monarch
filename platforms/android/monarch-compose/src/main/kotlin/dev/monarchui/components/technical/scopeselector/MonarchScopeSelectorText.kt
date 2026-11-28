package dev.monarchui.components.technical.scopeselector

import android.content.res.Resources
import dev.monarchui.R

/** The strings of a MonarchScopeSelector: each override from [labels], or the built-in resource. */
internal class MonarchScopeSelectorText(
    private val labels: MonarchScopeSelectorLabels,
    private val resources: Resources,
) {
    private fun string(override: String?, id: Int): String = override ?: resources.getString(id)

    val label: String get() = string(labels.label, R.string.monarch_scopeselector_label)
    val empty: String get() = string(labels.empty, R.string.monarch_scopeselector_empty)
    val loading: String get() = string(labels.loading, R.string.monarch_scopeselector_loading)
    val noScopes: String get() = string(labels.noScopes, R.string.monarch_scopeselector_no_scopes)
    val selectAll: String get() = string(labels.selectAll, R.string.monarch_scopeselector_select_all)
    val clearAll: String get() = string(labels.clearAll, R.string.monarch_scopeselector_clear_all)

    fun count(selected: Int, total: Int): String = labels.count?.invoke(selected, total)
        ?: resources.getQuantityString(R.plurals.monarch_scopeselector_count, total, selected, total)

    fun selectAllIn(group: String): String =
        labels.selectAllIn?.invoke(group) ?: resources.getString(R.string.monarch_scopeselector_select_all_in, group)

    fun clearAllIn(group: String): String =
        labels.clearAllIn?.invoke(group) ?: resources.getString(R.string.monarch_scopeselector_clear_all_in, group)

    fun showMore(next: Int, remaining: Int): String = labels.showMore?.invoke(next, remaining)
        ?: resources.getQuantityString(R.plurals.monarch_scopeselector_show_more, next, next, remaining)
}
