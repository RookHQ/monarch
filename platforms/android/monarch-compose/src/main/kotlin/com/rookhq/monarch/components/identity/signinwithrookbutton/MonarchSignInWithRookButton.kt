package com.rookhq.monarch.components.identity.signinwithrookbutton

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.internal.MonarchCentredText
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchControlState
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The "Sign in with Rook ID" button, for an app that offers Rook ID sign-in.
 *
 * Rook-scoped. A Monarch-drawn button in a capsule with the fixed Rook ID
 * looks: the `Light`, `Neutral` and `Dark` variants draw in the light or dark
 * theme whatever the theme of the screen (in its high-contrast form when the
 * screen uses one), so the button looks the same everywhere; `Auto` follows
 * the screen. The button only calls [onClick]: the app runs the sign-in, for
 * example in a Custom Tab. While [loading] it shows a progress indicator in
 * place of the mark, reads "Loading" and ignores taps.
 *
 * @param onClick called when the person activates the button. Not called while [loading] or when not
 * [enabled].
 * @param modifier the modifier of the button.
 * @param variant the look: light, dark and neutral are fixed; auto follows the theme.
 * @param size the visible height: 32, 40 or 48dp. The touch target is at least 48dp at every size.
 * @param enabled whether the person can activate the button.
 * @param loading shows a progress indicator in place of the mark and ignores taps.
 * @param fullWidth fills the available width.
 * @param mark the Rook ID mark, drawn before the label at 16, 20 or 24dp without a tint. Decorative.
 * Monarch ships no artwork, so the app passes it, for example with `painterResource`.
 * @param label the visible label. `null` uses the built-in "Sign in with Rook ID"
 * (`monarch_signinwithrookbutton_label`).
 * @param loadingLabel the state read while [loading]. `null` uses the built-in "Loading"
 * (`monarch_signinwithrookbutton_loading`).
 * @param interactionSource the source of the press, focus and hover interactions of the button.
 */
@Composable
public fun MonarchSignInWithRookButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MonarchSignInWithRookButtonVariant = MonarchSignInWithRookButtonVariant.Light,
    size: MonarchSignInWithRookButtonSize = MonarchSignInWithRookButtonSize.Md,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false,
    mark: Painter? = null,
    label: String? = null,
    loadingLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val scheme = MonarchSignInWithRookButtonTokens.pinnedScheme(variant)
    val button = @Composable {
        SignInButton(
            onClick, modifier, variant, size, enabled, loading, fullWidth, mark, label, loadingLabel,
            interactionSource,
        )
    }
    if (scheme == null) {
        button()
    } else {
        MonarchTheme(
            scheme = scheme,
            contrast = MonarchSignInWithRookButtonTokens.pinnedContrast(MonarchTheme.isHighContrast),
            typography = MonarchTheme.typography,
            content = button,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun SignInButton(
    onClick: () -> Unit,
    modifier: Modifier,
    variant: MonarchSignInWithRookButtonVariant,
    size: MonarchSignInWithRookButtonSize,
    enabled: Boolean,
    loading: Boolean,
    fullWidth: Boolean,
    mark: Painter?,
    label: String?,
    loadingLabel: String?,
    interactionSource: MutableInteractionSource?,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val state = rememberMonarchControlState(source, enabled)
    val colors = MonarchSignInWithRookButtonTokens.colors(variant, enabled, state.pressed, MonarchTheme.colors)
    val shape = MonarchTheme.shapes.full
    val busyLabel = monarchString(loadingLabel, R.string.monarch_signinwithrookbutton_loading)
    val text = monarchString(label, R.string.monarch_signinwithrookbutton_label)
    val markSize = MonarchSignInWithRookButtonTokens.markSize(size)
    val border = colors.border?.let { Modifier.border(MonarchSignInWithRookButtonTokens.BorderWidth, it, shape) }
    // Drawn by Monarch like MonarchButton: the press look comes from the colours and the 48dp target is reserved
    // outside the focus ring.
    CompositionLocalProvider(LocalContentColor provides colors.content) {
        Row(
            modifier = modifier
                .monarchMinimumTouchTarget()
                .monarchFocusRing(shape)
                .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
                .heightIn(min = MonarchSignInWithRookButtonTokens.minHeight(size))
                .clip(shape)
                .background(colors.container, shape)
                .then(border ?: Modifier)
                .clickable(
                    interactionSource = source,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = { if (!loading) onClick() },
                )
                .semantics {
                    if (loading) {
                        stateDescription = busyLabel
                        disabled()
                    }
                }
                .padding(MonarchSignInWithRookButtonTokens.contentPadding(size)),
            horizontalArrangement = Arrangement.spacedBy(
                MonarchSignInWithRookButtonTokens.gap(size),
                Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    // The state is announced on the button; the indicator is decoration.
                    modifier = Modifier.size(markSize).clearAndSetSemantics { },
                    color = LocalContentColor.current,
                    strokeWidth = MonarchSignInWithRookButtonTokens.SpinnerStrokeWidth,
                )
            } else if (mark != null) {
                Image(painter = mark, contentDescription = null, modifier = Modifier.size(markSize))
            }
            // Centred line by line once it wraps, as on the web; a single line is centred by the row.
            MonarchCentredText(
                text = text,
                modifier = Modifier.weight(1f, fill = false),
                style = MonarchSignInWithRookButtonTokens.textStyle(size, MonarchTheme.typography),
            )
        }
    }
}
