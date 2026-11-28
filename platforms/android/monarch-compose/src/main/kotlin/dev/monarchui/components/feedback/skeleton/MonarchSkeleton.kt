package dev.monarchui.components.feedback.skeleton

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import dev.monarchui.theme.MonarchTheme

/**
 * A loading placeholder in the shape of the content that will replace it.
 *
 * Built by Monarch from a `Box` drawn with a moving gradient brush: Material 3
 * has no placeholder. The pulse stands still when the user has turned
 * animations off.
 *
 * A skeleton is hidden from TalkBack. Describe the loading region instead, for
 * example with a content description "Loading contact" on its container, and
 * say when it has loaded.
 *
 * @param modifier the modifier of the placeholder. Set the size here: a width for text lines and blocks, and
 * `Modifier.size` for circles.
 * @param shape a text line, a block or a circle.
 */
@Composable
public fun MonarchSkeleton(
    modifier: Modifier = Modifier,
    shape: MonarchSkeletonShape = MonarchSkeletonShape.Rect,
) {
    val outline = MonarchSkeletonTokens.shape(shape, MonarchTheme.shapes)
    val (base, highlight) = MonarchSkeletonTokens.colors(MonarchTheme.colors)
    val textHeight = with(LocalDensity.current) {
        MonarchTheme.typography.body.fontSize.toDp() * MonarchSkeletonTokens.TEXT_HEIGHT_FRACTION
    }
    val sized = when (shape) {
        MonarchSkeletonShape.Text -> modifier.fillMaxWidth().height(textHeight)
        MonarchSkeletonShape.Rect ->
            modifier.fillMaxWidth().defaultMinSize(minHeight = MonarchSkeletonTokens.DefaultSize)
        MonarchSkeletonShape.Circle -> modifier.size(MonarchSkeletonTokens.DefaultSize)
    }.clearAndSetSemantics { }
    if (MonarchTheme.motion.reducedMotion) {
        Box(sized.background(base, outline))
        return
    }
    // A quiet pulse between the resting fill and the lighter one, like the web's opacity pulse. Not a sweeping
    // band: a band makes every placeholder look cut off at one edge in a still frame.
    val transition = rememberInfiniteTransition(label = "MonarchSkeleton")
    val pulse = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(MonarchSkeletonTokens.PULSE_MILLIS, easing = MonarchTheme.motion.standard),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    Box(
        sized.drawBehind {
            drawOutline(outline.createOutline(size, layoutDirection, this), lerp(base, highlight, pulse.value))
        },
    )
}
