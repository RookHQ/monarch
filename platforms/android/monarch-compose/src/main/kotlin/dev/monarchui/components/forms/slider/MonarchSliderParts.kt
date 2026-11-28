package dev.monarchui.components.forms.slider

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.foundation.MonarchControlMetrics
import dev.monarchui.foundation.layerColors
import dev.monarchui.internal.choice.drawChoiceFocusRing
import dev.monarchui.internal.choice.drawThumbHairline
import dev.monarchui.theme.MonarchTheme

/** The optional label row above a slider: the label at the start, the value at the end. */
@Composable
internal fun SliderGroup(
    label: String?,
    valueText: String?,
    enabled: Boolean,
    modifier: Modifier,
    slider: @Composable (Modifier) -> Unit,
) {
    if (label == null && valueText == null) {
        slider(modifier)
        return
    }
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MonarchSliderTokens.LabelGap)) {
        Row(
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(MonarchControlMetrics.FieldGap),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = label.orEmpty(),
                modifier = Modifier.weight(1f),
                style = typography.label,
                color = if (enabled) colors.textDefault else colors.textDisabled,
            )
            if (valueText != null) {
                Text(
                    text = valueText,
                    style = typography.label.copy(fontWeight = FontWeight.Normal),
                    color = if (enabled) colors.textMuted else colors.textDisabled,
                )
            }
        }
        slider(Modifier.fillMaxWidth())
    }
}

/**
 * The 18dp thumb: a soft surface circle with a 1dp ring in the range colour and, in light themes, a hairline shadow
 * (the only shadow on a control). The thumb slot is 48dp high, so the slider is 48dp high and the whole height takes
 * the drag. The focus ring sits outside it with the usual gap.
 */
@Composable
internal fun SliderThumb(colors: MonarchSliderColors, source: MutableInteractionSource) {
    val focused by source.collectIsFocusedAsState()
    val ring = MonarchTheme.colors.focusRing
    val ringWidth = MonarchTheme.focusRingWidth
    val dark = MonarchTheme.isDark
    val shade = MonarchTheme.elevation.sm.layerColors
    Box(
        modifier = Modifier.size(MonarchControlMetrics.SliderThumb, MonarchTouchTarget.Minimum),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(MonarchControlMetrics.SliderThumb)) {
            val radius = size.width / 2
            val border = MonarchSliderTokens.ThumbBorder.toPx()
            drawThumbHairline(center, radius, shade, dark)
            drawCircle(colors.thumb, radius)
            drawCircle(colors.thumbBorder, radius - border / 2, style = Stroke(border))
            if (focused) drawChoiceFocusRing(ring, ringWidth.toPx(), radius)
        }
    }
}

/** The 6dp track with the range drawn from [from] to [to] (fractions of the width, along the reading direction). */
@Composable
internal fun SliderTrack(colors: MonarchSliderColors, from: Float, to: Float) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(Modifier.fillMaxWidth().height(MonarchControlMetrics.SliderTrack)) {
        val corner = CornerRadius(size.height / 2)
        drawRoundRect(colors.track, cornerRadius = corner)
        val start = if (rtl) size.width * (1f - to) else size.width * from
        val end = if (rtl) size.width * (1f - from) else size.width * to
        if (end > start) {
            drawRoundRect(
                color = colors.range,
                topLeft = Offset(start, 0f),
                size = Size(end - start, size.height),
                cornerRadius = corner,
            )
        }
    }
}
