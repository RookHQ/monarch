package com.rookhq.monarch.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.isReducedMotionEnabled
import com.rookhq.monarch.foundation.MonarchElevation
import com.rookhq.monarch.foundation.MonarchMotion
import com.rookhq.monarch.foundation.MonarchShapes
import com.rookhq.monarch.foundation.MonarchSpacing
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.tokens.MonarchAccent
import com.rookhq.monarch.tokens.MonarchThemeTokens

/**
 * Applies the Monarch theme to [content].
 *
 * Resolves the three appearance inputs to one of the four themes with
 * [resolveMonarchTheme], provides Monarch's colours, typography, shapes,
 * spacing, elevation and motion through [MonarchTheme], and wraps
 * `MaterialTheme` so every Material 3 component inside is themed as well.
 *
 * @param scheme light, dark or follow the system dark theme setting.
 * @param contrast standard, more or follow the system contrast setting.
 * @param accent the brand accent; one of `MonarchTokens.Accents`. The default is `MonarchThemeDefaults.Accent`.
 * @param typography the type roles; pass `monarchTypography(fontFamily = …)` to use a product typeface.
 * @param dynamicColor when `true` on Android 12 or later, Material 3 components take the wallpaper-based
 * colour scheme instead of the Monarch mapping. Monarch components keep Monarch colours. Off by default
 * because the accent is part of the brand.
 * @param reducedMotion overrides the system "Remove animations" setting for this subtree: `true` makes every
 * Monarch animation an immediate change, `false` animates. `null` follows the system. For tests, screenshots
 * and an in-app motion setting.
 * @param ai a product colour for AI content (the `ai*` roles and the AI tone), applied after [accent] with
 * `withAi`. `null` keeps the theme's own AI colours.
 * @param content the themed UI.
 */
@Composable
public fun MonarchTheme(
    scheme: MonarchScheme = MonarchScheme.System,
    contrast: MonarchContrast = MonarchContrast.System,
    accent: MonarchAccent = MonarchThemeDefaults.Accent,
    typography: MonarchTypography = MonarchTypography.Default,
    dynamicColor: Boolean = false,
    reducedMotion: Boolean? = null,
    ai: MonarchAccent? = null,
    content: @Composable () -> Unit,
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    val systemInHighContrast = isSystemInHighContrast()
    val tokens = remember(scheme, contrast, accent, ai, systemInDarkTheme, systemInHighContrast) {
        val resolved = resolveMonarchTheme(scheme, contrast, accent, systemInDarkTheme, systemInHighContrast)
        if (ai == null) resolved else resolved.withAi(ai)
    }
    MonarchTheme(
        tokens = tokens,
        typography = typography,
        dynamicColor = dynamicColor,
        reducedMotion = reducedMotion,
        content = content,
    )
}

/**
 * Applies one specific theme to [content], without reading the system settings.
 *
 * Use this overload to pin a theme, for example in a preview, a screenshot or
 * a catalogue that shows all four themes.
 *
 * @param tokens the theme, for example `MonarchTokens.dark.withAccent(MonarchTokens.Accents.atlas)`.
 * @param typography the type roles.
 * @param dynamicColor see the other overload.
 * @param reducedMotion overrides the system "Remove animations" setting; `null` follows the system.
 * @param content the themed UI.
 */
@Composable
public fun MonarchTheme(
    tokens: MonarchThemeTokens,
    typography: MonarchTypography = MonarchTypography.Default,
    dynamicColor: Boolean = false,
    reducedMotion: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val colors = remember(tokens) { MonarchColors(tokens.colors) }
    val elevation = remember(tokens) { MonarchElevation.from(tokens.shadows) }
    val reduced = reducedMotion ?: isReducedMotionEnabled()
    val motion = remember(reduced) { MonarchMotion(reduced) }
    CompositionLocalProvider(
        LocalMonarchThemeTokens provides tokens,
        LocalMonarchColors provides colors,
        LocalMonarchTypography provides typography,
        LocalMonarchShapes provides MonarchShapes.Default,
        LocalMonarchSpacing provides MonarchSpacing.Default,
        LocalMonarchElevation provides elevation,
        LocalMonarchMotion provides motion,
    ) {
        MonarchMaterialTheme(tokens, colors, typography, MonarchShapes.Default, dynamicColor) {
            CompositionLocalProvider(
                LocalContentColor provides colors.textDefault,
                LocalTextStyle provides typography.body,
                LocalMinimumInteractiveComponentSize provides MonarchTouchTarget.Minimum,
                // Pressed, focused and hovered states in Monarch's colour, not Material's default ripple.
                LocalRippleConfiguration provides remember(colors) { monarchRippleConfiguration(colors) },
                content = content,
            )
        }
    }
}

/** Reads the values provided by the nearest [MonarchTheme] composable. */
public object MonarchTheme {
    /** The semantic colour roles of the active theme. */
    public val colors: MonarchColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMonarchColors.current

    /** The type roles. */
    public val typography: MonarchTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalMonarchTypography.current

    /** The corner shapes. */
    public val shapes: MonarchShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalMonarchShapes.current

    /** The spacing scale. */
    public val spacing: MonarchSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalMonarchSpacing.current

    /** The shadow levels of the active theme. */
    public val elevation: MonarchElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalMonarchElevation.current

    /** Durations, easing curves and the reduced-motion state. */
    public val motion: MonarchMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalMonarchMotion.current

    /** Whether the active theme is dark. */
    public val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalMonarchThemeTokens.current.isDark

    /** Whether the active theme is one of the high-contrast themes. */
    public val isHighContrast: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalMonarchThemeTokens.current.isHighContrast

    /** Width of the focus ring in the active theme. It is wider in the high-contrast themes. */
    public val focusRingWidth: Dp
        @Composable
        @ReadOnlyComposable
        get() = LocalMonarchThemeTokens.current.focusRingWidth.dp
}
