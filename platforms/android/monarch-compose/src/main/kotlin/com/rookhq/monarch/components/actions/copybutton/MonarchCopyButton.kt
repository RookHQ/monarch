package com.rookhq.monarch.components.actions.copybutton

import android.os.Build
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A button that copies a string to the clipboard.
 *
 * A `MonarchButton` that writes [value] as plain text with the Compose
 * `Clipboard`, plays the confirm haptic and announces "Copied to clipboard"
 * to TalkBack. Android 13 and later show their own clipboard confirmation,
 * so by default the in-app copied state (the copied label and icon) is shown
 * only on earlier versions; see [showConfirmation].
 *
 * @param value the text to copy.
 * @param modifier the modifier of the button.
 * @param variant the visual weight.
 * @param size the visible height. The touch target is at least 48dp at every size.
 * @param enabled whether the user can copy.
 * @param sensitive marks the clip as sensitive, so Android 13 and later hide it in the clipboard preview.
 * Use it for passwords, keys and tokens.
 * @param showConfirmation shows the copied label and [copiedIcon] after a copy. By default only below
 * Android 13, where the system shows no confirmation of its own.
 * @param resetAfterMillis how long the copied state lasts. Zero keeps it until the button leaves the screen.
 * @param target what is copied, added to the spoken name: "Copy webhook URL".
 * @param leadingIcon an icon before the label, for example a copy `MonarchIcon` with a `null` content
 * description.
 * @param copiedIcon the icon shown in the copied state instead of [leadingIcon], for example a check mark.
 * @param onCopy called with the copied text after each copy.
 * @param label replaces the built-in label "Copy" (`monarch_copybutton_copy`).
 * @param copiedLabel replaces the built-in label "Copied" (`monarch_copybutton_copied`).
 * @param copiedAnnouncement replaces the built-in state "Copied to clipboard"
 * (`monarch_copybutton_copied_announcement`).
 * @param interactionSource the source of the press, focus and hover interactions of the button.
 */
@Composable
public fun MonarchCopyButton(
    value: String,
    modifier: Modifier = Modifier,
    variant: MonarchButtonVariant = MonarchButtonVariant.Secondary,
    size: MonarchButtonSize = MonarchButtonSize.Md,
    enabled: Boolean = true,
    sensitive: Boolean = false,
    showConfirmation: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU,
    resetAfterMillis: Long = MonarchCopyButtonTokens.DEFAULT_RESET_MILLIS,
    target: String? = null,
    leadingIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Copy,
    copiedIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Check,
    onCopy: ((String) -> Unit)? = null,
    label: String? = null,
    copiedLabel: String? = null,
    copiedAnnouncement: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val idle = monarchString(label, R.string.monarch_copybutton_copy)
    val done = monarchString(copiedLabel, R.string.monarch_copybutton_copied)
    val announcement = monarchString(copiedAnnouncement, R.string.monarch_copybutton_copied_announcement)
    val state = rememberMonarchCopyButtonState(value, target ?: idle, sensitive, resetAfterMillis, onCopy)
    val shown = MonarchCopyButtonTokens.showsCopied(state.copied, showConfirmation)
    val name = spokenName(if (shown) done else idle, target)
    MonarchButton(
        onClick = state.copy,
        modifier = modifier.semantics {
            contentDescription = name
            if (state.copied) stateDescription = announcement
            liveRegion = LiveRegionMode.Polite
        },
        variant = variant,
        size = size,
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        (if (shown) copiedIcon ?: leadingIcon else leadingIcon)?.invoke()
        // Both labels share one cell, so the button keeps the width of the longer one and does not jump.
        Box(modifier = Modifier.weight(1f, fill = false)) {
            CopyLabel(text = idle, visible = !shown)
            CopyLabel(text = done, visible = shown)
        }
    }
}

/**
 * A copy button that shows only an icon, such as next to an API key.
 *
 * A `MonarchIconButton` with the same behaviour as the text overload. The
 * label is still the spoken name, so give it the object with [target]:
 * "Copy API key". Without a [copiedIcon] the copied state is only spoken.
 *
 * @param value the text to copy.
 * @param icon the copy icon, for example a `MonarchIcon` with a `null` content description.
 * @param modifier the modifier of the button.
 * @param enabled whether the user can copy.
 * @param sensitive marks the clip as sensitive, so Android 13 and later hide it in the clipboard preview.
 * @param showConfirmation shows [copiedIcon] after a copy. By default only below Android 13.
 * @param resetAfterMillis how long the copied state lasts. Zero keeps it.
 * @param target what is copied, added to the spoken name.
 * @param copiedIcon the icon shown in the copied state, for example a check mark.
 * @param onCopy called with the copied text after each copy.
 * @param label replaces the built-in spoken name "Copy".
 * @param copiedLabel replaces the built-in spoken name "Copied" of the copied state.
 * @param copiedAnnouncement replaces the built-in state "Copied to clipboard".
 * @param interactionSource the source of the press, focus and hover interactions of the button.
 * @param variant the button style; ghost by default, as the web's icon-only copy button.
 * @param size the button size; the web's icon-only copy button is `Sm`.
 */
@Composable
public fun MonarchCopyButton(
    value: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sensitive: Boolean = false,
    showConfirmation: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU,
    resetAfterMillis: Long = MonarchCopyButtonTokens.DEFAULT_RESET_MILLIS,
    target: String? = null,
    copiedIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Check,
    onCopy: ((String) -> Unit)? = null,
    label: String? = null,
    copiedLabel: String? = null,
    copiedAnnouncement: String? = null,
    interactionSource: MutableInteractionSource? = null,
    variant: MonarchButtonVariant = MonarchButtonVariant.Ghost,
    size: MonarchButtonSize = MonarchButtonSize.Md,
) {
    val idle = monarchString(label, R.string.monarch_copybutton_copy)
    val done = monarchString(copiedLabel, R.string.monarch_copybutton_copied)
    val announcement = monarchString(copiedAnnouncement, R.string.monarch_copybutton_copied_announcement)
    val state = rememberMonarchCopyButtonState(value, target ?: idle, sensitive, resetAfterMillis, onCopy)
    val shown = MonarchCopyButtonTokens.showsCopied(state.copied, showConfirmation)
    val name = spokenName(if (shown) done else idle, target)
    val shape = MonarchShapeDefaults.Mark
    val colors = MonarchTheme.colors
    MonarchIconButton(
        label = name,
        onClick = state.copy,
        modifier = modifier.semantics {
            if (state.copied) stateDescription = announcement
            liveRegion = LiveRegionMode.Polite
        },
        variant = variant,
        size = size,
        enabled = enabled,
        shape = shape,
        interactionSource = interactionSource,
    ) {
        val tint = MonarchCopyButtonTokens.iconColor(enabled, colors)
        Box(Modifier.clearAndSetSemantics { }) {
            CompositionLocalProvider(LocalContentColor provides tint) {
                if (shown && copiedIcon != null) copiedIcon() else icon()
            }
        }
    }
}

@Composable
private fun spokenName(label: String, target: String?): String =
    if (target == null) label else stringResource(R.string.monarch_copybutton_name, label, target)

/** One of the two labels; the hidden one keeps its size but is not drawn or read. */
@Composable
private fun CopyLabel(text: String, visible: Boolean) {
    Text(
        text = text,
        modifier = Modifier
            .drawWithContent { if (visible) drawContent() }
            .clearAndSetSemantics { },
        textAlign = TextAlign.Center,
    )
}
