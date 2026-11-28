package dev.monarchui.components.forms.select

import androidx.compose.runtime.Immutable

/**
 * One choice of a `MonarchSelect`.
 *
 * @property value the value passed to `onSelect` and compared with `selected`.
 * @property label the visible name of the option, shown in the menu and in the field once chosen. Already
 * localised.
 * @property description a second line under the label in the menu.
 * @property enabled whether the option can be chosen. A disabled option stays visible in the disabled colour.
 * @property group the heading the option is listed under. Consecutive options with the same group share one
 * heading; `null` lists the option without one.
 */
@Immutable
public class MonarchSelectOption<out T>(
    public val value: T,
    public val label: String,
    public val description: String? = null,
    public val enabled: Boolean = true,
    public val group: String? = null,
)
