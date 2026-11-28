package dev.monarchui.components.datadisplay.treeview

import androidx.compose.runtime.Immutable

/**
 * One node of a `MonarchTreeView`.
 *
 * @property id a stable id, unique in the whole tree.
 * @property label the visible name, read by TalkBack.
 * @property description a second line under the label, for example "Department · 12 people".
 * @property children the child items. An empty list is a parent with no children; `null` is a leaf unless
 * [hasChildren] is set.
 * @property hasChildren the item has children that are not loaded yet; expanding it calls `onLoadChildren`.
 * @property enabled `false` makes the item impossible to select, activate or expand.
 */
@Immutable
public class MonarchTreeViewItem(
    public val id: String,
    public val label: String,
    public val description: String? = null,
    public val children: List<MonarchTreeViewItem>? = null,
    public val hasChildren: Boolean = false,
    public val enabled: Boolean = true,
) {
    /** Whether the item can be expanded: it has children, loaded or not. */
    public val isParent: Boolean get() = children != null || hasChildren
}
