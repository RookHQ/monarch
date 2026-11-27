package com.rookhq.monarch.components.overlays.responsivedialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.components.overlays.dialog.MonarchDialogBody

/**
 * A dialog that is a centred panel on a large window and a bottom sheet on a
 * phone, from one call.
 *
 * Built by Monarch because Compose has no adaptive sheet: below [sheetBelow]
 * (by the window size class of `currentWindowAdaptiveInfoV2`) it is a
 * `MonarchBottomSheet`, fully expanded, with the Monarch handle; at or above
 * it, a `MonarchDialog`. Both show the title, the description, the [content]
 * (scrolling when it is taller than the window) and the [actions] at the end,
 * on the raised Monarch surface.
 *
 * Show it while a state is true and set the state to false in
 * [onDismissRequest]. Both forms are modal, and TalkBack reads the title as
 * the pane title when it opens.
 *
 * @param onDismissRequest called when the person dismisses it: back, a tap outside, or a swipe down on the
 * sheet. Not called when not [dismissible].
 * @param title the heading, also the pane title TalkBack reads.
 * @param modifier the modifier of the dialog panel or the sheet.
 * @param sheetBelow the window width class below which it is a sheet.
 * @param dismissible whether back, a tap outside and a swipe can dismiss it. Set it to false while work runs
 * or when a choice is required; the actions must then close it.
 * @param showHandle whether the sheet shows the drag handle. The dialog never does.
 * @param description a line under the title in the muted text colour.
 * @param actions the buttons at the end, for example a ghost Cancel and a primary MonarchButton. They wrap
 * when there is no room.
 * @param content the body, in a column that scrolls when it is too tall.
 */
@Composable
public fun MonarchResponsiveDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    sheetBelow: MonarchResponsiveDialogBreakpoint = MonarchResponsiveDialogBreakpoint.Medium,
    dismissible: Boolean = true,
    showHandle: Boolean = true,
    description: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheet = MonarchResponsiveDialogTokens.usesSheet(currentWindowAdaptiveInfoV2().windowSizeClass, sheetBelow)
    if (sheet) {
        MonarchBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            title = title,
            description = description,
            dismissible = dismissible,
            showHandle = showHandle,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                content = content,
            )
            if (actions != null) MonarchResponsiveDialogActions(actions)
        }
    } else {
        MonarchDialog(
            onDismissRequest = onDismissRequest,
            title = title,
            modifier = modifier,
            description = description,
            dismissible = dismissible,
            actions = actions,
            content = content,
        )
    }
}

/** The title, the description, the scrolling content and the actions, as the dialog shows them. */
@Composable
internal fun MonarchResponsiveDialogBody(
    title: String,
    description: String?,
    actions: (@Composable RowScope.() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    MonarchDialogBody(title, description, actions, content)
}

/** The actions of the sheet, at the end, wrapping when there is no room. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonarchResponsiveDialogActions(actions: @Composable RowScope.() -> Unit) {
    val tokens = MonarchResponsiveDialogTokens
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = tokens.SectionGap),
        horizontalArrangement = Arrangement.spacedBy(tokens.ActionGap, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(tokens.ActionGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) { actions() }
}
