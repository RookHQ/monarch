package com.rookhq.monarch.components.layout.logocloud

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchLogoCloud, built from primitives and roles only. */
internal object MonarchLogoCloudTokens {
    val TitleGap: Dp = MonarchTokens.Space.space4.dp
    val RowGapHorizontal: Dp = MonarchTokens.Space.space8.dp
    val RowGapVertical: Dp = MonarchTokens.Space.space4.dp
    val CellGap: Dp = MonarchTokens.Space.space3.dp
    val CellPadding: Dp = MonarchTokens.Space.space4.dp
    val MinCellWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space12).dp

    /** The height of a logo image: 24, 32 or 44dp, as on the web. */
    fun height(size: MonarchLogoCloudSize): Dp =
        when (size) {
            MonarchLogoCloudSize.Sm -> MonarchTokens.Space.space6.dp
            MonarchLogoCloudSize.Md -> MonarchTokens.Space.space8.dp
            MonarchLogoCloudSize.Lg -> (MonarchTokens.Space.space10 + MonarchTokens.Space.space1).dp
        }

    /** The type of a wordmark: larger logos take a larger style. */
    fun wordmarkStyle(size: MonarchLogoCloudSize): MonarchTextStyle =
        when (size) {
            MonarchLogoCloudSize.Sm -> MonarchTextStyle.Label
            MonarchLogoCloudSize.Md -> MonarchTextStyle.BodyStrong
            MonarchLogoCloudSize.Lg -> MonarchTextStyle.Subheading
        }

    /** Wordmarks and the title are quiet: the muted text colour. */
    fun wordmark(colors: MonarchColors): Color = colors.textMuted
    fun cellBorder(colors: MonarchColors): Color = colors.borderDefault
}
