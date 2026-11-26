package com.rookhq.monarch.internal

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.tokens.MonarchTokens

/** Tokens of [LabelledControlRow], built from primitives only. */
internal object LabelledControlRowTokens {
    val MinHeight: Dp = MonarchTouchTarget.Minimum
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space2.dp
    val DescriptionGap: Dp = MonarchTokens.Space.space0_5.dp
}
