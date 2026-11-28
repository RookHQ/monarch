package dev.monarchui.internal.choice

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.foundation.MonarchMotion
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/** The spring of a toggle: a small settle on the thumb, the dot or the check; a snap with reduced motion. */
internal fun <T> MonarchMotion.toggleSpring(): FiniteAnimationSpec<T> =
    if (reducedMotion) snap() else spring(MonarchChoiceTokens.TOGGLE_DAMPING, Spring.StiffnessMedium)

/** The spring of an indicator that moves between items: no overshoot, so the layout never seems to bounce. */
internal fun <T> MonarchMotion.indicatorSpring(): FiniteAnimationSpec<T> =
    if (reducedMotion) {
        snap()
    } else {
        spring(MonarchChoiceTokens.INDICATOR_DAMPING, MonarchChoiceTokens.INDICATOR_STIFFNESS)
    }

/**
 * Wraps a toggle callback so the device gives the platform's toggle haptic ("on" or "off"), as the system switch
 * does. The haptic follows the system setting: when touch feedback is off, nothing is felt.
 */
@Composable
internal fun rememberToggleHaptics(onChange: ((Boolean) -> Unit)?): ((Boolean) -> Unit)? {
    if (onChange == null) return null
    val haptics = LocalHapticFeedback.current
    val current by rememberUpdatedState(onChange)
    return remember(haptics) {
        { value: Boolean ->
            haptics.performHapticFeedback(if (value) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            current(value)
        }
    }
}

/**
 * The hairline shadow of a thumb (switch, slider) centred at [center] with [radius]: half a pixel of shade just
 * below the edge, one disc per layer of the theme's `sm` shadow ([shade]), so a white thumb reads as a separate,
 * raised object without a drop shadow. Light themes only. [strength] (0 to 1) fades it, for a thumb that is only
 * white in one position.
 */
internal fun DrawScope.drawThumbHairline(
    center: Offset,
    radius: Float,
    shade: List<Color>,
    dark: Boolean,
    strength: Float = 1f,
) {
    val amount = strength.coerceIn(0f, 1f)
    if (dark || amount == 0f) return
    val px = MonarchTokens.Space.px.dp.toPx()
    shade.forEach { layer ->
        drawCircle(layer.copy(alpha = layer.alpha * amount), radius + px / 2, center + Offset(0f, px / 2))
    }
}

/** The source of a control's interactions: the caller's, or one of its own. */
@Composable
internal fun rememberChoiceSource(external: MutableInteractionSource?): MutableInteractionSource =
    external ?: remember { MutableInteractionSource() }

/**
 * Draws the focus ring of a small control outside its bounds: the theme's ring colour and width, one and a half
 * stroke widths outside the edge, a gap and then the ring, as the web's `outline-offset: 2px`. [radius] is the
 * corner radius of the control itself; a circle passes half its size.
 */
internal fun DrawScope.drawChoiceFocusRing(color: Color, strokeWidthPx: Float, radius: Float) {
    val outset = strokeWidthPx * MonarchChoiceTokens.RING_OUTSET_IN_STROKES
    drawRoundRectRing(color, strokeWidthPx, outset, radius)
}

private fun DrawScope.drawRoundRectRing(color: Color, strokeWidthPx: Float, outset: Float, radius: Float) {
    drawRoundRect(
        color = color,
        topLeft = Offset(-outset, -outset),
        size = Size(size.width + outset * 2, size.height + outset * 2),
        cornerRadius = CornerRadius(radius + outset),
        style = Stroke(width = strokeWidthPx),
    )
}

/**
 * A control beside a label with an optional description, at least 48dp high. The caller's [modifier] carries the
 * `toggleable` or `selectable` behaviour, so the whole row is one target and one TalkBack element.
 *
 * A control at the start (checkbox, radio) is centred on the first line of the label, as on the web; a control at
 * the end (switch) is centred on the row. The label is 14sp medium and wraps at any font scale.
 *
 * @param controlHeight the height of the control, to centre it on the first line.
 * @param labelContent the label with links inside the sentence, shown instead of [label] when set.
 */
@Composable
internal fun MonarchChoiceRow(
    label: String,
    description: String?,
    enabled: Boolean,
    controlAtEnd: Boolean,
    controlHeight: Dp,
    modifier: Modifier,
    labelContent: AnnotatedString? = null,
    control: @Composable () -> Unit,
) {
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    val density = LocalDensity.current
    val lineHeight = with(density) { firstLineHeight(typography.label) } ?: LineFallback
    val padding = ((MonarchTouchTarget.Minimum - lineHeight) / 2).coerceAtLeast(MonarchChoiceTokens.MinVerticalPadding)
    val controlTop = ((lineHeight - controlHeight) / 2).coerceAtLeast(MonarchTokens.Space.space0.dp)
    Row(
        modifier = modifier
            .heightIn(min = MonarchTouchTarget.Minimum)
            .padding(vertical = padding),
        horizontalArrangement = Arrangement.spacedBy(MonarchChoiceTokens.Gap),
        verticalAlignment = if (controlAtEnd) Alignment.CenterVertically else Alignment.Top,
    ) {
        if (!controlAtEnd) Row(Modifier.padding(top = controlTop)) { control() }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MonarchChoiceTokens.DescriptionGap),
        ) {
            Text(
                text = labelContent ?: AnnotatedString(label),
                style = typography.label,
                color = if (enabled) colors.textDefault else colors.textDisabled,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = typography.caption,
                    color = if (enabled) colors.textMuted else colors.textDisabled,
                )
            }
        }
        if (controlAtEnd) control()
    }
}

/** The height of one line of [style] in dp, whether its line height is in sp or in em, or null when unset. */
private fun Density.firstLineHeight(style: TextStyle): Dp? {
    val line = style.lineHeight
    return when {
        line.isSp -> line.toDp()
        line.isEm && style.fontSize.isSp -> (style.fontSize.value * line.value * fontScale).dp
        else -> null
    }
}

private val LineFallback: Dp = MonarchTokens.Space.space5.dp
