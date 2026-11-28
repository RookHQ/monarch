package dev.monarchui.components.datadisplay.treeview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/**
 * One visible row of the flattened tree.
 *
 * @property item the item, or the parent whose children are loading when [loadingRow] is set.
 * @property level the depth, starting at 1.
 * @property parentId the id of the parent, `null` at the top level.
 * @property loadingRow the row is the "Loading" placeholder under [item].
 */
internal data class TreeRow(
    val item: MonarchTreeViewItem,
    val level: Int,
    val parentId: String?,
    val loadingRow: Boolean = false,
) {
    /** A key that is unique among the rows of the list. */
    val key: String get() = if (loadingRow) "loading:${item.id}" else item.id
}

/** The colours of a row. */
internal data class TreeRowColors(val container: Color, val text: Color, val description: Color, val bar: Color?)

/** Component tokens and rules of MonarchTreeView, built from primitives and roles only. */
internal object MonarchTreeViewTokens {
    /** The indent of each level after the first. */
    val Indent: Dp = MonarchTokens.Space.space5.dp
    val RowGap: Dp = MonarchTokens.Space.space2.dp
    val EndPadding: Dp = MonarchTokens.Space.space2.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space1.dp
    val SpinnerSize: Dp = MonarchTokens.Space.space4.dp
    val SpinnerStroke: Dp = MonarchTokens.Space.space0_5.dp

    /** The width of the bar at the start of a selected row. */
    val SelectedBar: Dp = MonarchTokens.Space.space0_5.dp

    /** The highlight is 40dp inside the 48dp target: this much is left free at the top and at the bottom. */
    val HighlightInset: Dp = MonarchTokens.Space.space1.dp

    /** The corner radius of the highlight. */
    val HighlightRadius: Dp = MonarchTokens.Radius.md.dp

    /** The indent guide lines. */
    val GuideWidth: Dp = MonarchTokens.Space.px.dp

    /** The quarter turn of the expand icon when the parent is open. */
    const val EXPANDED_ROTATION: Float = 90f

    /**
     * The visible rows in reading order: each open parent is followed by its
     * children, and by a loading row when its children are not loaded and its
     * id is in [loading].
     */
    fun flatten(items: List<MonarchTreeViewItem>, expanded: Set<String>, loading: Set<String>): List<TreeRow> {
        val rows = mutableListOf<TreeRow>()
        fun walk(list: List<MonarchTreeViewItem>, level: Int, parentId: String?) {
            list.forEach { item ->
                rows += TreeRow(item, level, parentId)
                if (item.isParent && item.id in expanded) {
                    val children = item.children
                    when {
                        children != null -> walk(children, level + 1, item.id)
                        item.id in loading -> rows += TreeRow(item, level + 1, item.id, loadingRow = true)
                    }
                }
            }
        }
        walk(items, 1, null)
        return rows
    }

    /** The ids of every parent in the tree, for a tree that starts fully expanded. */
    fun parentIds(items: List<MonarchTreeViewItem>): Set<String> = buildSet {
        fun walk(list: List<MonarchTreeViewItem>) {
            list.forEach { item ->
                if (item.isParent) add(item.id)
                item.children?.let(::walk)
            }
        }
        walk(items)
    }

    /** The selection after the user activates [id]: replaced in single mode, toggled in multiple mode. */
    fun select(mode: MonarchTreeViewSelectionMode, selected: Set<String>, id: String): Set<String> = when (mode) {
        MonarchTreeViewSelectionMode.None -> selected
        MonarchTreeViewSelectionMode.Single -> setOf(id)
        MonarchTreeViewSelectionMode.Multiple -> if (id in selected) selected - id else selected + id
    }

    /** The expanded set after [item] is opened or closed. */
    fun toggle(expanded: Set<String>, item: MonarchTreeViewItem): Set<String> =
        if (item.id in expanded) expanded - item.id else expanded + item.id

    /** Whether opening [item] needs its children loaded first. */
    fun needsLoad(item: MonarchTreeViewItem, expanded: Set<String>): Boolean =
        item.hasChildren && item.children == null && item.id !in expanded

    /**
     * The colours of a row: a selected row has the subtle accent fill, strong
     * accent text and a solid accent bar at its start; the current row strong
     * accent text; a disabled row disabled text.
     */
    fun colors(selected: Boolean, current: Boolean, enabled: Boolean, colors: MonarchColors): TreeRowColors = when {
        !enabled -> TreeRowColors(Color.Transparent, colors.textDisabled, colors.textDisabled, null)
        selected -> TreeRowColors(colors.accentSubtle, colors.accentStrong, colors.textMuted, colors.accentSolid)
        current -> TreeRowColors(Color.Transparent, colors.accentStrong, colors.textMuted, null)
        else -> TreeRowColors(Color.Transparent, colors.textDefault, colors.textMuted, null)
    }

    /** The colour of the expand icon. */
    fun iconColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled
}
