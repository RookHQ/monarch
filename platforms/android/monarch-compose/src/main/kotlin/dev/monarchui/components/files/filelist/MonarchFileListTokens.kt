package dev.monarchui.components.files.filelist

import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and selection rules of MonarchFileList, built from primitives only. */
internal object MonarchFileListTokens {
    /** The space between files and between the header and the files. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The narrowest grid column at the default font size. */
    val GridMinCell: Dp = MonarchTokens.Space.space24.dp + MonarchTokens.Space.space16.dp

    /** The narrowest grid column at [fontScale]: columns widen with the text so names are not cut. */
    fun gridMinCell(fontScale: Float): Dp = GridMinCell * fontScale.coerceAtLeast(1f)

    /** The ids of the files that can be selected. */
    fun selectable(items: List<MonarchFileListItem>): List<String> = items.filter { it.enabled }.map { it.id }

    /** How many of [items] are selected. Ids of files no longer in the list are not counted. */
    fun count(items: List<MonarchFileListItem>, selected: Set<String>): Int = items.count { it.id in selected }

    /** Select all: checked when every selectable file is, mixed when some files are, otherwise unchecked. */
    fun selectAllState(items: List<MonarchFileListItem>, selected: Set<String>): ToggleableState {
        val selectable = selectable(items)
        return when {
            selectable.isNotEmpty() && selectable.all { it in selected } -> ToggleableState.On
            count(items, selected) > 0 -> ToggleableState.Indeterminate
            else -> ToggleableState.Off
        }
    }

    /** The selection after select all is pressed: empty when every selectable file was selected, else all of them. */
    fun toggleAll(items: List<MonarchFileListItem>, selected: Set<String>): Set<String> =
        if (selectAllState(items, selected) == ToggleableState.On) emptySet() else selectable(items).toSet()

    /** The selection after one file is checked or unchecked. */
    fun toggle(selected: Set<String>, id: String, on: Boolean): Set<String> = if (on) selected + id else selected - id
}
