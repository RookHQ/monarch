package dev.monarchui.components.overlays.unsavedchangesguard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchUnsavedChangesGuard. */
internal object MonarchUnsavedChangesGuardTokens {
    /** The gap between the message, the details and the error. */
    val ContentGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the buttons. */
    val ActionGap: Dp = MonarchTokens.Space.space2.dp

    /** Whether back is intercepted: while there are changes, and always while saving. */
    fun interceptsBack(guardsBack: Boolean, hasChanges: Boolean, saving: Boolean): Boolean =
        saving || (guardsBack && hasChanges)

    /** Whether back opens the question: not while saving, when back is only swallowed. */
    fun backAsks(saving: Boolean): Boolean = !saving

    /** The colour of the save error. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong

    /** The colour of the message. */
    fun messageColor(colors: MonarchColors): Color = colors.textMuted
}
