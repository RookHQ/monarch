package dev.monarchui.components.forms.passwordstrengthmeter

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchPasswordStrengthMeter`.
 * Every `null` keeps the built-in strings `monarch_passwordstrengthmeter_<key>`.
 *
 * @property strengthText the whole strength line, given the strength or `null` while the password is empty.
 * By default "Password strength: Good", or "Password strength: Not entered".
 * @property rulesLabel the spoken name of the list of rules ("Password requirements").
 * @property ruleText the spoken text of a rule, given its label and whether it is met. By default "Met: …" or
 * "Not met: …".
 */
@Immutable
public class MonarchPasswordStrengthMeterLabels(
    public val strengthText: ((MonarchPasswordStrength?) -> String)? = null,
    public val rulesLabel: String? = null,
    public val ruleText: ((label: String, met: Boolean) -> String)? = null,
)
