package com.rookhq.monarch.theme

import com.rookhq.monarch.tokens.MonarchAccent
import com.rookhq.monarch.tokens.MonarchTokens

/** The defaults of `MonarchTheme`, for apps and components that need them without the token module. */
public object MonarchThemeDefaults {
    /** The accent `MonarchTheme` uses when none is given: the Rook accent. */
    public val Accent: MonarchAccent get() = MonarchTokens.Accents.rook
}
