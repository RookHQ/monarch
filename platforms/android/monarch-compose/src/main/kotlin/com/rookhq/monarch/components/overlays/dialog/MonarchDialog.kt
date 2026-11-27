package com.rookhq.monarch.components.overlays.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.tokens.MonarchTokens
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.monarchShadow
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A modal dialog: a title, an optional description and content, and the
 * actions at the end, on a Monarch panel over the scrim.
 *
 * It is the Material 3 `BasicAlertDialog`, so the platform keeps the window,
 * the scrim, the focus trap, back and Escape, and the enter and exit motion
 * (none when animations are off). Monarch draws the panel: up to 512dp wide
 * and 16dp from the window edges, 16dp corners, the raised background with a
 * subtle border and one soft shadow (none in a dark theme), 20dp padding, the
 * title at 18/600 and the description at 14 in the muted text colour. The
 * scrim is the overlay scrim role.
 *
 * Show it while a state is true and set the state to false in
 * [onDismissRequest]. TalkBack moves into the dialog, reads the title as the
 * pane title and as a heading, and returns to the screen when it closes.
 *
 * ```
 * if (open) {
 *     MonarchDialog(
 *         onDismissRequest = { open = false },
 *         title = "Delete project?",
 *         description = "Atlas and its 214 tasks are deleted for everyone.",
 *         actions = {
 *             MonarchButton(text = "Cancel", onClick = { open = false }, variant = MonarchButtonVariant.Ghost)
 *             MonarchButton(text = "Delete", onClick = { delete() }, variant = MonarchButtonVariant.Danger)
 *         },
 *     )
 * }
 * ```
 *
 * @param onDismissRequest called when the person dismisses it with back, Escape or a tap on the scrim. Not called
 * when not [dismissible]. Buttons close it themselves.
 * @param title the heading, also the pane title TalkBack reads when the dialog opens. `null` for content that
 * brings its own heading (a celebration); then give [paneTitle].
 * @param modifier the modifier of the panel.
 * @param description a short line under the title, in the muted text colour.
 * @param dismissible whether back, Escape and a tap on the scrim dismiss it. Set it to false while work runs or
 * when a choice is required; the actions must then close it.
 * @param dismissOnClickOutside whether a tap on the scrim dismisses it, when [dismissible]. Set it to false for
 * a dialog with typed text, so a stray tap does not lose it; back and Escape still close it.
 * @param paneTitle the name TalkBack reads when the dialog opens, when there is no [title]. Required then.
 * @param closeButton draws a Close button in the top corner, as the web dialog does. It calls [onDismissRequest],
 * so it shows only when the dialog is [dismissible].
 * @param icon a decorative icon above the title, such as a warning sign for a confirmation. Monarch bundles no icons.
 * @param startActions a button or two at the start of the same row, opposite [actions]: the choice that sets the
 * dialog's value aside ("Remove date", "Delete draft"), as a third action next to Cancel and Save.
 * @param actions the buttons at the end, for example a ghost Cancel and a primary MonarchButton. Any number; they
 * wrap when there is no room.
 * @param content the body under the description, in a column that scrolls when the dialog is taller than the
 * window.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
public fun MonarchDialog(
    onDismissRequest: () -> Unit,
    title: String?,
    modifier: Modifier = Modifier,
    description: String? = null,
    dismissible: Boolean = true,
    dismissOnClickOutside: Boolean = dismissible,
    paneTitle: String? = null,
    closeButton: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
    startActions: (@Composable RowScope.() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val name = requireNotNull(title ?: paneTitle) { "MonarchDialog needs a title or a paneTitle" }
    val currentDismissible by rememberUpdatedState(dismissible)
    // The dialog window has its own configuration: keep the one of the caller, so dates in the content use its locale.
    val configuration = LocalConfiguration.current
    BasicAlertDialog(
        onDismissRequest = { if (currentDismissible) onDismissRequest() },
        // Outermost, so the title replaces Material's generic "Dialog" pane title.
        modifier = Modifier
            .semantics { this.paneTitle = name }
            .monarchWindowMargin(MonarchDialogTokens.WindowMargin),
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible && dismissOnClickOutside,
            usePlatformDefaultWidth = false,
        ),
    ) {
        MonarchDialogScrim()
        CompositionLocalProvider(LocalConfiguration provides configuration) {
            MonarchDialogPanel(modifier) {
                MonarchDialogBody(
                    title,
                    description,
                    actions,
                    content,
                    startActions,
                    icon,
                    onClose = if (closeButton && dismissible) onDismissRequest else null,
                )
            }
        }
    }
}

