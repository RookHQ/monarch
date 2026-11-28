package com.rookhq.monarch.internal

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.theme.MonarchColors

/** How strongly a toned surface stands out. The words are the shared vocabulary: subtle, solid, outline. */
internal enum class MonarchToneEmphasis {
    /** A tinted fill with strong text. */
    Subtle,

    /** A strong fill with the text colour made for it. */
    Solid,

    /** No fill, strong text and a border. */
    Outline,
}

/**
 * The fill, content and border colour of a surface in one state.
 *
 * @property container the fill. `Color.Transparent` for no fill.
 * @property content the colour of text and icons on the fill.
 * @property border the outline, or `null` for none.
 */
@Immutable
internal data class MonarchSurfaceColors(val container: Color, val content: Color, val border: Color?)

/**
 * The colour roles of one tone, taken from one family, so every pair is one
 * the contrast report covers: `strong` on `subtle`, `onSolid` on `solid`.
 *
 * @property subtle a tinted background.
 * @property strong text and icons on `subtle` or on a surface.
 * @property solid a strong fill.
 * @property onSolid text and icons on `solid`.
 * @property border an outline in the tone.
 */
@Immutable
internal data class MonarchTonePalette(
    val subtle: Color,
    val strong: Color,
    val solid: Color,
    val onSolid: Color,
    val border: Color,
) {
    /** The colours of a surface in this tone at [emphasis]. */
    fun surface(emphasis: MonarchToneEmphasis): MonarchSurfaceColors = when (emphasis) {
        MonarchToneEmphasis.Subtle -> MonarchSurfaceColors(subtle, strong, null)
        MonarchToneEmphasis.Solid -> MonarchSurfaceColors(solid, onSolid, null)
        MonarchToneEmphasis.Outline -> MonarchSurfaceColors(Color.Transparent, strong, border)
    }

    companion object {
        /**
         * The strong colour of [tone] for text on the inverse surface (a
         * snackbar, a call tile name bar). Neutral uses the muted inverse text.
         */
        fun strongInverse(tone: MonarchTone, colors: MonarchColors): Color = when (tone) {
            MonarchTone.Neutral -> colors.textInverseMuted
            MonarchTone.Info -> colors.infoStrongInverse
            MonarchTone.Success -> colors.successStrongInverse
            MonarchTone.Warning -> colors.warningStrongInverse
            MonarchTone.Danger -> colors.dangerStrongInverse
            MonarchTone.Accent -> colors.accentStrongInverse
            MonarchTone.Ai -> colors.aiStrongInverse
        }

        /**
         * The palette of [tone] in the theme that [colors] belongs to. The
         * neutral tone uses the muted background, muted text and the inverse
         * surface, because the theme has no neutral intent family.
         */
        fun of(tone: MonarchTone, colors: MonarchColors): MonarchTonePalette = when (tone) {
            MonarchTone.Neutral -> MonarchTonePalette(
                subtle = colors.bgMuted,
                strong = colors.textMuted,
                solid = colors.bgInverse,
                onSolid = colors.textInverse,
                border = colors.borderDefault,
            )
            MonarchTone.Info -> MonarchTonePalette(
                colors.infoSubtle, colors.infoStrong, colors.infoSolid, colors.infoOnSolid, colors.infoBorder,
            )
            MonarchTone.Success -> MonarchTonePalette(
                colors.successSubtle,
                colors.successStrong,
                colors.successSolid,
                colors.successOnSolid,
                colors.successBorder,
            )
            MonarchTone.Warning -> MonarchTonePalette(
                colors.warningSubtle,
                colors.warningStrong,
                colors.warningSolid,
                colors.warningOnSolid,
                colors.warningBorder,
            )
            MonarchTone.Danger -> MonarchTonePalette(
                colors.dangerSubtle, colors.dangerStrong, colors.dangerSolid, colors.dangerOnSolid, colors.dangerBorder,
            )
            MonarchTone.Accent -> MonarchTonePalette(
                colors.accentSubtle, colors.accentStrong, colors.accentSolid, colors.accentOnSolid, colors.accentBorder,
            )
            MonarchTone.Ai -> MonarchTonePalette(
                colors.aiSubtle, colors.aiStrong, colors.aiSolid, colors.aiOnSolid, colors.aiBorder,
            )
        }
    }
}
