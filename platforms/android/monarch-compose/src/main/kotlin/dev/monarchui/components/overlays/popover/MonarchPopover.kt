package dev.monarchui.components.overlays.popover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.monarchui.foundation.monarchShadow
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/**
 * A floating panel anchored to a control, for a small amount of interactive
 * content that belongs to it: a filter panel, a quick edit, a short
 * explanation with a link.
 *
 * Material 3 has no popover, so Monarch builds one on the Compose `Popup`.
 * Place it next to its anchor inside a common parent, as with the Material 3
 * `DropdownMenu`: the parent's bounds are the anchor.
 *
 * ```
 * Box {
 *     MonarchButton(text = "Filter", onClick = { open = true })
 *     MonarchPopover(expanded = open, onDismissRequest = { open = false }, label = "Filter invoices") { … }
 * }
 * ```
 *
 * The panel opens on the preferred [side], aligned with the start edge of
 * the anchor, and moves to the other side or inward when the window has no
 * room. A tap outside, the back gesture and the Escape key call
 * [onDismissRequest]. The popup takes focus, so TalkBack and the keyboard move
 * into it and return to the anchor when it closes.
 *
 * @param expanded whether the panel is shown.
 * @param onDismissRequest called when the user taps outside, goes back or presses Escape. Set [expanded] to
 * `false` in it.
 * @param label the spoken name of the panel, for example "Filter invoices". Announced when it opens. Also start
 * the content with a visible heading.
 * @param modifier the modifier of the panel.
 * @param side the side of the anchor the panel prefers.
 * @param anchorX a point on the anchor to open over instead of its start edge: the panel is centred on it, on the
 * [side], and kept inside the window. Measured from the left edge of the anchor, so mirror it yourself in
 * right-to-left layouts. A chart or a bar opens the details of the segment the person tapped this way.
 * @param content the content, laid out in a column with the Monarch gap. Text takes the default text colour.
 */
@Composable
public fun MonarchPopover(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    side: MonarchPopoverSide = MonarchPopoverSide.Bottom,
    anchorX: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // A popover that is open from the first frame (an inspected period, a restored screen) shows at once; only
    // one opened later grows in.
    val visibility = remember { MutableTransitionState(expanded) }
    visibility.targetState = expanded
    if (!visibility.currentState && !visibility.targetState) return

    val density = LocalDensity.current
    val provider = with(density) {
        MonarchPopoverPositionProvider(
            side = side,
            gap = MonarchPopoverTokens.Gap.roundToPx(),
            margin = MonarchPopoverTokens.WindowMargin.roundToPx(),
            inset = MonarchPopoverTokens.ShadowRoom.roundToPx(),
            anchorX = anchorX?.roundToPx(),
        )
    }
    val windowWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val maxWidth = minOf(MonarchPopoverTokens.MaxWidth, windowWidth - MonarchPopoverTokens.WindowMargin * 2)
    val motion = MonarchTheme.motion
    val colors = MonarchPopoverTokens.colors(MonarchTheme.colors, MonarchTheme.isHighContrast)
    val shape = MonarchTheme.shapes.lg
    // A dark theme separates the panel by its lighter surface and border, not a shadow.
    val dark = MonarchTheme.isDark
    // The popup is a window of its own, which reads the configuration of the activity again: hand it the one this
    // popover is composed with, so a locale chosen for the content (a date, a clock) reaches it.
    val configuration = LocalConfiguration.current

    Popup(
        popupPositionProvider = provider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        AnimatedVisibility(
            visibleState = visibility,
            enter = fadeIn(motion.spec(motion.fast, motion.enter)) +
                scaleIn(motion.spec(motion.fast, motion.enter), initialScale = MonarchPopoverTokens.ENTER_SCALE),
            exit = fadeOut(motion.spec(motion.fast, motion.exit)),
        ) {
            // The popup window clips what it draws: leave room around the panel for its shadow.
            Box(Modifier.padding(MonarchPopoverTokens.ShadowRoom)) {
                Column(
                    modifier = modifier
                        .widthIn(max = maxWidth.coerceAtLeast(MonarchPopoverTokens.WindowMargin))
                        .then(if (dark) Modifier else Modifier.monarchShadow(MonarchTheme.elevation.lg, shape))
                        .monarchSurface(colors, shape)
                        .semantics { paneTitle = label }
                        .padding(MonarchPopoverTokens.ContentPadding),
                    verticalArrangement = Arrangement.spacedBy(MonarchPopoverTokens.ContentGap),
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.content,
                        LocalConfiguration provides configuration,
                    ) { content() }
                }
            }
        }
    }
}
