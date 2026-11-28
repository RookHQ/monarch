package dev.monarchui.components.datadisplay.statusdot

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.theme.MonarchTheme

/**
 * A small coloured dot followed by a text label, for example "● Running".
 *
 * Built by Monarch from a Compose `Canvas` and a `Text`: Material 3 has no
 * equivalent. The label is required, so colour is never the only way the
 * status is conveyed. The dot and the label are one TalkBack element, read as
 * the label; the dot is decoration. The dot grows with the font size setting.
 *
 * @param label the words for the status, for example "Running" or "Paused".
 * @param modifier the modifier of the status dot.
 * @param tone the status the dot conveys.
 * @param size the size of the dot and the text.
 * @param pulsing draws a ring that grows from the dot and fades, for something that is live or in
 * progress. Not drawn when the user has turned animations off.
 * @param labelHidden draws only the dot and keeps [label] for TalkBack. Pair it with a visible legend,
 * because sighted users then see only colour.
 */
@Composable
public fun MonarchStatusDot(
    label: String,
    modifier: Modifier = Modifier,
    tone: MonarchTone = MonarchTone.Neutral,
    size: MonarchStatusDotSize = MonarchStatusDotSize.Sm,
    pulsing: Boolean = false,
    labelHidden: Boolean = false,
) {
    val colors = MonarchTheme.colors
    val dotSize = MonarchStatusDotTokens.dotSize(size) * LocalDensity.current.fontScale
    val dotColor = MonarchStatusDotTokens.dotColor(tone, colors)
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            if (labelHidden) contentDescription = label
        },
        horizontalArrangement = Arrangement.spacedBy(MonarchStatusDotTokens.gap(size)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (MonarchStatusDotTokens.showsPulse(pulsing, MonarchTheme.motion.reducedMotion)) {
            PulsingDot(color = dotColor, size = dotSize)
        } else {
            Canvas(modifier = Modifier.size(dotSize)) { drawCircle(dotColor) }
        }
        if (!labelHidden) {
            Text(
                text = label,
                modifier = Modifier.weight(1f, fill = false),
                style = MonarchStatusDotTokens.textStyle(size, MonarchTheme.typography),
                color = colors.textDefault,
            )
        }
    }
}

/** The dot with a ring that grows from it and fades, read in the draw phase only. */
@Composable
private fun PulsingDot(color: Color, size: Dp) {
    val transition = rememberInfiniteTransition(label = "status dot pulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = pulseSpec(MonarchTheme.motion.standard),
        label = "pulse",
    )
    Canvas(modifier = Modifier.size(size)) {
        val radius = this.size.minDimension / 2
        // A transient fade of the animation, not a dimmed colour role.
        drawCircle(
            color = color,
            radius = radius * (1f + (MonarchStatusDotTokens.PULSE_SCALE - 1f) * progress),
            alpha = MonarchStatusDotTokens.PULSE_START_ALPHA * (1f - progress),
        )
        drawCircle(color = color, radius = radius)
    }
}

private fun pulseSpec(easing: Easing): InfiniteRepeatableSpec<Float> =
    infiniteRepeatable(tween(durationMillis = MonarchStatusDotTokens.PULSE_DURATION_MILLIS, easing = easing))
