package dev.monarchui.components.forms.passwordinput

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchPasswordInput`. Every
 * `null` keeps the built-in string `monarch_passwordinput_<key>`.
 *
 * @property reveal the spoken name of the reveal toggle, the same in both states ("Show password").
 * @property show the visible text of the toggle while the password is hidden ("Show").
 * @property hide the visible text of the toggle while the password is shown ("Hide").
 */
@Immutable
public class MonarchPasswordInputLabels(
    public val reveal: String? = null,
    public val show: String? = null,
    public val hide: String? = null,
)
