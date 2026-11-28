package dev.monarchui.components.layout.rookbar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchRookBar, built from primitives and roles only. */
internal object MonarchRookBarTokens {
    val Height: Dp = MonarchTokens.Space.space16.dp
    val PaddingStart: Dp = MonarchTokens.Space.space4.dp
    val PaddingEnd: Dp = MonarchTokens.Space.space1.dp
    val BrandGap: Dp = MonarchTokens.Space.space2.dp
    val ActionGap: Dp = MonarchTokens.Space.space1.dp
    val SheetGap: Dp = MonarchTokens.Space.space4.dp

    /** The narrowest bar that shows "Log in" and "Sign up" beside the brand. */
    val InlineActionsWidth: Dp = (MonarchTokens.Space.space24 * 4 + MonarchTokens.Space.space16).dp

    /** The narrowest bar that shows "Sign up" beside the brand; narrower moves both buttons to the menu. */
    val InlineSignUpWidth: Dp = (MonarchTokens.Space.space24 * 3).dp

    /** Above this font scale the account buttons move to the menu sheet. */
    const val INLINE_ACTIONS_MAX_FONT_SCALE: Float = 1.3f

    /** The bar is the surface with a subtle line under it. */
    fun fill(colors: MonarchColors): Color = colors.bgSurface

    /** "Sign up" is the inverse pill of the company site. */
    val SignUpVariant: MonarchButtonVariant = MonarchButtonVariant.Inverse
}
