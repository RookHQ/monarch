package com.rookhq.monarch.theme

import com.rookhq.monarch.tokens.MonarchAccent
import com.rookhq.monarch.tokens.MonarchThemeTokens
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * Resolves the three appearance inputs to one of the four Monarch themes.
 *
 * A pure function: the system state is passed in, so the rule can be tested
 * without a device. [MonarchTheme] calls it with the current system values.
 *
 * @param scheme light, dark or follow the system.
 * @param contrast standard, more or follow the system.
 * @param accent the brand accent applied to the resolved theme.
 * @param systemInDarkTheme whether the system is in dark theme; used when [scheme] is [MonarchScheme.System].
 * @param systemInHighContrast whether the system asks for more contrast; used when [contrast] is
 * [MonarchContrast.System].
 * @return `light`, `dark`, `hc-light` or `hc-dark` with [accent] applied.
 */
public fun resolveMonarchTheme(
    scheme: MonarchScheme,
    contrast: MonarchContrast,
    accent: MonarchAccent,
    systemInDarkTheme: Boolean,
    systemInHighContrast: Boolean,
): MonarchThemeTokens {
    val dark = when (scheme) {
        MonarchScheme.System -> systemInDarkTheme
        MonarchScheme.Light -> false
        MonarchScheme.Dark -> true
    }
    val highContrast = when (contrast) {
        MonarchContrast.System -> systemInHighContrast
        MonarchContrast.Standard -> false
        MonarchContrast.More -> true
    }
    val base = when {
        dark && highContrast -> MonarchTokens.hcDark
        dark -> MonarchTokens.dark
        highContrast -> MonarchTokens.hcLight
        else -> MonarchTokens.light
    }
    return base.withAccent(accent)
}
