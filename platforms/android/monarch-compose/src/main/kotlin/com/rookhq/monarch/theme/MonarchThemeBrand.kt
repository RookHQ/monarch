package com.rookhq.monarch.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.tokens.MonarchAccent
import com.rookhq.monarch.tokens.MonarchBrandTheme

/**
 * Applies the Monarch theme with an accent derived from one brand colour.
 *
 * The colour runs through [MonarchBrandTheme], the same engine as the web's
 * `deriveTheme`, so the accent meets Monarch's contrast guarantees in the
 * light, dark and both high-contrast themes. Monarch components and the
 * Material 3 colour scheme both follow it. An invalid colour, such as an empty
 * string or a named colour, silently gives the default accent.
 *
 * ```kotlin
 * MonarchTheme(brand = workspace.brandColor) {
 *     // content
 * }
 * ```
 *
 * @param brand the brand colour, `#rgb` or `#rrggbb`.
 * @param scheme light, dark or follow the system dark theme setting.
 * @param contrast standard, more or follow the system contrast setting.
 * @param typography the type roles.
 * @param dynamicColor see the `accent` overload of [MonarchTheme].
 * @param ai a product colour for AI content, applied after the brand accent with `withAi`. `null` keeps the
 * theme's own AI colours.
 * @param content the themed UI.
 */
@Composable
public fun MonarchTheme(
    brand: String,
    scheme: MonarchScheme = MonarchScheme.System,
    contrast: MonarchContrast = MonarchContrast.System,
    typography: MonarchTypography = MonarchTypography.Default,
    dynamicColor: Boolean = false,
    ai: MonarchAccent? = null,
    content: @Composable () -> Unit,
) {
    val accent = remember(brand) { MonarchBrandTheme.derive(brand).accent }
    MonarchTheme(
        scheme = scheme,
        contrast = contrast,
        accent = accent,
        typography = typography,
        dynamicColor = dynamicColor,
        ai = ai,
        content = content,
    )
}
