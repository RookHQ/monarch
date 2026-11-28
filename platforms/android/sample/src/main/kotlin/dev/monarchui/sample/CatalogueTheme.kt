package dev.monarchui.sample

import dev.monarchui.tokens.MonarchThemeTokens
import dev.monarchui.tokens.MonarchTokens

/** One of the four themes the catalogue can show. */
enum class CatalogueTheme(val label: String, val tokens: MonarchThemeTokens) {
    Light("Light", MonarchTokens.light),
    Dark("Dark", MonarchTokens.dark),
    HighContrastLight("High contrast light", MonarchTokens.hcLight),
    HighContrastDark("High contrast dark", MonarchTokens.hcDark),
}
