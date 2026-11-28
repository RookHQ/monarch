package dev.monarchui.testing

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchThemeTokens
import dev.monarchui.tokens.MonarchTokens

/** The four Monarch themes, in token order. */
internal val AllThemes: List<MonarchThemeTokens> =
    listOf(MonarchTokens.light, MonarchTokens.dark, MonarchTokens.hcLight, MonarchTokens.hcDark)

/** The largest font scale of the Android font size setting. */
internal const val LargestFontScale = 2f

/**
 * The theme, font scale and layout direction of a test, as state. Change a
 * property after `setMonarchContent` to re-render the same content under
 * another setting; that is much cheaper than a new test per setting.
 */
internal class MonarchTestEnvironment(
    tokens: MonarchThemeTokens = MonarchTokens.light,
    fontScale: Float = 1f,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    typography: MonarchTypography = MonarchTypography.Default,
) {
    var typography: MonarchTypography by mutableStateOf(typography)
    var tokens: MonarchThemeTokens by mutableStateOf(tokens)
    var fontScale: Float by mutableFloatStateOf(fontScale)
    var layoutDirection: LayoutDirection by mutableStateOf(layoutDirection)
}

/** Wraps [content] in the theme, font scale and layout direction of [environment], on the canvas colour. */
@Composable
internal fun MonarchTestFrame(environment: MonarchTestEnvironment, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, environment.fontScale),
        LocalLayoutDirection provides environment.layoutDirection,
    ) {
        MonarchTheme(tokens = environment.tokens, typography = environment.typography) {
            Surface(color = MonarchTheme.colors.bgCanvas, content = content)
        }
    }
}

/** Sets [content] inside a pinned Monarch theme, with an optional font scale and layout direction. */
internal fun ComposeContentTestRule.setMonarchContent(
    tokens: MonarchThemeTokens = MonarchTokens.light,
    fontScale: Float = 1f,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    content: @Composable () -> Unit,
) {
    setMonarchContent(MonarchTestEnvironment(tokens, fontScale, layoutDirection), content)
}

/** Sets [content] inside [environment], whose properties can be changed while the test runs. */
internal fun ComposeContentTestRule.setMonarchContent(
    environment: MonarchTestEnvironment,
    content: @Composable () -> Unit,
) {
    setContent { MonarchTestFrame(environment, content) }
}
