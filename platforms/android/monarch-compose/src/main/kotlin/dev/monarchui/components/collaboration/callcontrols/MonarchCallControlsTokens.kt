package dev.monarchui.components.collaboration.callcontrols

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and logic of MonarchCallControls, built from primitives and roles only. */
internal object MonarchCallControlsTokens {
    val ControlSize: Dp = MonarchTokens.Control.xl.dp
    /** The wider pill of the Leave control. */
    val WideWidth: Dp = ControlSize * 1.3f
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /**
     * The fill and content of a round control. A device that is off takes the
     * danger fill; sharing takes the success fill and a raised hand the warning
     * fill; everything else rests on the muted fill. The icon changes as well,
     * so colour is never the only cue.
     */
    fun colors(
        control: MonarchCallControl,
        on: Boolean,
        enabled: Boolean,
        colors: MonarchColors,
    ): MonarchSurfaceColors {
        val resting = MonarchSurfaceColors(colors.bgMuted, colors.textDefault, null)
        return when {
            !enabled -> MonarchSurfaceColors(colors.bgMuted, colors.textDisabled, null)
            control == MonarchCallControl.Microphone || control == MonarchCallControl.Camera ->
                if (on) resting else MonarchSurfaceColors(colors.dangerSolid, colors.dangerOnSolid, null)
            control == MonarchCallControl.ShareScreen && on ->
                MonarchSurfaceColors(colors.successSolid, colors.successOnSolid, null)
            control == MonarchCallControl.RaiseHand && on ->
                MonarchSurfaceColors(colors.warningSolid, colors.warningOnSolid, null)
            else -> resting
        }
    }

    /** Whether the More menu is shown: it has an item, and never in the minimal form. */
    fun showsMore(minimal: Boolean, itemCount: Int): Boolean = !minimal && itemCount > 0

    /** Whether the reactions menu is shown. */
    fun showsReactions(minimal: Boolean, reactions: List<String>, hasAction: Boolean): Boolean =
        !minimal && hasAction && reactions.isNotEmpty()

    /** Whether the hand toggle is shown. */
    fun showsHand(minimal: Boolean, hasAction: Boolean): Boolean = !minimal && hasAction
}
