package dev.monarchui.components.feedback.spinner

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.foundation.MonarchControlMetrics
import dev.monarchui.theme.MonarchTheme

/**
 * A ring with one open quarter that turns, for a short wait in a small space.
 *
 * It is the Monarch spinner of the web: a 2dp ring in the current content colour, 14, 16 or 24dp across,
 * one turn in 0.7 seconds. When the user removes animations in the system settings the ring stands still.
 * TalkBack reads it as an indeterminate progress bar named [label].
 *
 * @param modifier the modifier of the spinner.
 * @param size the diameter.
 * @param color the ring. The default is the content colour of the surrounding surface, so a spinner in a
 * button takes the button's text colour.
 * @param label what is loading, for TalkBack. `null` uses the built-in string `monarch_progress_loading`.
 * Pass `""` to hide the spinner from TalkBack when a parent already announces the busy state.
 */
@Composable
public fun MonarchSpinner(
    modifier: Modifier = Modifier,
    size: MonarchSpinnerSize = MonarchSpinnerSize.Md,
    color: Color = LocalContentColor.current,
    label: String? = null,
) {
    val description = label ?: stringResource(R.string.monarch_progress_loading)
    val semantics = if (description.isEmpty()) {
        Modifier.clearAndSetSemantics { }
    } else {
        Modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            contentDescription = description
        }
    }
    SpinnerRing(modifier.size(MonarchSpinnerTokens.diameter(size)).then(semantics), color)
}

@Composable
private fun SpinnerRing(modifier: Modifier, color: Color) {
    val turn = if (MonarchTheme.motion.reducedMotion) {
        0f
    } else {
        rememberInfiniteTransition(label = "monarch-spinner").animateFloat(
            initialValue = 0f,
            targetValue = FullTurn,
            animationSpec = infiniteRepeatable(
                tween(MonarchSpinnerTokens.TurnMillis, easing = LinearEasing),
                RepeatMode.Restart,
            ),
            label = "turn",
        ).value
    }
    Canvas(modifier) {
        val stroke = MonarchControlMetrics.SpinnerStroke.toPx()
        val inset = stroke / 2
        rotate(turn) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = ArcSweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Butt),
            )
        }
    }
}

private const val FullTurn = 360f
private const val ArcSweep = 270f
