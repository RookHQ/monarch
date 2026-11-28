package dev.monarchui.theme

import dev.monarchui.tokens.MonarchAccent
import dev.monarchui.tokens.MonarchTokens

/** The defaults of `MonarchTheme`, for apps and components that need them without the token module. */
public object MonarchThemeDefaults {
    /** The accent `MonarchTheme` uses when none is given: the Rook accent. */
    public val Accent: MonarchAccent get() = MonarchTokens.Accents.rook
}
