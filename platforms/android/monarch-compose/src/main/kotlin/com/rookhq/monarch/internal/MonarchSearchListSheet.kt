package com.rookhq.monarch.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchModalSheet
import com.rookhq.monarch.components.search.searchbar.MonarchSearchBar
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle

/**
 * The bottom-sheet search list of the pickers (combobox, multi-select,
 * entity picker, switchers): the Monarch modal sheet (a Material 3 modal
 * bottom sheet with the Monarch surface, corners, scrim and handle), expanded
 * at once, with the title as a heading, an optional status line, a
 * `MonarchSearchBar`, a lazy list that takes the height that is left, and the
 * buttons under it. The caller owns the query, the rows and the buttons.
 *
 * @param title the heading of the sheet, usually the label of the field.
 * @param query the search text.
 * @param onQueryChange called with the new search text.
 * @param onDismiss called when the person closes the sheet by swiping it down, tapping outside or Back.
 * @param searchPlaceholder the placeholder of the search field.
 * @param searching shows the search field's busy state.
 * @param containerColor the colour of the sheet.
 * @param gap the gap between the parts of the sheet.
 * @param status an optional line under the title, such as "2 of 5 chosen".
 * @param buttons the buttons under the list, such as Cancel or Done; in a column, so they can align.
 * @param list the rows of the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
internal fun MonarchSearchListSheet(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    searchPlaceholder: String?,
    searching: Boolean,
    containerColor: Color = MonarchSearchListSheetDefaults.containerColor(),
    gap: Dp = MonarchSearchListSheetDefaults.Gap,
    status: (@Composable () -> Unit)? = null,
    buttons: @Composable ColumnScope.() -> Unit,
    list: LazyListScope.() -> Unit,
) {
    MonarchModalSheet(
        onDismissRequest = onDismiss,
        state = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = containerColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = MonarchSearchListSheetDefaults.Padding),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            MonarchText(text = title, style = MonarchTextStyle.Subheading, heading = true)
            status?.invoke()
            MonarchSearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {},
                modifier = Modifier.fillMaxWidth(),
                pending = searching,
                placeholder = searchPlaceholder,
            )
            LazyColumn(modifier = Modifier.weight(1f, fill = false), content = list)
            buttons()
        }
    }
}
