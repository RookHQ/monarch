package com.rookhq.monarch.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.rookhq.monarch.foundation.MonarchElevation
import com.rookhq.monarch.foundation.MonarchMotion
import com.rookhq.monarch.foundation.MonarchShapes
import com.rookhq.monarch.foundation.MonarchSpacing
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.tokens.MonarchThemeTokens
import com.rookhq.monarch.tokens.MonarchTokens

// The values MonarchTheme provides. The defaults are the light theme, so a
// component used outside MonarchTheme still renders with Monarch values.

internal val LocalMonarchThemeTokens = staticCompositionLocalOf<MonarchThemeTokens> { MonarchTokens.light }

internal val LocalMonarchColors = staticCompositionLocalOf { MonarchColors(MonarchTokens.light.colors) }

internal val LocalMonarchTypography = staticCompositionLocalOf { MonarchTypography.Default }

internal val LocalMonarchShapes = staticCompositionLocalOf { MonarchShapes.Default }

internal val LocalMonarchSpacing = staticCompositionLocalOf { MonarchSpacing.Default }

internal val LocalMonarchElevation = staticCompositionLocalOf { MonarchElevation.from(MonarchTokens.light.shadows) }

internal val LocalMonarchMotion = staticCompositionLocalOf { MonarchMotion(reducedMotion = false) }
