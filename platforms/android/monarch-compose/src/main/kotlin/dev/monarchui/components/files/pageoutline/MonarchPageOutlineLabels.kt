package dev.monarchui.components.files.pageoutline

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchPageOutline`. Every
 * `null` keeps the built-in string `monarch_pageoutline_<key>`.
 *
 * @property title the title and the spoken name of the outline, "On this page".
 * @property empty shown when there are no headings, "Headings you add appear here.".
 * @property stats the line under the list, "1,234 words · 6 min read".
 * @property current the state TalkBack reads on the current heading, "Current section".
 */
@Immutable
public class MonarchPageOutlineLabels(
    public val title: String? = null,
    public val empty: String? = null,
    public val stats: ((MonarchPageOutlineStats) -> String)? = null,
    public val current: String? = null,
)
