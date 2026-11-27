package com.rookhq.monarch.components.technical.jsonviewer

import android.content.res.Resources
import android.icu.text.NumberFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import com.rookhq.monarch.R
import java.util.Locale

/** The strings of one MonarchJsonViewer, with the caller's replacements applied. */
internal class MonarchJsonViewerStrings(
    private val labels: MonarchJsonViewerLabels,
    private val resources: Resources,
    locale: Locale,
) {
    private val numbers = NumberFormat.getIntegerInstance(locale)

    val label: String = labels.label ?: resources.getString(R.string.monarch_jsonviewer_label)
    val search: String = labels.search ?: resources.getString(R.string.monarch_jsonviewer_search)
    val searchPrompt: String = labels.searchPrompt ?: resources.getString(R.string.monarch_jsonviewer_search_prompt)
    val clearSearch: String = labels.clearSearch ?: resources.getString(R.string.monarch_jsonviewer_clear_search)
    val collapseAll: String = labels.collapseAll ?: resources.getString(R.string.monarch_jsonviewer_collapse_all)
    val copyPath: String = labels.copyPath ?: resources.getString(R.string.monarch_jsonviewer_copy_path)
    val copyValue: String = labels.copyValue ?: resources.getString(R.string.monarch_jsonviewer_copy_value)
    val selectedPath: String = labels.selectedPath ?: resources.getString(R.string.monarch_jsonviewer_selected_path)
    val expanded: String = labels.expanded ?: resources.getString(R.string.monarch_jsonviewer_expanded)
    val collapsed: String = labels.collapsed ?: resources.getString(R.string.monarch_jsonviewer_collapsed)

    private fun count(value: Int): String = numbers.format(value.toLong())

    /** "No matches", "1 match", "12 matches", "500+ matches". */
    fun matches(value: Int, capped: Boolean): String = labels.matches?.invoke(value, capped) ?: when (value) {
        0 -> resources.getString(R.string.monarch_jsonviewer_no_matches)
        else -> resources.getQuantityString(
            R.plurals.monarch_jsonviewer_matches,
            if (capped) Int.MAX_VALUE else value,
            count(value) + if (capped) "+" else "",
        )
    }

    /** "3 keys". */
    fun keys(value: Int): String = labels.keys?.invoke(value)
        ?: resources.getQuantityString(R.plurals.monarch_jsonviewer_keys, value, count(value))

    /** "2 items". */
    fun items(value: Int): String = labels.items?.invoke(value)
        ?: resources.getQuantityString(R.plurals.monarch_jsonviewer_items, value, count(value))

    /** "Show 100 more (250 hidden)". */
    fun showMore(next: Int, remaining: Int): String = labels.showMore?.invoke(next, remaining)
        ?: resources.getString(R.string.monarch_jsonviewer_show_more, count(next), count(remaining))

    /** "… (1,200 characters)". */
    fun truncated(length: Int): String =
        labels.truncated?.invoke(length) ?: resources.getString(R.string.monarch_jsonviewer_truncated, count(length))

    /**
     * The summary of a value: `{ } 3 keys`, `[ ] 2 items`, a quoted string cut after [maxStringLength] characters, or
     * the primitive.
     */
    fun summary(value: MonarchJsonValue, maxStringLength: Int): String = when (value) {
        is MonarchJsonValue.Object -> "{ } " + keys(value.members.size)
        is MonarchJsonValue.Array -> "[ ] " + items(value.items.size)
        is MonarchJsonValue.String -> if (value.value.length > maxStringLength) {
            "\"" + value.value.take(maxStringLength) + truncated(value.value.length) + "\""
        } else {
            "\"" + value.value + "\""
        }
        else -> MonarchJsonViewerPaths.searchableText(value).orEmpty()
    }
}

/** The strings of a JSON viewer, from [labels], the built-in resources and the locale. */
@Composable
internal fun monarchJsonViewerStrings(labels: MonarchJsonViewerLabels): MonarchJsonViewerStrings =
    MonarchJsonViewerStrings(labels, LocalResources.current, LocalConfiguration.current.locales[0])
