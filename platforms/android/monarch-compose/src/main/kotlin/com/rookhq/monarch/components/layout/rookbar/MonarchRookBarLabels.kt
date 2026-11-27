package com.rookhq.monarch.components.layout.rookbar

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchRookBar`. Every `null` uses the built-in string named after
 * it in `strings_rookbar.xml`.
 *
 * @property nav the spoken name of the bar and its menu sheet: "Rook".
 * @property openMenu the spoken name of the menu button: "Open menu".
 * @property signIn "Log in".
 * @property signUp "Sign up".
 * @property dashboard "Open dashboard".
 * @property account "Account settings".
 * @property signOut "Sign out".
 */
@Immutable
public class MonarchRookBarLabels(
    public val nav: String? = null,
    public val openMenu: String? = null,
    public val signIn: String? = null,
    public val signUp: String? = null,
    public val dashboard: String? = null,
    public val account: String? = null,
    public val signOut: String? = null,
)