/**
 * Dims the window of the enclosing dialog by the alpha of the overlay scrim
 * role, in place of the platform's fixed dim. Call it inside a `Dialog`.
 */
@Composable
internal fun MonarchDialogScrim() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    val amount = MonarchDialogTokens.dimAmount(MonarchTheme.colors)
    SideEffect { window?.setDimAmount(amount) }
}

/** The Monarch dialog panel: width, corners, raised surface, border and the light-theme shadow. */
@Composable
internal fun MonarchDialogPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = MonarchDialogTokens.colors(MonarchTheme.colors, MonarchTheme.isHighContrast)
    val shape = MonarchTheme.shapes.xl
    val shadow = MonarchDialogTokens.shadow(MonarchTheme.elevation, MonarchTheme.isDark)
    val surface = Modifier
        .widthIn(max = MonarchDialogTokens.MaxWidth)
        .fillMaxWidth()
        .then(if (shadow != null) Modifier.monarchShadow(shadow, shape) else Modifier)
        .monarchSurface(colors, shape)
        .clip(shape)
    CompositionLocalProvider(LocalContentColor provides colors.content) {
        Column(modifier = surface.then(modifier)) { content() }
    }
}

/** The title, the description, the scrolling content and the actions of a dialog panel. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonarchDialogBody(
    title: String?,
    description: String?,
    actions: (@Composable RowScope.() -> Unit)?,
    content: (@Composable ColumnScope.() -> Unit)?,
    startActions: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    val tokens = MonarchDialogTokens
    val hasFooter = actions != null || startActions != null
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .padding(
                    start = tokens.Padding,
                    end = tokens.Padding,
                    top = tokens.Padding,
                    bottom = tokens.Padding,
                ),
            verticalArrangement = Arrangement.spacedBy(tokens.SectionGap),
        ) {
            if (icon != null) {
                Box(Modifier.clearAndSetSemantics { }) { icon() }
            }
            if (title != null || !description.isNullOrBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.HeaderGap), verticalAlignment = Alignment.Top) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(tokens.HeaderGap),
                    ) {
                        if (title != null) {
                            MonarchText(text = title, style = MonarchTextStyle.Subheading, heading = true)
                        }
                        if (!description.isNullOrBlank()) {
                            Text(
                                text = description,
                                style = tokens.descriptionStyle(MonarchTheme.typography),
                                color = MonarchTheme.colors.textMuted,
                            )
                        }
                    }
                    // The web draws a close button in the top corner of the dialog.
                    if (onClose != null) {
                        MonarchIconButton(label = stringResource(R.string.monarch_dialog_close), onClick = onClose) {
                            Box(Modifier.clearAndSetSemantics { }) { MonarchDefaultIcons.Close() }
                        }
                    }
                }
            }
            if (content != null) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    content = content,
                )
            }
        }
        if (hasFooter) MonarchDialogFooter(startActions, actions)
    }
}

/**
 * The footer band of the web dialog: a subtle background under a hairline, full width, 12dp above and below the
 * buttons and 20dp at the sides, the buttons at the end. Bordered Secondary buttons (Cancel) and the primary one
 * sit in it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonarchDialogFooter(
    startActions: (@Composable RowScope.() -> Unit)?,
    actions: (@Composable RowScope.() -> Unit)?,
) {
    val tokens = MonarchDialogTokens
    val colors = MonarchTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(MonarchShapeDefaults.BorderWidth).background(colors.borderSubtle))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgSubtle)
                .padding(horizontal = tokens.Padding, vertical = MonarchTokens.Space.space3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (startActions != null) {
                FlowRow(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(tokens.ActionGap),
                    verticalArrangement = Arrangement.spacedBy(tokens.ActionGap),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) { startActions() }
                Spacer(Modifier.weight(1f))
            }
            if (actions != null) {
                FlowRow(
                    modifier = if (startActions == null) Modifier.fillMaxWidth() else Modifier,
                    horizontalArrangement = Arrangement.spacedBy(tokens.ActionGap, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(tokens.ActionGap),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) { actions() }
            }
        }
    }
}
