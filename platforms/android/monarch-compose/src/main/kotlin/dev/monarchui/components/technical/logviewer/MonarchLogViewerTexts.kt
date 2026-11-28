package dev.monarchui.components.technical.logviewer

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import dev.monarchui.R
import java.util.Locale

/** The strings of a MonarchLogViewer: the app's replacements, else the built-in ones. */
internal class MonarchLogViewerTexts(private val labels: MonarchLogViewerLabels, private val resources: Resources) {
    private fun pick(override: String?, @StringRes id: Int): String = override ?: resources.getString(id)

    val log: String get() = pick(labels.log, R.string.monarch_logviewer_log)
    val levels: String get() = pick(labels.levels, R.string.monarch_logviewer_levels)
    val search: String get() = pick(labels.search, R.string.monarch_logviewer_search)
    val clearSearch: String get() = pick(labels.clearSearch, R.string.monarch_logviewer_clear_search)
    val follow: String get() = pick(labels.follow, R.string.monarch_logviewer_follow)
    val wrap: String get() = pick(labels.wrap, R.string.monarch_logviewer_wrap)
    val jumpToLatest: String get() = pick(labels.jumpToLatest, R.string.monarch_logviewer_jump_to_latest)
    val followPaused: String get() = pick(labels.followPaused, R.string.monarch_logviewer_follow_paused)
    val followResumed: String get() = pick(labels.followResumed, R.string.monarch_logviewer_follow_resumed)
    val empty: String get() = pick(labels.empty, R.string.monarch_logviewer_empty)
    val noMatches: String get() = pick(labels.noMatches, R.string.monarch_logviewer_no_matches)
    val clearFilters: String get() = pick(labels.clearFilters, R.string.monarch_logviewer_clear_filters)
    val loading: String get() = pick(labels.loading, R.string.monarch_logviewer_loading)

    /** A level's word. */
    fun levelName(level: MonarchLogLevel): String = labels.levelName?.invoke(level) ?: resources.getString(
        when (level) {
            MonarchLogLevel.Debug -> R.string.monarch_logviewer_level_debug
            MonarchLogLevel.Info -> R.string.monarch_logviewer_level_info
            MonarchLogLevel.Warn -> R.string.monarch_logviewer_level_warn
            MonarchLogLevel.Error -> R.string.monarch_logviewer_level_error
        },
    )

    /** "5 lines", or "2 of 5 lines" when filters hide lines. */
    fun count(shown: Int, total: Int, locale: Locale): String {
        labels.count?.let { return it(shown, total) }
        return if (shown == total) {
            val formatted = MonarchLogViewerLogic.count(total, locale)
            resources.getQuantityString(R.plurals.monarch_logviewer_count, total, formatted)
        } else {
            resources.getQuantityString(
                R.plurals.monarch_logviewer_count_filtered,
                total,
                MonarchLogViewerLogic.count(shown, locale),
                MonarchLogViewerLogic.count(total, locale),
            )
        }
    }

    /** "3 new lines". */
    fun newLines(count: Int, locale: Locale): String = labels.newLines?.invoke(count) ?: resources.getQuantityString(
        R.plurals.monarch_logviewer_new_lines,
        count,
        MonarchLogViewerLogic.count(count, locale),
    )
}

@Composable
internal fun monarchLogViewerTexts(labels: MonarchLogViewerLabels): MonarchLogViewerTexts {
    val resources = LocalResources.current
    return remember(labels, resources) { MonarchLogViewerTexts(labels, resources) }
}
