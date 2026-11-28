package dev.monarchui.components.datadisplay.treeview

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchTreeView`. Every `null`
 * uses the built-in string named after it in `strings_treeview.xml`.
 *
 * @property expand writes the spoken name of the expand button from the item label: "Expand Engineering".
 * @property collapse writes the spoken name of the collapse button from the item label: "Collapse Engineering".
 * @property expanded the state of an open parent: "Expanded".
 * @property collapsed the state of a closed parent: "Collapsed".
 * @property level writes the depth of an item, starting at 1: "Level 2".
 * @property loading the row shown and announced while children load: "Loading".
 */
@Immutable
public class MonarchTreeViewLabels(
    public val expand: ((label: String) -> String)? = null,
    public val collapse: ((label: String) -> String)? = null,
    public val expanded: String? = null,
    public val collapsed: String? = null,
    public val level: ((level: Int) -> String)? = null,
    public val loading: String? = null,
)
