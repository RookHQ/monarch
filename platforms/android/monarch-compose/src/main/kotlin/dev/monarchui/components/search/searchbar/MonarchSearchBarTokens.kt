package dev.monarchui.components.search.searchbar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The colour roles of a search bar. */
@Immutable
internal data class SearchBarRoles(
    val container: Color,
    val divider: Color,
    val text: Color,
    val disabledText: Color,
    val placeholder: Color,
    val icon: Color,
    val cursor: Color,
)

/** Component tokens of MonarchSearchBar, built from primitives and roles only. */
internal object MonarchSearchBarTokens {
    /** The size of the progress indicator that replaces the leading icon while a search runs. */
    val PendingSize: Dp = MonarchTokens.Space.space5.dp

    /** No tonal elevation: Monarch surfaces are flat and use the subtle background. */
    val Elevation: Dp = MonarchTokens.Space.space0.dp

    /** The colours: the subtle background, default text, subtle placeholder, muted icons, the accent cursor. */
    fun roles(colors: MonarchColors): SearchBarRoles = SearchBarRoles(
        container = colors.bgSubtle,
        divider = colors.borderSubtle,
        text = colors.textDefault,
        disabledText = colors.textDisabled,
        placeholder = colors.textSubtle,
        icon = colors.textMuted,
        cursor = colors.accentSolid,
    )
}
