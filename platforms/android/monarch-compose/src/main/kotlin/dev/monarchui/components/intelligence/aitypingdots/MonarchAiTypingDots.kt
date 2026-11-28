package dev.monarchui.components.intelligence.aitypingdots

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import dev.monarchui.R
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Three dots that rise and brighten in turn to show the assistant is writing,
 * for places with no room for a sentence: a chat dock, a list row, an empty
 * message bubble.
 *
 * Built by Monarch from a `Row` of drawn dots: Material 3 has no equivalent.
 * The dots are one TalkBack element read as [label]. With [announce] the
 * element is a polite live region whose text is filled one frame after it
 * appears, so TalkBack announces it without moving focus. The dots stand still
 * when the user has turned animations off.
 *
 * @param modifier the modifier of the dots.
 * @param size the size of the dots.
 * @param announce announces [label] when the dots appear. Pass `false` when something else already
 * announces that the assistant is writing.
 * @param label the text TalkBack reads. `null` uses the built-in string `monarch_aitypingdots_label`,
 * "Assistant is typing".
 */
@Composable
public fun MonarchAiTypingDots(
    modifier: Modifier = Modifier,
    size: MonarchAiTypingDotsSize = MonarchAiTypingDotsSize.Md,
    announce: Boolean = true,
    label: String? = null,
) {
    val text = monarchString(label, R.string.monarch_aitypingdots_label)
    // A live region that already holds its text when it is inserted is not reliably announced,
    // so the text is set after the first frame, as on the web.
    var shown by remember { mutableStateOf(!announce) }
    LaunchedEffect(Unit) { shown = true }
    val density = LocalDensity.current
    val dot = MonarchAiTypingDotsTokens.dotSize(size) * density.fontScale
    val step = dot * MonarchAiTypingDotsTokens.GAP_FRACTION
    Row(
        modifier = modifier
            .clearAndSetSemantics {
                if (shown) contentDescription = text
                if (announce) liveRegion = LiveRegionMode.Polite
            }
            .padding(vertical = dot * MonarchAiTypingDotsTokens.PADDING_FRACTION),
        horizontalArrangement = Arrangement.spacedBy(step),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val colors = MonarchTheme.colors
        val active = MonarchAiTypingDotsTokens.activeColor(colors)
        if (MonarchTheme.motion.reducedMotion) {
            repeat(MonarchAiTypingDotsTokens.DOT_COUNT) {
                Box(modifier = Modifier.size(dot).drawBehind { drawCircle(active) })
            }
        } else {
            val resting = MonarchAiTypingDotsTokens.restingColor(colors)
            val rise = with(density) { step.toPx() }
            val transition = rememberInfiniteTransition(label = "ai typing dots")
            repeat(MonarchAiTypingDotsTokens.DOT_COUNT) { index ->
                val height = transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 0f,
                    animationSpec = riseSpec(index),
                    label = "dot $index",
                )
                Box(
                    modifier = Modifier
                        .size(dot)
                        .graphicsLayer { translationY = -rise * height.value }
                        .drawBehind { drawCircle(lerp(resting, active, height.value)) },
                )
            }
        }
    }
}

private fun riseSpec(index: Int): InfiniteRepeatableSpec<Float> {
    val cycle = MonarchAiTypingDotsTokens.CYCLE_MILLIS
    val peak = (cycle * MonarchAiTypingDotsTokens.PEAK_FRACTION).toInt()
    return infiniteRepeatable(
        animation = keyframes {
            durationMillis = cycle
            0f at 0
            1f at peak
            0f at peak * 2
            0f at cycle
        },
        initialStartOffset = StartOffset(MonarchAiTypingDotsTokens.DOT_DELAY_MILLIS * index),
    )
}
