package com.rookhq.monarch.components.navigation.skiplink

import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant

/** Component tokens of MonarchSkipLink. */
internal object MonarchSkipLinkTokens {
    /** A quiet secondary button: it helps keyboard and TalkBack users without competing with the screen. */
    val Variant: MonarchButtonVariant = MonarchButtonVariant.Secondary
    val Size: MonarchButtonSize = MonarchButtonSize.Sm

    /** Placed before its siblings in the TalkBack order, whatever its position in the layout. */
    const val TRAVERSAL_INDEX: Float = -1f
}
