package dev.monarchui.components.collaboration.calltile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and logic of MonarchCallTile, built from primitives and roles only. */
internal object MonarchCallTileTokens {
    /** Above this font scale the name and status of the bar wrap instead of ending in an ellipsis. */
    const val WRAP_FONT_SCALE: Float = 1.3f

    /** The space between the edge of the tile and the badge, the buttons and the name bar. */
    val Padding: Dp = MonarchTokens.Space.space2.dp

    /** The space between the parts of a row. */
    val Gap: Dp = MonarchTokens.Space.space1_5.dp

    /** The horizontal padding of the name bar and the hand badge. */
    val BarPaddingHorizontal: Dp = MonarchTokens.Space.space1_5.dp

    /** The vertical padding of the name bar and the hand badge. */
    val BarPaddingVertical: Dp = MonarchTokens.Space.space0_5.dp

    /** The border of a speaking participant. */
    val SpeakingBorder: Dp = MonarchTokens.Space.space0_5.dp

    /** The width of one connection bar. */
    val QualityBarWidth: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the connection bars. */
    val QualityBarGap: Dp = MonarchTokens.Space.px.dp

    /** The heights of the three connection bars, shortest first. */
    val QualityBarHeights: List<Dp> = listOf(
        MonarchTokens.Space.space1_5.dp,
        MonarchTokens.Space.space2_5.dp,
        MonarchTokens.Space.space3.dp,
    )

    /** The tile is at least 16 by 9, and grows when its text needs more room. */
    const val ASPECT_RATIO: Float = 16f / 9f

    /** How far a reaction rises as it appears, as a fraction of its height. */
    const val REACTION_RISE: Int = 2

    /** The tile behind the video or the avatar: the emphasis background. */
    fun tileColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.bgEmphasis, colors.textDefault, null)

    /** The name bar and the buttons: the inverse surface with inverse text. */
    fun barColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.bgInverse, colors.textInverse, null)

    /** The raised-hand badge: the solid warning colour. */
    fun handColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.warningSolid, colors.warningOnSolid, null)

    /** The border of a speaking participant. */
    fun speakingColor(colors: MonarchColors): Color = colors.successSolid

    /** The muted microphone in the name bar. */
    fun mutedColor(colors: MonarchColors): Color = colors.dangerStrongInverse

    /** The status text in the name bar, in the tone's strong colour for the inverse surface. */
    fun statusColor(tone: MonarchTone, colors: MonarchColors): Color = MonarchTonePalette.strongInverse(tone, colors)

    /** The colour of a lit connection bar; unlit bars use the muted inverse text colour. */
    fun qualityColor(quality: MonarchCallQuality, colors: MonarchColors): Color = when (quality) {
        MonarchCallQuality.Good -> colors.successSolid
        MonarchCallQuality.Fair -> colors.warningSolid
        MonarchCallQuality.Poor -> colors.dangerSolid
    }

    /** The colour of an unlit connection bar. */
    fun unlitColor(colors: MonarchColors): Color = colors.textInverseMuted
}
