package com.rookhq.monarch.components.charts.deltaindicator

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.MonarchToneEmphasis
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchDeltaIndicator, built from primitives and roles only. */
internal object MonarchDeltaIndicatorTokens {
    val Gap: Dp = MonarchTokens.Space.space1.dp
    val PillHorizontalPadding: Dp = MonarchTokens.Space.space1_5.dp
    val PillVerticalPadding: Dp = MonarchTokens.Space.space0_5.dp

    /** Tabular figures, so changes line up in a column. */
    const val TABULAR_FIGURES = "tnum"

    fun textStyle(size: MonarchDeltaIndicatorSize, typography: MonarchTypography): TextStyle = when (size) {
        MonarchDeltaIndicatorSize.Sm -> typography.caption
        MonarchDeltaIndicatorSize.Md -> typography.label
    }

    /** The colour of the change in the plain variant: the strong colour of the tone, muted for neutral. */
    fun plainColor(tone: MonarchTone, colors: MonarchColors): Color = MonarchTonePalette.of(tone, colors).strong

    /** The pill of the subtle variant: the tinted background and strong text of the tone. */
    fun pillColors(tone: MonarchTone, colors: MonarchColors): MonarchSurfaceColors =
        MonarchTonePalette.of(tone, colors).surface(MonarchToneEmphasis.Subtle)

    /** The comparison and the no-data text. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted
}
