package dev.monarchui.components.actions.splitbutton

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.overlays.menu.MonarchMenu

/**
 * A main action joined to an arrow button that opens a menu of related
 * alternatives, such as Save with Save as draft and Save and close.
 *
 * Built from two `MonarchButton`s in a row and a `MonarchMenu`
 * anchored under the arrow. The Material 3 `SplitButtonLayout` is still
 * experimental, so Monarch joins its own buttons with the same small gap. The
 * main button acts at once; the arrow is a separate TalkBack element named
 * "More options for" and the label, with the expand and collapse actions and
 * an expanded or collapsed state.
 *
 * @param text the main action's label.
 * @param onClick runs the main action. Not called while [loading] or when not [enabled].
 * @param arrowIcon the arrow of the menu button, for example a `MonarchIcon` of a chevron that points down with a
 * `null` content description. Monarch bundles no icons, so it is required.
 * @param modifier the modifier of the pair.
 * @param variant the visual weight of both buttons.
 * @param size the visible height of both buttons. The touch target is at least 48dp at every size.
 * @param enabled whether the main action and the menu can be used.
 * @param loading shows progress on the main button; the arrow is off meanwhile.
 * @param menuEnabled disables only the arrow, keeping the main action available.
 * @param leadingIcon an icon before the main label.
 * @param moreOptionsLabel the name of the arrow. `null` uses `monarch_splitbutton_more_options` with [text].
 * @param loadingLabel the state of the main button while [loading]. `null` uses the button's built-in string.
 * @param interactionSource the source of the interactions of the main button.
 * @param menuContent the menu items, usually `MonarchMenuItem`s. Call `close` from an item to close the menu.
 */
@Composable
public fun MonarchSplitButton(
    text: String,
    onClick: () -> Unit,
    arrowIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    variant: MonarchButtonVariant = MonarchButtonVariant.Secondary,
    size: MonarchButtonSize = MonarchButtonSize.Md,
    enabled: Boolean = true,
    loading: Boolean = false,
    menuEnabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    moreOptionsLabel: String? = null,
    loadingLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    menuContent: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val arrowEnabled = MonarchSplitButtonTokens.arrowEnabled(enabled, loading, menuEnabled)
    val expanded = open && arrowEnabled
    // A menu closed by a disabled arrow stays closed when the arrow comes back.
    LaunchedEffect(arrowEnabled) { if (!arrowEnabled) open = false }
    val arrowName = moreOptionsLabel ?: stringResource(R.string.monarch_splitbutton_more_options, text)
    val expandedState = stringResource(R.string.monarch_splitbutton_expanded)
    val collapsedState = stringResource(R.string.monarch_splitbutton_collapsed)
    Row(
        modifier = modifier.semantics { isTraversalGroup = true },
        horizontalArrangement = Arrangement.spacedBy(MonarchSplitButtonTokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchButton(
            text = text,
            onClick = onClick,
            // Measured after the arrow, so a long label wraps instead of pushing the arrow out.
            modifier = Modifier.weight(1f, fill = false),
            variant = variant,
            size = size,
            enabled = enabled,
            loading = loading,
            leadingIcon = leadingIcon,
            loadingLabel = loadingLabel,
            interactionSource = interactionSource,
        )
        Box {
            MonarchButton(
                onClick = { open = true },
                modifier = Modifier.semantics {
                    contentDescription = arrowName
                    stateDescription = if (expanded) expandedState else collapsedState
                    if (arrowEnabled) {
                        if (expanded) {
                            collapse {
                                open = false
                                true
                            }
                        } else {
                            expand {
                                open = true
                                true
                            }
                        }
                    }
                },
                variant = variant,
                size = size,
                enabled = arrowEnabled,
            ) {
                arrowIcon()
            }
            MonarchMenu(expanded = expanded, onDismissRequest = { open = false }) {
                menuContent { open = false }
            }
        }
    }
}
