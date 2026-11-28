package dev.monarchui.components.collaboration.emojipicker

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.tokens.MonarchTokens

/** The size of MonarchEmojiPicker. */
internal object MonarchEmojiPickerTokens {
    /** The default height: room for the category tabs and about seven rows of emoji. */
    val Height: Dp = (MonarchTokens.Space.space24 * 3 + MonarchTokens.Space.space8).dp
}
