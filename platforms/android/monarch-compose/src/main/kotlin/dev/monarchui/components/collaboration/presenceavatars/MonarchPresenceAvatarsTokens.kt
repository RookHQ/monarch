package dev.monarchui.components.collaboration.presenceavatars

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.identity.avatar.MonarchAvatarLogic
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and logic of MonarchPresenceAvatars, built from primitives and roles only. */
internal object MonarchPresenceAvatarsTokens {
    /** The padding around the title and the rows of the list. */
    val ListPadding: Dp = MonarchTokens.Space.space4.dp

    /** The space between the title and the Done button. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** How many avatars the stack shows; a "+1" tile is never shown. */
    fun visibleCount(total: Int, maxVisible: Int): Int =
        MonarchAvatarLogic.visibleCount(total, maxVisible.coerceAtLeast(0))

    /** The stack opens the list when some people are hidden or a person can be chosen. */
    fun opensList(total: Int, maxVisible: Int, clickable: Boolean): Boolean =
        clickable || visibleCount(total, maxVisible) < total

    /** The activity, or `null` when it is empty or white space. */
    fun activityText(activity: String?): String? = activity?.trim()?.takeIf { it.isNotEmpty() }
}
