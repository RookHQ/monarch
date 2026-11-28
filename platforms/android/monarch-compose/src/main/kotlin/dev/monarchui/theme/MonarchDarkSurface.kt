package dev.monarchui.theme

import androidx.compose.runtime.Composable
import dev.monarchui.tokens.MonarchTokens

/**
 * Shows [content] in the dark theme (high contrast when the app's theme is) unless the app's theme is dark already.
 * For a surface that is dark in every theme, such as a photo viewer.
 */
@Composable
internal fun MonarchDarkSurface(content: @Composable () -> Unit) {
    if (MonarchTheme.isDark) {
        content()
    } else {
        val tokens = if (MonarchTheme.isHighContrast) MonarchTokens.hcDark else MonarchTokens.dark
        MonarchTheme(tokens = tokens, typography = MonarchTheme.typography, content = content)
    }
}
