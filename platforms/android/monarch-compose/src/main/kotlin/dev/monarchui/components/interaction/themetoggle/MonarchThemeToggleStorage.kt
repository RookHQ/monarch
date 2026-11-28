package dev.monarchui.components.interaction.themetoggle

import dev.monarchui.theme.MonarchScheme

/**
 * The stored form of a theme choice, for the app to keep in DataStore or
 * `SharedPreferences`.
 *
 * The values are `light`, `dark` and `system`, the same words the web
 * `rook_theme` cookie and the iOS toggle use, so a choice synced between
 * platforms means the same everywhere.
 */
public object MonarchThemeToggleStorage {
    private const val LIGHT = "light"
    private const val DARK = "dark"
    private const val SYSTEM = "system"

    /**
     * The stored value of [scheme]: `light`, `dark` or `system`.
     *
     * @param scheme the choice to store.
     */
    public fun storageValue(scheme: MonarchScheme): String = when (scheme) {
        MonarchScheme.Light -> LIGHT
        MonarchScheme.Dark -> DARK
        MonarchScheme.System -> SYSTEM
    }

    /**
     * The choice a stored value names, or `null` when [value] is missing or is not one of the three values.
     *
     * @param value the stored value, for example read from DataStore.
     */
    public fun scheme(value: String?): MonarchScheme? = when (value) {
        LIGHT -> MonarchScheme.Light
        DARK -> MonarchScheme.Dark
        SYSTEM -> MonarchScheme.System
        else -> null
    }
}
