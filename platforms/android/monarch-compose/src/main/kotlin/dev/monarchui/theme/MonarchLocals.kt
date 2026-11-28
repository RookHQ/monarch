package dev.monarchui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import dev.monarchui.foundation.MonarchElevation
import dev.monarchui.foundation.MonarchMotion
import dev.monarchui.foundation.MonarchShapes
import dev.monarchui.foundation.MonarchSpacing
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.tokens.MonarchThemeTokens
import dev.monarchui.tokens.MonarchTokens

// The values MonarchTheme provides. The defaults are the light theme, so a
// component used outside MonarchTheme still renders with Monarch values.

internal val LocalMonarchThemeTokens = staticCompositionLocalOf<MonarchThemeTokens> { MonarchTokens.light }

internal val LocalMonarchColors = staticCompositionLocalOf { MonarchColors(MonarchTokens.light.colors) }

internal val LocalMonarchTypography = staticCompositionLocalOf { MonarchTypography.Default }

internal val LocalMonarchShapes = staticCompositionLocalOf { MonarchShapes.Default }

internal val LocalMonarchSpacing = staticCompositionLocalOf { MonarchSpacing.Default }

internal val LocalMonarchElevation = staticCompositionLocalOf { MonarchElevation.from(MonarchTokens.light.shadows) }

internal val LocalMonarchMotion = staticCompositionLocalOf { MonarchMotion(reducedMotion = false) }
