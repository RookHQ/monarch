package dev.monarchui.components.forms.phoneinput

import androidx.compose.runtime.Immutable

/**
 * A phone number split into its country, calling code and national digits,
 * as returned by [MonarchPhoneInputDefaults.parse].
 *
 * @property country the ISO 3166-1 alpha-2 code of the country, upper case, such as "GB".
 * @property dialCode the calling code with a leading plus, such as "+44".
 * @property national the national digits after the calling code, such as "2079460958".
 */
@Immutable
public data class MonarchPhoneInputParts(
    public val country: String,
    public val dialCode: String,
    public val national: String,
)
