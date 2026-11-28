package dev.monarchui.components.collaboration.audiolevelmeter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.R
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import java.text.NumberFormat

/**
 * Shows how loud a microphone is, as rising segments or a thin bar, from a
 * level the app measures.
 *
 * Built by Monarch with a Compose `Canvas`: Material 3 has no equivalent.
 * Capturing audio needs the microphone permission and an `AudioRecord` or a
 * call SDK, which belong to the app, so the meter only takes a [level].
 * Segments that are on are tall and segments that are off are dots, so the
 * level does not depend on colour. When the user has removed animations the
 * meter only shows whether there is sound. It is not interactive.
 *
 * @param level the loudness from 0 to 1. Values outside are limited to that range; a value that is not
 * finite is silence.
 * @param modifier the modifier of the meter.
 * @param variant rising segments or one bar that fills the width.
 * @param size the height of the segments.
 * @param segments the number of segments; at least one.
 * @param muted the microphone is off: the meter is empty, in the disabled colour, and reads [mutedLabel].
 * @param decorative hides the meter from TalkBack, when the same information is given elsewhere, such as a
 * call tile that already says who is speaking.
 * @param label replaces the built-in name "Microphone level" (`monarch_audiolevelmeter_label`).
 * @param mutedLabel replaces the built-in value "Muted" (`monarch_audiolevelmeter_muted`).
 */
@Composable
public fun MonarchAudioLevelMeter(
    level: Float,
    modifier: Modifier = Modifier,
    variant: MonarchAudioLevelMeterVariant = MonarchAudioLevelMeterVariant.Segments,
    size: MonarchAudioLevelMeterSize = MonarchAudioLevelMeterSize.Md,
    segments: Int = 5,
    muted: Boolean = false,
    decorative: Boolean = false,
    label: String? = null,
    mutedLabel: String? = null,
) {
    val value = MonarchAudioLevelMeterTokens.level(level, muted)
    val shown = MonarchAudioLevelMeterTokens.shown(value, MonarchTheme.motion.reducedMotion)
    val colors = MonarchAudioLevelMeterTokens.colors(muted, MonarchTheme.colors)
    val name = monarchString(label, R.string.monarch_audiolevelmeter_label)
    val mutedText = monarchString(mutedLabel, R.string.monarch_audiolevelmeter_muted)
    val locale = LocalConfiguration.current.locales[0]
    val spoken = if (muted) mutedText else NumberFormat.getPercentInstance(locale).format(value.toDouble())
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val count = MonarchAudioLevelMeterTokens.count(segments)
    val semantics = if (decorative) {
        Modifier.clearAndSetSemantics { }
    } else {
        Modifier.clearAndSetSemantics {
            contentDescription = name
            stateDescription = spoken
            progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
        }
    }
    val sized = when (variant) {
        MonarchAudioLevelMeterVariant.Segments -> {
            val width = MonarchAudioLevelMeterTokens.SegmentWidth * count +
                MonarchAudioLevelMeterTokens.SegmentGap * (count - 1)
            modifier.size(width, MonarchAudioLevelMeterTokens.height(size))
        }
        MonarchAudioLevelMeterVariant.Bar -> modifier.fillMaxWidth().height(MonarchAudioLevelMeterTokens.BarHeight)
    }
    Canvas(modifier = sized.then(semantics)) {
        // Segments rise towards the end and the bar fills from the start, in both layout directions.
        scale(scaleX = if (rtl) -1f else 1f, scaleY = 1f) {
            when (variant) {
                MonarchAudioLevelMeterVariant.Segments -> drawSegments(count, shown, colors)
                MonarchAudioLevelMeterVariant.Bar -> drawBar(shown, colors)
            }
        }
    }
}

private fun DrawScope.drawSegments(count: Int, shown: Float, colors: MonarchAudioLevelMeterColors) {
    val width = MonarchAudioLevelMeterTokens.SegmentWidth.toPx()
    val gap = MonarchAudioLevelMeterTokens.SegmentGap.toPx()
    val lit = MonarchAudioLevelMeterTokens.lit(shown, count)
    val corner = CornerRadius(width / 2, width / 2)
    repeat(count) { index ->
        val left = index * (width + gap)
        if (index < lit) {
            val height = size.height * (index + 1) / count
            drawRoundRect(
                color = colors.on,
                topLeft = Offset(left, size.height - height),
                size = Size(width, height),
                cornerRadius = corner,
            )
        } else {
            val center = Offset(left + width / 2, size.height - width / 2)
            drawCircle(color = colors.off, radius = width / 2, center = center)
        }
    }
}

private fun DrawScope.drawBar(shown: Float, colors: MonarchAudioLevelMeterColors) {
    val corner = CornerRadius(size.height / 2, size.height / 2)
    drawRoundRect(color = colors.track, cornerRadius = corner)
    if (shown > 0f) {
        drawRoundRect(color = colors.on, size = Size(size.width * shown, size.height), cornerRadius = corner)
    }
}
