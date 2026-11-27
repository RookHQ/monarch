package com.rookhq.monarch.components.files.diffviewer

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchDiffViewer`. Every
 * `null` uses the built-in string named after it in `strings_diffviewer.xml`.
 *
 * @property caption writes the TalkBack name of the lines from the names of the two sides: "Changes from
 * Version 3 to Version 4".
 * @property before the name of the earlier side when no `beforeLabel` is given: "Before".
 * @property after the name of the later side when no `afterLabel` is given: "After".
 * @property mode the TalkBack name of the layout switch: "Layout".
 * @property unified the label of the one-column layout: "Unified".
 * @property split the label of the two-column layout: "Side by side".
 * @property stats writes the totals for TalkBack from the added and removed lines: "3 added, 1 removed".
 * @property added read before an added line: "Added".
 * @property removed read before a removed line: "Removed".
 * @property blankLine read for an empty line: "Blank line".
 * @property expand writes the label of the button that shows a folded run: "Show 4 unchanged lines".
 * @property previous the label of the button that goes to the previous change: "Previous change".
 * @property next the label of the button that goes to the next change: "Next change".
 * @property position writes the announcement after previous or next from the change, from 1, and the
 * number of changes: "Change 2 of 5".
 * @property noChanges shown when the two texts are the same: "No changes.".
 */
@Immutable
public class MonarchDiffViewerLabels(
    public val caption: ((before: String, after: String) -> String)? = null,
    public val before: String? = null,
    public val after: String? = null,
    public val mode: String? = null,
    public val unified: String? = null,
    public val split: String? = null,
    public val stats: ((added: Int, removed: Int) -> String)? = null,
    public val added: String? = null,
    public val removed: String? = null,
    public val blankLine: String? = null,
    public val expand: ((count: Int) -> String)? = null,
    public val previous: String? = null,
    public val next: String? = null,
    public val position: ((index: Int, total: Int) -> String)? = null,
    public val noChanges: String? = null,
)
