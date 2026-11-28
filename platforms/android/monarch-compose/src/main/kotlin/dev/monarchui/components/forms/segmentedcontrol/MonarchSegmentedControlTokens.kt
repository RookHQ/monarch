package dev.monarchui.components.forms.segmentedcontrol

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchSegmentedControl, from `segmented-control.css`. */
internal object MonarchSegmentedControlTokens {
    /** The padding between the track and the segments, and the gap between segments: 2dp. */
    val Pad: Dp = MonarchTokens.Space.space0_5.dp

    /** The gap between a segment's icon, text, count and suffix: 6dp. */
    val ContentGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The icon size: 16dp. */
    val IconSize: Dp = MonarchTokens.Space.space4.dp

    /** The visible height of the track. */
    fun trackHeight(size: MonarchSegmentedControlSize): Dp = when (size) {
        MonarchSegmentedControlSize.Sm -> MonarchTokens.Control.sm.dp
        MonarchSegmentedControlSize.Md -> MonarchTokens.Control.md.dp
        MonarchSegmentedControlSize.Lg -> MonarchTokens.Control.lg.dp
    }

    /** The visible height of a segment's pill: the track less the padding on both sides. */
    fun pillHeight(size: MonarchSegmentedControlSize): Dp = trackHeight(size) - Pad * 2

    /** The horizontal padding inside a segment: 10, 12 and 16dp. */
    fun horizontalPadding(size: MonarchSegmentedControlSize): Dp = when (size) {
        MonarchSegmentedControlSize.Sm -> MonarchTokens.Space.space2_5.dp
        MonarchSegmentedControlSize.Md -> MonarchTokens.Space.space3.dp
        MonarchSegmentedControlSize.Lg -> MonarchTokens.Space.space4.dp
    }

    /** Segment text: 14sp medium, 12sp on the small track. */
    fun textStyle(size: MonarchSegmentedControlSize, typography: MonarchTypography): TextStyle =
        if (size == MonarchSegmentedControlSize.Sm) {
            typography.label.copy(fontSize = typography.caption.fontSize)
        } else {
            typography.label
        }

    /** The track: the muted fill. */
    fun track(colors: MonarchColors): Color = colors.bgMuted

    /**
     * The indicator under the chosen segment. Light: the surface with a default hairline (it replaces the web's
     * shadow-xs: no shadows on controls). Dark: one surface step up from the track, no border (dark themes use
     * surface steps, not shadows).
     */
    fun indicator(colors: MonarchColors, dark: Boolean): Color = if (dark) colors.bgEmphasis else colors.bgSurface

    /** The indicator's hairline: the default border in light themes, none in dark ones. */
    fun indicatorBorder(colors: MonarchColors, dark: Boolean): Color =
        if (dark) Color.Transparent else colors.borderDefault

    /** The text and icon: default when chosen, muted otherwise, disabled when the segment cannot be used. */
    fun content(selected: Boolean, enabled: Boolean, pressed: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        selected || pressed -> colors.textDefault
        else -> colors.textMuted
    }
}
