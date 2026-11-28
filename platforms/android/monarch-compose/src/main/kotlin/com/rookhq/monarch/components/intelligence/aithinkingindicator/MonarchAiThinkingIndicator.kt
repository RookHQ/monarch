package com.rookhq.monarch.components.intelligence.aithinkingindicator

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.R
import com.rookhq.monarch.components.intelligence.aitypingdots.MonarchAiTypingDots
import com.rookhq.monarch.components.intelligence.aitypingdots.MonarchAiTypingDotsSize
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay

/**
 * Shows that the assistant is working before its first token arrives: a
 * "Thinking…" label with a soft shimmer, or a pulsing mark, and, after a few
 * seconds, how long it has been thinking.
 *
 * Built by Monarch from a `FlowRow` of text: Material 3 has no equivalent. The
 * label, with the elapsed time as its state, is one TalkBack element. With
 * [announce] the label is announced politely once when the indicator appears
 * and again when it changes, through a live region that never holds the
 * elapsed time, so the ticking clock is not announced. The shimmer and the
 * pulse stop when the user has turned animations off; the clock still ticks.
 *
 * @param modifier the modifier of the indicator.
 * @param variant a shimmer through the label, or a pulsing mark before it.
 * @param label replaces the built-in "Thinking…" (`monarch_aithinkingindicator_label`), for example
 * "Searching your files…".
 * @param startedAt when the assistant started working, in epoch milliseconds (UTC). `null` counts from when the
 * indicator first appeared; pass it when the indicator can leave the composition during one turn, for example
 * in a lazy list.
 * @param showElapsedAfterMillis how long before the elapsed time appears after the label. [Long.MAX_VALUE]
 * never shows it.
 * @param announce announces [label] when the indicator appears and when the label changes. Pass `false` when
 * something else already announces that the assistant is working.
 * @param icon the mark of the [MonarchAiThinkingIndicatorVariant.Mark] variant, for example a `MonarchIcon` of
 * the assistant with a `null` content description. It takes the strong assistant colour as its content colour
 * and pulses. `null` uses `MonarchAiTypingDots`. Monarch bundles no icons. Ignored by the shimmer variant.
 * @param elapsedLabel formats whole elapsed seconds. `null` uses the narrow unit format of the current locale:
 * "5s", "1m 5s".
 */
@Composable
public fun MonarchAiThinkingIndicator(
    modifier: Modifier = Modifier,
    variant: MonarchAiThinkingIndicatorVariant = MonarchAiThinkingIndicatorVariant.Shimmer,
    label: String? = null,
    startedAt: Long? = null,
    showElapsedAfterMillis: Long = 3_000L,
    announce: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    elapsedLabel: ((seconds: Int) -> String)? = null,
) {
    ThinkingIndicator(
        modifier = modifier,
        variant = variant,
        label = label,
        startedAt = startedAt,
        showElapsedAfterMillis = showElapsedAfterMillis,
        announce = announce,
        icon = icon,
        elapsedLabel = elapsedLabel,
        clock = SystemClock,
    )
}

/** The indicator with its clock injected, so tests drive the elapsed time without waiting. */
@Composable
internal fun ThinkingIndicator(
    modifier: Modifier,
    variant: MonarchAiThinkingIndicatorVariant,
    label: String?,
    startedAt: Long?,
    showElapsedAfterMillis: Long,
    announce: Boolean,
    icon: (@Composable () -> Unit)?,
    elapsedLabel: ((seconds: Int) -> String)?,
    clock: () -> Long,
) {
    val tokens = MonarchAiThinkingIndicatorTokens
    val text = monarchString(label, R.string.monarch_aithinkingindicator_label)
    val currentClock by rememberUpdatedState(clock)
    val mountedAt = remember { clock() }
    val start = startedAt ?: mountedAt
    var now by remember { mutableLongStateOf(clock()) }
    LaunchedEffect(start) {
        now = currentClock()
        while (true) {
            delay(tokens.untilNextSecond(start, now))
            now = currentClock()
        }
    }
    val seconds = tokens.elapsedSeconds(start, now)
    val locale = LocalConfiguration.current.locales[0]
    val elapsed = if (tokens.showsElapsed(seconds, showElapsedAfterMillis)) {
        elapsedLabel?.invoke(seconds) ?: tokens.elapsedText(seconds, locale)
    } else {
        null
    }
    // A live region that already holds its text when it is inserted is not reliably announced,
    // so the text is set after the first frame, as on the web.
    var filled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { filled = true }
    val colors = MonarchTheme.colors
    Box(modifier = modifier) {
        FlowRow(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = text
                if (elapsed != null) stateDescription = elapsed
            },
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            if (variant == MonarchAiThinkingIndicatorVariant.Mark) Mark(icon)
            Text(
                text = text,
                modifier = if (variant == MonarchAiThinkingIndicatorVariant.Shimmer) Modifier.shimmer() else Modifier,
                style = MonarchTheme.typography.body,
                color = tokens.labelColor(colors),
            )
            if (elapsed != null) {
                Text(text = elapsed, style = MonarchTheme.typography.caption, color = tokens.elapsedColor(colors))
            }
        }
        // Always present, holding the label only, so the label is announced and the clock is not.
        Box(
            modifier = Modifier.semantics {
                if (announce) {
                    liveRegion = LiveRegionMode.Polite
                    if (filled) contentDescription = text
                }
            },
        )
    }
}

/** The mark: the caller's icon pulsing, or the assistant typing dots. Decorative. */
@Composable
private fun Mark(icon: (@Composable () -> Unit)?) {
    val tokens = MonarchAiThinkingIndicatorTokens
    if (icon == null) {
        MonarchAiTypingDots(
            size = MonarchAiTypingDotsSize.Sm,
            announce = false,
            modifier = Modifier.clearAndSetSemantics { },
        )
        return
    }
    val pulse = if (MonarchTheme.motion.reducedMotion) {
        null
    } else {
        rememberInfiniteTransition(label = "thinking mark").animateFloat(
            initialValue = 1f,
            targetValue = tokens.PULSE_LOW_SCALE,
            animationSpec = infiniteRepeatable(
                animation = tween(tokens.CYCLE_MILLIS / 2, easing = MonarchTheme.motion.standard),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse",
        )
    }
    Box(
        modifier = Modifier
            .clearAndSetSemantics { }
            .graphicsLayer {
                val scale = pulse?.value ?: 1f
                scaleX = scale
                scaleY = scale
            },
    ) {
        CompositionLocalProvider(LocalContentColor provides tokens.markColor(MonarchTheme.colors)) { icon() }
    }
}

/**
 * Sweeps a band of the strong assistant colour through the text drawn under it. The band is drawn only over
 * the text's own pixels, so contrast holds in every theme. Still when animations are off.
 */
@Composable
private fun Modifier.shimmer(): Modifier {
    if (MonarchTheme.motion.reducedMotion) return this
    val band = MonarchAiThinkingIndicatorTokens.shimmerColor(MonarchTheme.colors)
    val progress = rememberInfiniteTransition(label = "thinking shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(MonarchAiThinkingIndicatorTokens.CYCLE_MILLIS, easing = LinearEasing),
        ),
        label = "band",
    )
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val start = MonarchAiThinkingIndicatorTokens.bandStart(
                progress = progress.value,
                width = size.width,
                rightToLeft = layoutDirection == LayoutDirection.Rtl,
            )
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, band, Color.Transparent),
                    start = Offset(start, 0f),
                    end = Offset(start + size.width, 0f),
                ),
                blendMode = BlendMode.SrcAtop,
            )
        }
}

private val SystemClock: () -> Long = { System.currentTimeMillis() }
