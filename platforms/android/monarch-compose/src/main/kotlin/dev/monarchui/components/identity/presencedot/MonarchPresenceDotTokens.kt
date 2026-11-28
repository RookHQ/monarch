package dev.monarchui.components.identity.presencedot

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The form a presence is drawn in. */
internal enum class MonarchPresenceDotForm {
    /** A filled circle. */
    Filled,

    /** A ring with its leading half filled. */
    HalfFilled,

    /** A filled square with small corners. */
    Square,

    /** A filled circle with a bar across it. */
    Barred,

    /** A ring on the surface colour. */
    Ring,
}

/** Component tokens of MonarchPresenceDot, built from primitives and roles only. */
internal object MonarchPresenceDotTokens {
    /** The width of the offline ring, the bar and the overlay ring. */
    val StrokeWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** The corner radius of the busy square in the high-contrast themes. */
    val SquareCorner: Dp = MonarchTokens.Space.px.dp

    /** How far the do-not-disturb bar stays from each side, as a fraction of the dot. */
    const val BAR_INSET: Float = 0.22f

    /** The diameter of the dot at the default font size. */
    fun diameter(size: MonarchPresenceDotSize): Dp = when (size) {
        MonarchPresenceDotSize.Sm -> MonarchTokens.Space.space2.dp
        MonarchPresenceDotSize.Md -> MonarchTokens.Space.space2_5.dp
        MonarchPresenceDotSize.Lg -> MonarchTokens.Space.space3.dp
    }

    /** The main colour of the presence: the fill, or the ring of offline. */
    fun color(status: MonarchPresenceStatus, colors: MonarchColors): Color = when (status) {
        MonarchPresenceStatus.Online -> colors.successSolid
        MonarchPresenceStatus.Away -> colors.warningSolid
        MonarchPresenceStatus.Busy, MonarchPresenceStatus.DoNotDisturb -> colors.dangerSolid
        MonarchPresenceStatus.Offline -> colors.textSubtle
    }

    /** The colour of the bar across do not disturb. */
    fun barColor(colors: MonarchColors): Color = colors.dangerOnSolid

    /** The colour inside the offline ring and of the overlay ring. */
    fun surfaceColor(colors: MonarchColors): Color = colors.bgSurface

    /**
     * The form of the presence. Do not disturb has a bar and offline is a
     * ring in every theme. In the high-contrast themes away is half-filled
     * and busy is a square, so all five differ by form, as the web does in
     * forced-colours mode.
     */
    fun form(status: MonarchPresenceStatus, highContrast: Boolean): MonarchPresenceDotForm = when (status) {
        MonarchPresenceStatus.Online -> MonarchPresenceDotForm.Filled
        MonarchPresenceStatus.Away ->
            if (highContrast) MonarchPresenceDotForm.HalfFilled else MonarchPresenceDotForm.Filled
        MonarchPresenceStatus.Busy ->
            if (highContrast) MonarchPresenceDotForm.Square else MonarchPresenceDotForm.Filled
        MonarchPresenceStatus.DoNotDisturb -> MonarchPresenceDotForm.Barred
        MonarchPresenceStatus.Offline -> MonarchPresenceDotForm.Ring
    }

    /** The custom status without surrounding white space, or `null` when it has no text. */
    fun customText(customStatus: String?): String? = customStatus?.trim()?.takeIf { it.isNotEmpty() }
}
