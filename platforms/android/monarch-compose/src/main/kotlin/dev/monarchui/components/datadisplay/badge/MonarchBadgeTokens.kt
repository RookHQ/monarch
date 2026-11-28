package dev.monarchui.components.datadisplay.badge

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.internal.MonarchToneEmphasis
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchBadge, built from primitives and roles only. */
internal object MonarchBadgeTokens {
    val MinHeight: Dp = MonarchTokens.Space.space6.dp
    val HorizontalPadding: Dp = MonarchTokens.Space.space2.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space0_5.dp
    val ContentGap: Dp = MonarchTokens.Space.space1.dp
    val CompactHeight: Dp = MonarchTokens.Space.space5.dp
    val CompactPadding: Dp = MonarchTokens.Space.space1_5.dp
    val DotSize: Dp = MonarchTokens.Space.space1_5.dp
    private const val COMPACT_TEXT_SP = 11

    fun minHeight(size: MonarchBadgeSize): Dp = if (size == MonarchBadgeSize.Sm) CompactHeight else MinHeight

    fun horizontalPadding(size: MonarchBadgeSize): Dp =
        if (size == MonarchBadgeSize.Sm) CompactPadding else HorizontalPadding

    /** The compact badge sets its text a step smaller, as the web's 2xs. */
    fun textStyle(size: MonarchBadgeSize, base: TextStyle): TextStyle =
        if (size == MonarchBadgeSize.Sm) base.copy(fontSize = COMPACT_TEXT_SP.sp) else base

    const val FONT_WEIGHT: Int = MonarchTokens.FontWeight.medium

    /** The colours of a badge: the shared tone palette at the emphasis of the variant. */
    fun colors(tone: MonarchTone, variant: MonarchBadgeVariant, colors: MonarchColors): MonarchSurfaceColors =
        MonarchTonePalette.of(tone, colors).surface(emphasis(variant))

    private fun emphasis(variant: MonarchBadgeVariant): MonarchToneEmphasis = when (variant) {
        MonarchBadgeVariant.Subtle -> MonarchToneEmphasis.Subtle
        MonarchBadgeVariant.Solid -> MonarchToneEmphasis.Solid
        MonarchBadgeVariant.Outline -> MonarchToneEmphasis.Outline
    }
}
