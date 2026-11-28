package dev.monarchui.components.interaction.appearancepanel

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAppearancePanel`. Every
 * `null` keeps the built-in string `monarch_appearancepanel_<key>`; unset
 * theme names keep `MonarchThemeToggle`'s.
 *
 * @property theme the label of the theme row, "Theme".
 * @property themeDescription the line under it.
 * @property light the light choice, "Light".
 * @property dark the dark choice, "Dark".
 * @property system the system theme choice, "System".
 * @property contrast the label of the contrast row, "Contrast".
 * @property contrastDescription the line under it.
 * @property contrastStandard the standard choice, "Standard".
 * @property contrastMore the more-contrast choice, "More".
 * @property contrastSystem the system choice, "System".
 * @property accent the label of the accent row, "Accent".
 * @property accentDescription the line under it.
 * @property motion the label of the motion row, "Motion".
 * @property motionDescription the line under it.
 * @property motionSystem the system choice, "System".
 * @property motionReduce the reduce choice, "Reduce".
 * @property density the label of the density row, "Density".
 * @property densityDescription the line under it.
 * @property densityComfortable the comfortable choice, "Comfortable".
 * @property densityCompact the compact choice, "Compact".
 */
@Immutable
public class MonarchAppearancePanelLabels(
    public val theme: String? = null,
    public val themeDescription: String? = null,
    public val light: String? = null,
    public val dark: String? = null,
    public val system: String? = null,
    public val contrast: String? = null,
    public val contrastDescription: String? = null,
    public val contrastStandard: String? = null,
    public val contrastMore: String? = null,
    public val contrastSystem: String? = null,
    public val accent: String? = null,
    public val accentDescription: String? = null,
    public val motion: String? = null,
    public val motionDescription: String? = null,
    public val motionSystem: String? = null,
    public val motionReduce: String? = null,
    public val density: String? = null,
    public val densityDescription: String? = null,
    public val densityComfortable: String? = null,
    public val densityCompact: String? = null,
)
