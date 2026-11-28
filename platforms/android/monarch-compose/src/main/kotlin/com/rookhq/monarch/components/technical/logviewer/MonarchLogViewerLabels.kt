package com.rookhq.monarch.components.technical.logviewer

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchLogViewer`. Every
 * `null` uses the built-in string in `strings_logviewer.xml`.
 *
 * @property log the spoken name of the scrolling log: "Log".
 * @property levels the spoken name of the level filters: "Levels".
 * @property levelName writes a level's word, shown on lines and filters: "Debug", "Info", "Warn", "Error".
 * @property search the search field's placeholder: "Search".
 * @property clearSearch the spoken name of the search field's clear button: "Clear search".
 * @property follow the Follow toggle: "Follow".
 * @property wrap the wrap toggle: "Wrap lines".
 * @property count writes the line count from the lines shown and the total: "2 of 5 lines" or "5 lines".
 * @property jumpToLatest the button shown while following is paused: "Jump to latest".
 * @property newLines writes that button's text from the lines that arrived while paused: "3 new lines".
 * @property followPaused announced when following stops: "Following paused".
 * @property followResumed announced when following starts again: "Following new lines".
 * @property empty shown when there are no lines: "No log lines yet."
 * @property noMatches shown when the filters hide every line: "No lines match the filters."
 * @property clearFilters the button that empties the search and shows every level: "Clear filters".
 * @property loading shown while the first lines load: "Loading log".
 */
@Immutable
public class MonarchLogViewerLabels(
    public val log: String? = null,
    public val levels: String? = null,
    public val levelName: ((level: MonarchLogLevel) -> String)? = null,
    public val search: String? = null,
    public val clearSearch: String? = null,
    public val follow: String? = null,
    public val wrap: String? = null,
    public val count: ((shown: Int, total: Int) -> String)? = null,
    public val jumpToLatest: String? = null,
    public val newLines: ((count: Int) -> String)? = null,
    public val followPaused: String? = null,
    public val followResumed: String? = null,
    public val empty: String? = null,
    public val noMatches: String? = null,
    public val clearFilters: String? = null,
    public val loading: String? = null,
)
