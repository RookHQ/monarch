package dev.monarchui.components.overlays.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.overlays.dialog.MonarchDialogTokens
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens
import kotlinx.coroutines.launch

/**
 * A modal sheet that rises from the bottom of the screen, for a task that
 * belongs to the current screen: a short form, the details of a row, a list
 * of choices.
 *
 * It is the Material 3 `ModalBottomSheet`, so the platform keeps the window,
 * the scrim, dragging, back, predictive back and the slide motion (none when
 * animations are off). Monarch draws the sheet: the raised background, which
 * is a lighter step than the screen in a dark theme, 16dp top corners, a
 * no shadow (a dark theme lifts it by the surface step), a Monarch handle, and a header with the
 * title at 18/600 and the description at 14 in the muted text colour. The
 * scrim is the overlay scrim role.
 *
 * Show it while a state is true and set the state to false in
 * [onDismissRequest]. To close it from a button with the slide, call
 * [MonarchBottomSheetScope.dismiss] in the content.
 *
 * TalkBack reads the title as the pane title when it opens. The handle is a
 * button that closes the sheet and has the Dismiss action (and Expand or
 * Collapse when the sheet is half open), so the sheet never depends on a drag.
 *
 * @param onDismissRequest called when the sheet has closed: a swipe down, back, a tap on the scrim, the handle,
 * or [MonarchBottomSheetScope.dismiss].
 * @param modifier the modifier of the sheet.
 * @param title the heading of the sheet, also its pane title. `null` for a sheet whose content has its own.
 * @param description a short line under the title, in the muted text colour.
 * @param dismissible whether a swipe, back and a tap on the scrim close it. Set it to false while work runs;
 * [MonarchBottomSheetScope.dismiss] still closes it.
 * @param showHandle whether the handle is shown.
 * @param fullyExpanded whether it opens fully expanded (the default), or half open first when its content is
 * taller than half the screen.
 * @param fullWidthContent whether the content spans the width of the sheet, for list rows that bring their own
 * 16dp padding (a `MonarchListRow`); the header then starts at 16dp too, so titles and rows share one edge. By
 * default the content has 20dp at the sides, like the header.
 * @param closeButton draws a Close button in the top corner of the header, as the web sheet does. It closes the
 * sheet with the slide. Needs a [title].
 * @param content the content under the header, in a column with 20dp under it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
public fun MonarchBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    dismissible: Boolean = true,
    showHandle: Boolean = true,
    fullyExpanded: Boolean = true,
    fullWidthContent: Boolean = false,
    closeButton: Boolean = false,
    content: @Composable MonarchBottomSheetScope.() -> Unit,
) {
    val currentDismissible by rememberUpdatedState(dismissible)
    var closing by remember { mutableStateOf(false) }
    val state = rememberModalBottomSheetState(
        skipPartiallyExpanded = fullyExpanded,
        confirmValueChange = { value -> closing || currentDismissible || value != SheetValue.Hidden },
    )
    val scope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    val dismiss: () -> Unit = {
        closing = true
        scope.launch { state.hide() }.invokeOnCompletion { if (!state.isVisible) currentOnDismiss() }
    }
    MonarchModalSheet(
        onDismissRequest = onDismissRequest,
        state = state,
        modifier = modifier,
        dismissible = dismissible,
        showHandle = showHandle,
    ) {
        MonarchBottomSheetBody(
            title,
            description,
            topPadding = !showHandle,
            fullWidthContent = fullWidthContent,
            onClose = if (closeButton) dismiss else null,
        ) {
            MonarchBottomSheetScopeImpl(this, dismiss).content()
        }
    }
}

/**
 * The Material 3 `ModalBottomSheet` in Monarch dress: the raised surface (or
 * [containerColor]) with 16dp top corners, no shadow, the
 * overlay scrim and the Monarch handle. Shared by MonarchBottomSheet and the
 * picker sheets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
internal fun MonarchModalSheet(
    onDismissRequest: () -> Unit,
    state: SheetState,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    showHandle: Boolean = true,
    containerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MonarchBottomSheetTokens.colors(MonarchTheme.colors)
    val container = containerColor ?: colors.container
    val shape = MonarchBottomSheetTokens.shape(MonarchTheme.shapes)
    // The sheet window has its own configuration: keep the one of the caller, so dates use its locale.
    val configuration = LocalConfiguration.current
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = state,
        sheetGesturesEnabled = dismissible,
        shape = shape,
        containerColor = container,
        contentColor = colors.content,
        scrimColor = MonarchTheme.colors.overlayScrim,
        dragHandle = if (showHandle) {
            { MonarchBottomSheetHandle() }
        } else {
            null
        },
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = dismissible,
            shouldDismissOnClickOutside = dismissible,
        ),
        content = { CompositionLocalProvider(LocalConfiguration provides configuration) { content() } },
    )
}

/** The handle: a short pill centred in a 48dp touch target. Material makes it a button with the sheet actions. */
@Composable
internal fun MonarchBottomSheetHandle() {
    val description = stringResource(R.string.monarch_bottomsheet_drag_handle)
    Box(
        modifier = Modifier
            .monarchMinimumTouchTarget()
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(MonarchBottomSheetTokens.HandleWidth, MonarchBottomSheetTokens.HandleHeight)
                .background(MonarchBottomSheetTokens.handleColor(MonarchTheme.colors), MonarchTheme.shapes.full),
        )
    }
}

/** The header and the padded content of a sheet. */
@Composable
internal fun MonarchBottomSheetBody(
    title: String?,
    description: String?,
    topPadding: Boolean,
    fullWidthContent: Boolean = false,
    onClose: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MonarchBottomSheetTokens
    val side = if (fullWidthContent) MonarchTokens.Space.space0.dp else tokens.Padding
    val headerSide = if (fullWidthContent) tokens.ListInset else MonarchTokens.Space.space0.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (title != null) Modifier.semantics { paneTitle = title } else Modifier)
            .padding(
                start = side,
                end = side,
                top = if (topPadding) tokens.Padding else MonarchTokens.Space.space0.dp,
                bottom = tokens.Padding,
            ),
        verticalArrangement = Arrangement.spacedBy(tokens.SectionGap),
    ) {
        if (title != null) {
            Row(
                modifier = Modifier.padding(horizontal = headerSide),
                horizontalArrangement = Arrangement.spacedBy(tokens.HeaderGap),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.HeaderGap)) {
                    MonarchText(text = title, style = MonarchTextStyle.Subheading, heading = true)
                    if (!description.isNullOrBlank()) {
                        Text(
                            text = description,
                            style = MonarchDialogTokens.descriptionStyle(MonarchTheme.typography),
                            color = MonarchTheme.colors.textMuted,
                        )
                    }
                }
                if (onClose != null) {
                    MonarchIconButton(label = stringResource(R.string.monarch_bottomsheet_close), onClick = onClose) {
                        MonarchDefaultIcons.Close()
                    }
                }
            }
        }
        Column(content = content)
    }
}
