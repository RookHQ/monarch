package dev.monarchui.components.interaction.themetoggle

/** How a `MonarchThemeToggle` shows the three choices. */
public enum class MonarchThemeToggleVariant {
    /** Three visible segments, Light, Dark and System. For settings screens. The default. */
    Segmented,

    /** One button that opens a menu with the three choices. For toolbars and account menus. */
    Menu,
}
