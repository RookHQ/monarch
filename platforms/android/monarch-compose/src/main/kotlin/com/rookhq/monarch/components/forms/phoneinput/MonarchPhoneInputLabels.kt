package com.rookhq.monarch.components.forms.phoneinput

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.geo.phonecountryselect.MonarchPhoneCountrySelectLabels

/**
 * Replacements for the built-in strings of a `MonarchPhoneInput`. Every
 * `null` keeps the built-in string.
 *
 * @property invalid shown under the number when it is too short or too long: "Enter a complete phone number."
 * (`monarch_phoneinput_invalid`).
 * @property country the strings of the country picker, a `MonarchPhoneCountrySelect`: its label "Country code",
 * its headings and its search strings.
 */
@Immutable
public class MonarchPhoneInputLabels(
    public val invalid: String? = null,
    public val country: MonarchPhoneCountrySelectLabels = MonarchPhoneCountrySelectLabels(),
)
