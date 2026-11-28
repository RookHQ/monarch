package com.rookhq.monarch.components.layout.careersjoblist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchCareersJobList, built from primitives and roles only. */
internal object MonarchCareersJobListTokens {
    val Gap: Dp = MonarchTokens.Space.space4.dp
    val CardGap: Dp = MonarchTokens.Space.space3.dp
    val LineGap: Dp = MonarchTokens.Space.space1.dp
    val MetaGap: Dp = MonarchTokens.Space.space3.dp
    val TagGap: Dp = MonarchTokens.Space.space1_5.dp
    val DetailsGap: Dp = MonarchTokens.Space.space5.dp
    val DetailsPadding: Dp = MonarchTokens.Space.space4.dp
    val MinCardWidth: Dp = (MonarchTokens.Space.space24 * 3).dp
    const val SUMMARY_LINES: Int = 2

    /** Metadata, the count and the summary are muted; the chevron is subtle. */
    fun muted(colors: MonarchColors): Color = colors.textMuted
    fun chevron(colors: MonarchColors): Color = colors.textMuted
}
