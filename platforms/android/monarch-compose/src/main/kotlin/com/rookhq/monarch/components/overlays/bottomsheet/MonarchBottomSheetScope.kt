package com.rookhq.monarch.components.overlays.bottomsheet

import androidx.compose.foundation.layout.ColumnScope

/** The content scope of a MonarchBottomSheet: a column that can also close the sheet. */
public interface MonarchBottomSheetScope : ColumnScope {
    /**
     * Slides the sheet down with the platform motion and then calls the
     * sheet's `onDismissRequest`, for a button such as Save or Cancel. Works
     * also when the sheet is not dismissible by gestures.
     */
    public fun dismiss()
}

internal class MonarchBottomSheetScopeImpl(
    column: ColumnScope,
    private val onDismiss: () -> Unit,
) : MonarchBottomSheetScope, ColumnScope by column {
    override fun dismiss(): Unit = onDismiss()
}
