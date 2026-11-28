package com.rookhq.monarch.components.interaction.appearancepanel

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.theme.MonarchContrast
import com.rookhq.monarch.theme.MonarchScheme
import com.rookhq.monarch.tokens.MonarchAccent

/**
 * Every appearance choice `MonarchAppearancePanel` edits. Pass [scheme],
 * [contrast] and [accent] to `MonarchTheme`; apply [motion] and [density] in
 * the app.
 *
 * @property accent the accent colour, for example `MonarchTokens.Accents.rook`, the default of `MonarchTheme`.
 * @property scheme light, dark or system.
 * @property contrast standard, more or system contrast.
 * @property motion system or reduced motion.
 * @property density comfortable or compact spacing.
 */
@Immutable
public data class MonarchAppearancePreferences(
    public val accent: MonarchAccent,
    public val scheme: MonarchScheme = MonarchScheme.System,
    public val contrast: MonarchContrast = MonarchContrast.System,
    public val motion: MonarchAppearanceMotion = MonarchAppearanceMotion.System,
    public val density: MonarchAppearanceDensity = MonarchAppearanceDensity.Comfortable,
) {
    /**
     * Whether the app's own animations should be reduced: always with
     * [MonarchAppearanceMotion.Reduce], otherwise as the device says.
     *
     * @param systemReducesMotion whether the device has animations turned off.
     */
    public fun reducesMotion(systemReducesMotion: Boolean): Boolean =
        motion == MonarchAppearanceMotion.Reduce || systemReducesMotion
}
