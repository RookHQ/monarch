package com.rookhq.monarch.components.datadisplay.listrow

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchListRow, built from primitives and roles only. */
internal object MonarchListRowTokens {
    /** The smallest height of a row: the touch target. A row that does nothing keeps it for a uniform list. */
    val MinHeight: Dp = MonarchTouchTarget.Minimum

    /** 16dp at the sides, 12dp above and below: a one-line row is 48dp, a two-line row about 62dp. */
    val ContentPadding: PaddingValues =
        PaddingValues(horizontal = MonarchTokens.Space.space4.dp, vertical = MonarchTokens.Space.space3.dp)

    /** The space between the leading element, the text and the trailing element. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The size of the box of a leading icon or avatar. */
    val LeadingSize: Dp = MonarchTokens.Space.space6.dp

    /** The space between the lines of text. */
    val LineGap: Dp = MonarchTokens.Space.space0_5.dp

    val DividerThickness: Dp = MonarchTokens.Space.px.dp

    fun headline(typography: MonarchTypography): TextStyle = typography.label

    fun supporting(typography: MonarchTypography): TextStyle = typography.body.copy(
        fontSize = MonarchTokens.FontSize.sm.sp,
        lineHeight = (MonarchTokens.FontSize.sm * MonarchTokens.LineHeight.normal).sp,
    )

    fun overline(typography: MonarchTypography): TextStyle = typography.caption

    /** The fill of the row: transparent at rest, `bgMuted` pressed or hovered, `accentSubtle` selected. */
    fun container(selected: Boolean, pressedOrHovered: Boolean, colors: MonarchColors): Color = when {
        selected -> colors.accentSubtle
        pressedOrHovered -> colors.bgMuted
        else -> Color.Transparent
    }
}
