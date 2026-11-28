package com.rookhq.monarch.components.intelligence.aimessageactions

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMessageFeedback
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiMessageActions. */
internal object MonarchAiMessageActionsTokens {
    val Gap: Dp = MonarchTokens.Space.space1.dp

    /** The rating after pressing [pressed]: pressing the current rating again clears it. */
    fun nextFeedback(current: MonarchAiMessageFeedback?, pressed: MonarchAiMessageFeedback): MonarchAiMessageFeedback? =
        if (current == pressed) null else pressed
}
