package com.rookhq.monarch.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.rookhq.monarch.foundation.MonarchShapes
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.tokens.MonarchThemeTokens

/**
 * The one place where Monarch is mapped to Material 3. Wraps `MaterialTheme`
 * with a colour scheme, typography and shapes derived from the Monarch values.
 */
@Composable
internal fun MonarchMaterialTheme(
    tokens: MonarchThemeTokens,
    colors: MonarchColors,
    typography: MonarchTypography,
    shapes: MonarchShapes,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val mapped = remember(colors, tokens.isDark) { colors.toMaterialColorScheme(tokens.isDark) }
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (tokens.isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        mapped
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = remember(typography) { typography.toMaterialTypography() },
        shapes = remember(shapes) { shapes.toMaterialShapes() },
        content = content,
    )
}

/**
 * Monarch roles as a Material 3 colour scheme.
 *
 * Material uses `primary` and `error` both as fills and as text on a surface,
 * so they map to the `strong` roles, which the contrast report covers in both
 * positions. `MonarchButton` uses the `solid` roles for its fill. Monarch has
 * no secondary brand colour: `secondary` repeats the accent and `tertiary` is
 * the info intent.
 */
internal fun MonarchColors.toMaterialColorScheme(isDark: Boolean): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accentStrong,
        onPrimary = accentOnStrong,
        primaryContainer = accentSubtle,
        onPrimaryContainer = accentStrong,
        inversePrimary = accentStrongInverse,
        secondary = accentStrong,
        onSecondary = accentOnStrong,
        secondaryContainer = accentSubtle,
        onSecondaryContainer = accentStrong,
        tertiary = infoStrong,
        onTertiary = infoOnStrong,
        tertiaryContainer = infoSubtle,
        onTertiaryContainer = infoStrong,
        background = bgCanvas,
        onBackground = textDefault,
        surface = bgSurface,
        onSurface = textDefault,
        surfaceVariant = bgMuted,
        onSurfaceVariant = textMuted,
        surfaceTint = accentStrong,
        inverseSurface = bgInverse,
        inverseOnSurface = textInverse,
        error = dangerStrong,
        onError = dangerOnStrong,
        errorContainer = dangerSubtle,
        onErrorContainer = dangerStrong,
        outline = borderInput,
        outlineVariant = borderDefault,
        scrim = overlayScrim,
        surfaceBright = bgSurface,
        surfaceDim = bgMuted,
        surfaceContainerLowest = bgSurface,
        surfaceContainerLow = bgRaised,
        surfaceContainer = bgRaised,
        surfaceContainerHigh = bgRaised,
        surfaceContainerHighest = bgMuted,
        primaryFixed = accentSubtle,
        primaryFixedDim = accentBorder,
        onPrimaryFixed = accentStrong,
        onPrimaryFixedVariant = accentStrong,
        secondaryFixed = accentSubtle,
        secondaryFixedDim = accentBorder,
        onSecondaryFixed = accentStrong,
        onSecondaryFixedVariant = accentStrong,
        tertiaryFixed = infoSubtle,
        tertiaryFixedDim = infoBorder,
        onTertiaryFixed = infoStrong,
        onTertiaryFixedVariant = infoStrong,
    )
}

/** Monarch type roles as the fifteen Material 3 text styles. */
internal fun MonarchTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = display,
    displayMedium = display,
    displaySmall = display,
    headlineLarge = title,
    headlineMedium = title,
    headlineSmall = heading,
    titleLarge = heading,
    titleMedium = subheading,
    titleSmall = label,
    bodyLarge = body,
    bodyMedium = body,
    bodySmall = caption,
    labelLarge = label,
    labelMedium = caption,
    labelSmall = caption,
)

/** Monarch radii as the five Material 3 shape sizes. */
internal fun MonarchShapes.toMaterialShapes(): Shapes = Shapes(
    extraSmall = xs,
    small = md,
    medium = lg,
    large = xl,
    extraLarge = xl2,
)
