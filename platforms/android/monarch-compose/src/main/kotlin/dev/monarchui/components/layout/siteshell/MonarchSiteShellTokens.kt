package dev.monarchui.components.layout.siteshell

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchSiteShell and MonarchSiteFooter, built from primitives and roles only. */
internal object MonarchSiteShellTokens {
    val DrawerPadding: Dp = MonarchTokens.Space.space3.dp
    val DrawerTitlePadding: Dp = MonarchTokens.Space.space4.dp
    val FooterPadding: Dp = MonarchTokens.Space.space6.dp
    val FooterGap: Dp = MonarchTokens.Space.space6.dp
    val FooterLinkGap: Dp = MonarchTokens.Space.space1.dp
    val LegalGap: Dp = MonarchTokens.Space.space6.dp
    val ActionGap: Dp = MonarchTokens.Space.space1.dp
    val BarHeight: Dp = MonarchTokens.Space.space16.dp
    val BarPadding: Dp = MonarchTokens.Space.space1.dp

    /** The top app bar and the drawer sit on the surface; the bar is set apart by a subtle line. */
    fun barFill(colors: MonarchColors): Color = colors.bgSurface
    fun footerLink(colors: MonarchColors): Color = colors.textMuted

    /** The brand in the bar: the semibold weight at the size of control labels, so it fits beside two actions. */
    fun brandStyle(typography: MonarchTypography): TextStyle =
        typography.bodyStrong.copy(fontSize = typography.label.fontSize, lineHeight = typography.label.lineHeight)
}
