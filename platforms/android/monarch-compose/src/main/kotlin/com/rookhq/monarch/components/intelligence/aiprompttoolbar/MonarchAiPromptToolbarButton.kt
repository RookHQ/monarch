package com.rookhq.monarch.components.intelligence.aiprompttoolbar

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant

/**
 * A compact ghost button for `MonarchAiPromptToolbar`, with an optional icon
 * and a label that can be hidden.
 *
 * A `MonarchButton` in the ghost variant at the small size, so it keeps the
 * Material 3 button role and a 48dp touch target. With [hideLabel] only the
 * icon is drawn and [text] becomes the TalkBack name.
 *
 * @param text the label, and always the TalkBack name.
 * @param onClick called when the user activates the button. Not called when not [enabled].
 * @param modifier the modifier of the button.
 * @param enabled whether the user can activate the button.
 * @param hideLabel shows only [icon]. Ignored without an icon, because the button would be empty.
 * @param icon an icon before the label, for example a `MonarchIcon` with a `null` content description. Monarch
 * bundles no icons.
 * @param interactionSource the source of the press, focus and hover interactions of the button.
 */
@Composable
public fun MonarchAiPromptToolbarButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hideLabel: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    if (hideLabel && icon != null) {
        MonarchButton(
            onClick = onClick,
            modifier = modifier.semantics { contentDescription = text },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
            interactionSource = interactionSource,
        ) { icon() }
    } else {
        MonarchButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
            leadingIcon = icon,
            interactionSource = interactionSource,
        )
    }
}
