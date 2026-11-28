package com.rookhq.monarch.components.collaboration.typingindicator

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.theme.MonarchTheme

/**
 * "Ana is typing…" under a conversation, with three bouncing dots.
 *
 * Built by Monarch from a `Row` of animated dots and a `Text`: Material 3 has
 * no equivalent. It takes the names of the people typing from the app's
 * realtime layer and words them for one, two or several people. The dots are
 * decoration and stand still when the user has turned animations off.
 *
 * The element stays in place while nobody is typing, so the next sentence is
 * announced by TalkBack as a polite live region.
 *
 * @param names who is typing now, as their names should be read, newest last. Leave out the person using
 * the app. An empty list shows nothing.
 * @param modifier the modifier of the typing indicator.
 * @param text replaces the built-in sentence, for example for a language whose wording the built-in
 * strings do not cover. Ignored while [names] is empty.
 * @param reserveSpace keeps the height of one line while nobody is typing, so the conversation above does
 * not move each time someone starts or stops.
 * @param announceChanges announces each new sentence. Pass `false` when the app announces typing itself.
 */
@Composable
public fun MonarchTypingIndicator(
    names: List<String>,
    modifier: Modifier = Modifier,
    text: String? = null,
    reserveSpace: Boolean = true,
    announceChanges: Boolean = true,
) {
    val colors = MonarchTheme.colors
    val one = stringResource(R.string.monarch_typingindicator_one)
    val two = stringResource(R.string.monarch_typingindicator_two)
    val several = stringResource(R.string.monarch_typingindicator_several)
    val sentence = if (names.isEmpty()) {
        ""
    } else {
        text ?: MonarchTypingIndicatorTokens.sentence(
            names = names,
            one = { one.format(it) },
            two = { first, second -> two.format(first, second) },
            several = { several },
        )
    }
    val style = MonarchTheme.typography.caption
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                if (announceChanges) liveRegion = LiveRegionMode.Polite
            }
            .padding(horizontal = MonarchTypingIndicatorTokens.HorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(MonarchTypingIndicatorTokens.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (sentence.isNotEmpty()) {
            TypingDots()
            Text(
                text = sentence,
                modifier = Modifier.weight(1f, fill = false),
                style = style,
                color = MonarchTypingIndicatorTokens.textColor(colors),
            )
        } else if (reserveSpace) {
            // An empty line of the same style holds the height of one line at every font size.
            Text(text = "", modifier = Modifier.clearAndSetSemantics { }, style = style)
        }
    }
}

/**
 * The three decorative dots. Their bounce is read in the draw phase only.
 * When animations are off they stand still in the colour of a raised dot.
 */
@Composable
private fun TypingDots() {
    val colors = MonarchTheme.colors
    val resting = MonarchTypingIndicatorTokens.restingDotColor(colors)
    val active = MonarchTypingIndicatorTokens.activeDotColor(colors)
    val density = LocalDensity.current
    val dotSize = MonarchTypingIndicatorTokens.DotSize * density.fontScale
    val bounce = with(density) { (MonarchTypingIndicatorTokens.BounceHeight * density.fontScale).toPx() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchTypingIndicatorTokens.DotGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (MonarchTheme.motion.reducedMotion) {
            repeat(MonarchTypingIndicatorTokens.DOT_COUNT) {
                Box(modifier = Modifier.size(dotSize).drawBehind { drawCircle(active) })
            }
        } else {
            val transition = rememberInfiniteTransition(label = "typing dots")
            repeat(MonarchTypingIndicatorTokens.DOT_COUNT) { index ->
                val height = transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 0f,
                    animationSpec = bounceSpec(index),
                    label = "dot $index",
                )
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .graphicsLayer { translationY = -bounce * height.value }
                        .drawBehind { drawCircle(lerp(resting, active, height.value)) },
                )
            }
        }
    }
}

private fun bounceSpec(index: Int): InfiniteRepeatableSpec<Float> {
    val cycle = MonarchTypingIndicatorTokens.CYCLE_MILLIS
    val peak = (cycle * MonarchTypingIndicatorTokens.PEAK_FRACTION).toInt()
    return infiniteRepeatable(
        animation = keyframes {
            durationMillis = cycle
            0f at 0
            1f at peak
            0f at peak * 2
            0f at cycle
        },
        initialStartOffset = StartOffset(MonarchTypingIndicatorTokens.DOT_DELAY_MILLIS * index),
    )
}
