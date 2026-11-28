package com.rookhq.monarch.components.identity.recoverycodes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchRecoveryCodes. */
internal object MonarchRecoveryCodesTokens {
    /** The suggested name of the downloaded file, as on the web. */
    const val DEFAULT_FILE_NAME: String = "recovery-codes.txt"

    /** The MIME type of the downloaded file. */
    const val FILE_TYPE: String = "text/plain"

    /** The gap between the parts of the component. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between codes and between buttons. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The padding inside the panel of codes. */
    val PanelPadding: Dp = MonarchTokens.Space.space4.dp

    /** The width of one column of codes before the font scale: 112dp. */
    val CodeWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space4).dp

    /** The colours of the panel: the subtle background with a subtle border. */
    fun panelColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.bgSubtle, colors.textDefault, colors.borderSubtle)

    /** The colour of a code: subtle once used. */
    fun codeColor(used: Boolean, colors: MonarchColors): Color = if (used) colors.textSubtle else colors.textDefault

    /** How many columns of codes fit in [width] at [fontScale]; at least one. */
    fun columns(width: Dp, fontScale: Float): Int =
        maxOf(1, (width.value / (CodeWidth.value * maxOf(1f, fontScale))).toInt())

    /** The text that is copied or saved: the heading and a blank line, then each unused code on its own line. */
    fun text(codes: List<MonarchRecoveryCode>, heading: String?): String {
        val body = codes.filterNot { it.used }.joinToString("\n") { it.code }
        return if (heading.isNullOrBlank()) body else "$heading\n\n$body"
    }

    /** Whether any code can still be used. */
    fun anyUnused(codes: List<MonarchRecoveryCode>): Boolean = codes.any { !it.used }

    /** Whether the show and hide button is shown: by default when the codes start hidden. */
    fun showsRevealToggle(override: Boolean?, initiallyRevealed: Boolean): Boolean = override ?: !initiallyRevealed
}
