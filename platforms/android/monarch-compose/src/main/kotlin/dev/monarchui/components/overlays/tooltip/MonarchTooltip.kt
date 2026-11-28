package dev.monarchui.components.overlays.tooltip

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.theme.MonarchTheme

/**
 * A short label that names a control for people who can see it, shown on a
 * long press, a pointer hover or keyboard focus: the action of an icon-only
 * button, the meaning of a status mark.
 *
 * It is the Material 3 `TooltipBox` with a `PlainTooltip`, so the platform
 * keeps the triggers, the timing, the position next to the control and the
 * fade (none when animations are off). Monarch draws the label: the inverse
 * background with the inverse text at 12, 6dp corners, 4 by 8dp of padding,
 * up to 288dp wide.
 *
 * The tooltip is not the name of the control for TalkBack: give the control
 * the same text as its label or content description (a `MonarchIconButton`
 * does this with its `label`).
 *
 * @param text the label, a few words.
 * @param modifier the modifier of the box around the control.
 * @param side the side of the control the label prefers.
 * @param enabled whether the label shows; `false` keeps the control without one.
 * @param initiallyVisible shows the label from the first frame and keeps it (the docs preview shows it, as the web).
 * @param content the control.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MonarchTooltip(
    text: String,
    modifier: Modifier = Modifier,
    side: MonarchTooltipSide = MonarchTooltipSide.Above,
    enabled: Boolean = true,
    initiallyVisible: Boolean = false,
    content: @Composable () -> Unit,
) {
    val state = rememberTooltipState(initialIsVisible = initiallyVisible, isPersistent = initiallyVisible)
    if (initiallyVisible) LaunchedEffect(Unit) { state.show() }
    MonarchTooltipBox(text, state, modifier, side, enabled, content)
}

/** [MonarchTooltip] with a given state, so a preview or a test can show the label. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
internal fun MonarchTooltipBox(
    text: String,
    state: TooltipState,
    modifier: Modifier = Modifier,
    side: MonarchTooltipSide = MonarchTooltipSide.Above,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val position = when (side) {
        MonarchTooltipSide.Above -> TooltipAnchorPosition.Above
        MonarchTooltipSide.Below -> TooltipAnchorPosition.Below
    }
    val colors = MonarchTheme.colors
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            position,
            MonarchTooltipTokens.Gap,
        ),
        tooltip = {
            PlainTooltip(
                maxWidth = MonarchTooltipTokens.MaxWidth,
                shape = MonarchTheme.shapes.sm,
                contentColor = colors.textInverse,
                containerColor = colors.bgInverse,
            ) {
                Text(text = text, style = MonarchTheme.typography.caption, color = colors.textInverse)
            }
        },
        state = state,
        modifier = modifier,
        enableUserInput = enabled,
        content = content,
    )
}
