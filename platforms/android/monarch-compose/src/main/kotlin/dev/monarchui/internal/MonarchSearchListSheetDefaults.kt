package dev.monarchui.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/** The spacing of [MonarchSearchListSheet]. */
internal object MonarchSearchListSheetDefaults {
    /** The padding at the sides of the sheet and under its last button. */
    val Padding: Dp = MonarchTokens.Space.space4.dp

    /** The gap between the title, the search field, the list and the buttons. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The sheet colour of the pickers. */
    @Composable
    fun containerColor(): Color = MonarchTheme.colors.bgSurface
}
