package dev.monarchui.components.actions.button

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.components.feedback.spinner.MonarchSpinner
import dev.monarchui.components.feedback.spinner.MonarchSpinnerSize
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.LocalMonarchThemeTokens
import dev.monarchui.theme.MonarchTheme

/**
 * The anatomy shared by `MonarchButton`, `MonarchIconButton` and `MonarchButtonGroup`.
 *
 * Own layers, no Material `Button`: the Material button forces a 40dp minimum height and a 58dp minimum width,
 * which would make the web's 32 and 36dp buttons impossible. Outside to inside:
 *
 * 1. the tap target: `clickable` with the button role, then the 48dp reservation (`touchTarget`), so a tap in
 *    the reserved space around a 32 or 36dp button presses it;
 * 2. the visible button: the focus ring outside the button, the fill (resting, hover, pressed; a subtle fill
 *    when disabled), the 1dp outline, a ripple, the content. No shadow: buttons are inline controls.
 *
 * @param touchTarget the modifier that reserves the touch target, for example `monarchMinimumTouchTarget()`.
 * @param minHeight the visible height; the button grows with the content, so a large font scale never clips.
 * @param minWidth the visible width, for the square icon-only button; `Dp.Unspecified` for none.
 * @param spinnerOverContent when loading, draws the spinner over the (hidden) content, centred, so the button
 * keeps its size. When `false` the content already carries the spinner in place of an icon.
 * @param semanticsExtra more semantics of the button, for example a content description.
 */
@Composable
@Suppress("LongParameterList")
internal fun MonarchButtonSurface(
    onClick: () -> Unit,
    modifier: Modifier,
    variant: MonarchButtonVariant,
    size: MonarchButtonSize,
    enabled: Boolean,
    loading: Boolean,
    loadingLabel: String?,
    shape: Shape,
    interactionSource: MutableInteractionSource?,
    touchTarget: Modifier,
    minHeight: Dp,
    minWidth: Dp = Dp.Unspecified,
    spinnerOverContent: Boolean = true,
    semanticsExtra: SemanticsPropertyReceiver.() -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val busyLabel = monarchString(loadingLabel, R.string.monarch_button_loading)
    Box(
        modifier = modifier
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = { if (!loading) onClick() },
            )
            .semantics(mergeDescendants = true) {
                if (loading) {
                    stateDescription = busyLabel
                    disabled()
                }
                semanticsExtra()
            }
            .then(touchTarget),
        propagateMinConstraints = true,
    ) {
        ButtonVisual(
            source = source,
            variant = variant,
            size = size,
            enabled = enabled,
            hideContent = loading && spinnerOverContent,
            shape = shape,
            minHeight = minHeight,
            minWidth = minWidth,
            content = content,
        )
    }
}

/** The visible button: focus ring, fill, outline, ripple and the content row. */
@Composable
@Suppress("LongParameterList")
private fun ButtonVisual(
    source: MutableInteractionSource,
    variant: MonarchButtonVariant,
    size: MonarchButtonSize,
    enabled: Boolean,
    hideContent: Boolean,
    shape: Shape,
    minHeight: Dp,
    minWidth: Dp,
    content: @Composable RowScope.() -> Unit,
) {
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()
    val focused by source.collectIsFocusedAsState()
    val look = MonarchButtonTokens.colors(variant, enabled, MonarchTheme.colors)
    val fill = when {
        !enabled -> look.container
        pressed -> look.pressed
        hovered -> look.hover
        else -> look.container
    }
    val ring = if (focused && enabled) {
        Modifier.buttonFocusRing(
            shape,
            MonarchTheme.colors.focusRing,
            LocalMonarchThemeTokens.current.focusRingWidth.dp,
        )
    } else {
        Modifier
    }
    val outline = look.border?.let { Modifier.border(MonarchButtonTokens.BorderWidth, it, shape) } ?: Modifier
    val width = if (minWidth == Dp.Unspecified) Modifier else Modifier.widthIn(min = minWidth)
    Box(
        modifier = width
            .heightIn(min = minHeight)
            .then(ring)
            .background(fill, shape)
            .then(outline)
            .clip(shape)
            .indication(source, ripple(color = look.content))
            .padding(MonarchButtonTokens.contentPadding(size)),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides look.content,
            LocalTextStyle provides MonarchButtonTokens.textStyle(size, MonarchTheme.typography, variant)
                .copy(color = look.content),
        ) {
            Row(
                modifier = Modifier.graphicsLayer { alpha = if (hideContent) 0f else 1f },
                horizontalArrangement = Arrangement.spacedBy(
                    MonarchButtonTokens.ContentGap,
                    Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
            if (hideContent) ButtonSpinner()
        }
    }
}

/** The loading indicator of a button: 16dp, in the content colour, decoration (the button announces the state). */
@Composable
internal fun ButtonSpinner() {
    MonarchSpinner(size = MonarchSpinnerSize.Md, label = "")
}

/** Draws the focus ring outside the visible button, with a gap of one ring width, like the web. */
internal fun Modifier.buttonFocusRing(
    shape: Shape,
    color: Color,
    width: Dp,
): Modifier = drawWithContent {
    drawContent()
    val stroke = width.toPx()
    val outset = stroke * RingOffsetFactor
    val grown = Size(size.width + outset * 2, size.height + outset * 2)
    val outline = shape.createOutline(grown, layoutDirection, this)
    translate(left = -outset, top = -outset) {
        drawOutline(outline = outline, color = color, style = Stroke(width = stroke))
    }
}

/** The ring is centred one and a half widths outside the button: a gap, then the stroke. */
private const val RingOffsetFactor = 1.5f
