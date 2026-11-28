package dev.monarchui.components.actions.button

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.theme.MonarchTheme

/**
 * A button with a text label.
 *
 * The Monarch button, built from its own layers (not the Material 3 `Button`, whose 40dp minimum height
 * would rule out the web's 32 and 36dp buttons): the variants and sizes of the web, the hover, pressed,
 * focus and disabled looks, a loading state and the ripple. The visible height is 32, 36 or 40dp; the layout
 * reserves a 48dp touch target around it and a tap in that space presses the button. The label wraps and
 * the button grows instead of cutting the label off when the font size setting is large.
 *
 * @param text the label.
 * @param onClick called when the user activates the button. Not called while [loading] or when not [enabled].
 * @param modifier the modifier of the button.
 * @param variant the visual weight.
 * @param size the visible height. The touch target is at least 48dp at every size.
 * @param enabled whether the user can activate the button.
 * @param loading shows a spinner and ignores taps. The button keeps its size (the spinner replaces the leading
 * icon, or is drawn over the label when there is no icon), keeps its place in the focus order, and TalkBack
 * announces [loadingLabel] as its state.
 * @param leadingIcon an icon before the label, for example a `MonarchIcon` with a `null` content description.
 * @param trailingIcon an icon after the label.
 * @param loadingLabel the state announced while [loading]. `null` uses the built-in string
 * `monarch_button_loading`.
 * @param shape the outline of the button and its focus ring. `null` uses the medium Monarch shape. A button
 * group passes the shape of each position (first, middle, last).
 * @param interactionSource the source of the press, focus and hover interactions of the button.
 */
@Composable
public fun MonarchButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MonarchButtonVariant = MonarchButtonVariant.Secondary,
    size: MonarchButtonSize = MonarchButtonSize.Md,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    loadingLabel: String? = null,
    shape: Shape? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val hasLeadingIcon = leadingIcon != null
    MonarchButtonImpl(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        loading = loading,
        loadingLabel = loadingLabel,
        interactionSource = interactionSource,
        shape = shape,
        spinnerOverContent = !hasLeadingIcon,
    ) {
        MonarchButtonLabel(
            text = text,
            // With an icon the spinner takes its place, so the button keeps its width. Without one the
            // spinner is drawn over the label (see MonarchButtonSurface), which also keeps the width.
            leadingIcon = if (loading && hasLeadingIcon) ({ ButtonSpinner() }) else leadingIcon,
            trailingIcon = trailingIcon,
        )
    }
}

/**
 * A button with custom content.
 *
 * Use this overload when the label is not a plain string, for example styled
 * text or a label with a count. The content is laid out in a row, centred,
 * with the Monarch gap between its children. Prefer the text overload when a
 * string is enough.
 *
 * @param onClick called when the user activates the button. Not called while [loading] or when not [enabled].
 * @param modifier the modifier of the button.
 * @param variant the visual weight.
 * @param size the visible height. The touch target is at least 48dp at every size.
 * @param enabled whether the user can activate the button.
 * @param loading draws a spinner over the content, which stays in place and keeps the size of the button,
 * and ignores taps.
 * @param loadingLabel the state announced while [loading]. `null` uses the built-in string
 * `monarch_button_loading`.
 * @param shape the outline of the button and its focus ring. `null` uses the medium Monarch shape. A button
 * group passes the shape of each position (first, middle, last).
 * @param interactionSource the source of the press, focus and hover interactions of the button.
 * @param content the content of the button. Text inside uses the label style and the content colour of
 * the variant.
 */
@Composable
public fun MonarchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MonarchButtonVariant = MonarchButtonVariant.Secondary,
    size: MonarchButtonSize = MonarchButtonSize.Md,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingLabel: String? = null,
    shape: Shape? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MonarchButtonImpl(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        loading = loading,
        loadingLabel = loadingLabel,
        interactionSource = interactionSource,
        shape = shape,
        spinnerOverContent = true,
        content = content,
    )
}

@Composable
@Suppress("LongParameterList")
private fun MonarchButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    variant: MonarchButtonVariant,
    size: MonarchButtonSize,
    enabled: Boolean,
    loading: Boolean,
    loadingLabel: String?,
    shape: Shape?,
    interactionSource: MutableInteractionSource?,
    spinnerOverContent: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    MonarchButtonSurface(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        loading = loading,
        loadingLabel = loadingLabel,
        shape = shape ?: MonarchTheme.shapes.md,
        interactionSource = interactionSource,
        touchTarget = Modifier.monarchMinimumTouchTarget(),
        minHeight = MonarchButtonTokens.minHeight(size),
        spinnerOverContent = spinnerOverContent,
        content = content,
    )
}
