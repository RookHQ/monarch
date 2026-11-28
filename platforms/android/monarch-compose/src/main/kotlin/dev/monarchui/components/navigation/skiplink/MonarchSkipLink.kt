package dev.monarchui.components.navigation.skiplink

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.internal.monarchString

/**
 * An action that moves focus past repeated navigation to the main content of a screen, for keyboard, switch access
 * and TalkBack users: the Android form of the web's skip link.
 *
 * A small secondary `MonarchButton`, read first in its traversal group. Activating it requests focus on the
 * element marked with `Modifier.monarchSkipLinkTarget(target)`, so the next key press or swipe continues from the
 * main content. Put it at the top of a screen whose navigation (a long toolbar, a rail of links) comes before the
 * content.
 *
 * @param target the requester of the main content, shared with `Modifier.monarchSkipLinkTarget`.
 * @param modifier the modifier of the button.
 * @param label the label. `null` uses "Skip to main content".
 * @param onSkip called after focus moved, for example to scroll the content into view.
 * @param interactionSource the source of the press and focus interactions.
 */
@Composable
public fun MonarchSkipLink(
    target: FocusRequester,
    modifier: Modifier = Modifier,
    label: String? = null,
    onSkip: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    MonarchButton(
        text = monarchString(label, R.string.monarch_skiplink_label),
        onClick = {
            target.requestFocus()
            onSkip?.invoke()
        },
        modifier = modifier.semantics { traversalIndex = MonarchSkipLinkTokens.TRAVERSAL_INDEX },
        variant = MonarchSkipLinkTokens.Variant,
        size = MonarchSkipLinkTokens.Size,
        interactionSource = interactionSource,
    )
}
