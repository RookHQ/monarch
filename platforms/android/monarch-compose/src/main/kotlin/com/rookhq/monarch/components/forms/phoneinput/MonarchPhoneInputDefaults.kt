package com.rookhq.monarch.components.forms.phoneinput

import com.rookhq.monarch.components.geo.countryselect.CountrySelectData
import com.rookhq.monarch.components.geo.phonecountryselect.PhoneCountrySelectCountry
import com.rookhq.monarch.components.geo.phonecountryselect.PhoneCountrySelectData

/**
 * The phone number rules of `MonarchPhoneInput`, for apps that build or read
 * E.164 numbers themselves. Pure functions over the same ITU-T calling-code
 * table as `MonarchPhoneCountrySelect`; no phone library, so a number is
 * checked by its length only.
 */
public object MonarchPhoneInputDefaults {
    /** Countries whose national numbers keep their leading 0 after the calling code. */
    private val KeepsLeadingZero = setOf("IT", "SM", "VA")

    /** The owner of a calling code shared by several countries, when no leading digits decide. */
    private val MainForCode = mapOf(
        "+1" to "US",
        "+7" to "RU",
        "+44" to "GB",
        "+47" to "NO",
        "+61" to "AU",
        "+212" to "MA",
        "+262" to "RE",
        "+358" to "FI",
        "+39" to "IT",
        "+590" to "GP",
        "+599" to "CW",
    )

    private const val LONGEST_CODE = 4
    private const val INTERNATIONAL_PREFIX = "00"
    private val E164 = Regex("^\\+[1-9]\\d{7,14}$")
    private val Separators = Regex("[\\s().\\-]")

    /**
     * The ASCII digits of [text], whatever digits were typed: "(020) 7946-0958"
     * gives "02079460958", and Arabic-Indic or full-width digits become 0 to 9.
     */
    internal fun digits(text: String): String = ascii(text).filter { it in '0'..'9' }

    /** [text] with every decimal digit of any script written as an ASCII digit. */
    internal fun ascii(text: String): String = buildString {
        text.forEach { char ->
            val digit = Character.digit(char, RADIX)
            append(if (digit >= 0 && Character.isDigit(char)) '0' + digit else char)
        }
    }

    /**
     * Joins a calling code and a national number into E.164: ("+44",
     * "020 7946 0958") gives "+442079460958". Separators are removed and one
     * leading trunk 0 is dropped, except for Italy, San Marino and Vatican City
     * when [country] says so. Returns `null` when [national] has no digits.
     *
     * @param dialCode the calling code, with or without the plus.
     * @param national the national number as typed.
     * @param country the ISO code of the country, which decides whether the leading 0 is kept.
     */
    public fun toE164(dialCode: String, national: String, country: String? = null): String? {
        var number = digits(national)
        if (number.isEmpty()) return null
        val code = country?.let(CountrySelectData::canonical)
        if (number.startsWith("0") && code !in KeepsLeadingZero) number = number.drop(1)
        return "+" + digits(dialCode) + number
    }

    /** Whether [value] is a plausible E.164 number: a plus, then 8 to 15 digits that do not start with 0. */
    public fun isValidE164(value: String): Boolean = E164.matches(value)

    /**
     * Splits an international number ("+44 20 7946 0958", "0044 20…" or E.164)
     * into its country, calling code and national digits. Countries that share
     * a calling code (+1, +7, +44) are told apart by their leading digits, then
     * by [preferred], then by the code's main country. Returns `null` when the
     * number does not start with + or 00, or no calling code matches.
     *
     * @param phone the number in any international form.
     * @param preferred the ISO code to choose among countries that share the calling code, such as "CA" for +1.
     */
    public fun parse(phone: String, preferred: String? = null): MonarchPhoneInputParts? {
        val rest = internationalDigits(phone) ?: return null
        for (length in minOf(LONGEST_CODE, rest.length) downTo 1) {
            val dialCode = "+" + rest.take(length)
            val candidates = PhoneCountrySelectData.COUNTRIES.filter { it.dialCode == dialCode }
            if (candidates.isEmpty()) continue
            val national = rest.drop(length)
            val country = owner(candidates, national, preferred?.let(CountrySelectData::canonical), dialCode)
            return MonarchPhoneInputParts(country.code, dialCode, national)
        }
        return null
    }

    /** The digits after + or 00, or `null` when [phone] is not written internationally. */
    private fun internationalDigits(phone: String): String? {
        val compact = ascii(phone).replace(Separators, "")
        val rest = when {
            compact.startsWith("+") -> compact.drop(1)
            compact.startsWith(INTERNATIONAL_PREFIX) -> compact.drop(INTERNATIONAL_PREFIX.length)
            else -> return null
        }
        return rest.takeIf { it.isNotEmpty() && it.all { char -> char in '0'..'9' } }
    }

    private fun owner(
        candidates: List<PhoneCountrySelectCountry>,
        national: String,
        preferred: String?,
        dialCode: String,
    ): PhoneCountrySelectCountry =
        candidates.firstOrNull { country -> country.leadingDigits.any { national.startsWith(it) } }
            ?: candidates.firstOrNull { it.code == preferred && it.leadingDigits.isEmpty() }
            ?: candidates.firstOrNull { it.code == MainForCode[dialCode] }
            ?: candidates.firstOrNull { it.leadingDigits.isEmpty() }
            ?: candidates.first()

    private const val RADIX = 10
}
