package com.rookhq.monarch.components.interaction.themetoggle

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchThemeToggle`. Every
 * `null` uses the built-in string named after it in `strings_themetoggle.xml`.
 *
 * @property title the name of the control, spoken by TalkBack and shown at the top of the menu: "Theme".
 * @property light the name of the light choice: "Light".
 * @property dark the name of the dark choice: "Dark".
 * @property system the name of the choice that follows the system setting: "System".
 */
@Immutable
public class MonarchThemeToggleLabels(
    public val title: String? = null,
    public val light: String? = null,
    public val dark: String? = null,
    public val system: String? = null,
)
