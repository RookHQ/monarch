package dev.monarchui.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.theme.MonarchTheme

/**
 * A remove or dismiss button: a small ghost `MonarchIconButton` with [icon],
 * or a small ghost `MonarchButton` that shows [text] without one, because
 * Monarch bundles no close icon. TalkBack reads [name] either way. Both
 * reserve a 48dp touch target. Shared by the workflow editors, the callout banner and
 * the inline notice.
 *
 * @param name what TalkBack reads, such as "Remove condition 2".
 * @param text the visible text of the text button, such as "Remove".
 * @param onClick called when the user activates the button.
 * @param enabled whether the user can activate it.
 * @param icon the mark, decorative; `null` for a text button.
 * @param modifier the modifier of the button.
 */
@Composable
internal fun MonarchRemoveButton(
    name: String,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    icon: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (icon != null) {
        // A quiet mark: the muted text colour while enabled; disabled is the button's own disabled look.
        MonarchIconButton(
            label = name,
            onClick = onClick,
            modifier = modifier,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
        ) {
            // The icon is decoration: the button is read by [name] only.
            Box(Modifier.clearAndSetSemantics { }) {
                if (enabled) {
                    CompositionLocalProvider(LocalContentColor provides MonarchTheme.colors.textMuted) { icon() }
                } else {
                    icon()
                }
            }
        }
    } else {
        MonarchButton(
            text = text,
            onClick = onClick,
            modifier = if (name == text) modifier else modifier.semantics { contentDescription = name },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
        )
    }
}
