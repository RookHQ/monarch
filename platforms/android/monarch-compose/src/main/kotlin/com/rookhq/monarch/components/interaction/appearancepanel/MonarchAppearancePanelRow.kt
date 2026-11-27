package com.rookhq.monarch.components.interaction.appearancepanel

/** A row of `MonarchAppearancePanel`. */
public enum class MonarchAppearancePanelRow {
    /** Light, dark or system, with `MonarchThemeToggle`. */
    Theme,

    /** Standard, more or system contrast. */
    Contrast,

    /** The accent colour, chosen by name from a menu. */
    Accent,

    /** System or reduced motion. */
    Motion,

    /** Comfortable or compact density. */
    Density,
    ;

    /** The standard rows. */
    public companion object {
        /** Theme, contrast, motion and density, as on the web. Add [Accent] for the accent menu. */
        public val Standard: List<MonarchAppearancePanelRow> = listOf(Theme, Contrast, Motion, Density)
    }
}
