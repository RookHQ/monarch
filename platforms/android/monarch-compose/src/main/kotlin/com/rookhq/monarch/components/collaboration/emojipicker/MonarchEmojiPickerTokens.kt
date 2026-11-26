package com.rookhq.monarch.components.collaboration.emojipicker

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens

/** The size of MonarchEmojiPicker. */
internal object MonarchEmojiPickerTokens {
    /** The default height: room for the category tabs and about seven rows of emoji. */
    val Height: Dp = (MonarchTokens.Space.space24 * 3 + MonarchTokens.Space.space8).dp
}
