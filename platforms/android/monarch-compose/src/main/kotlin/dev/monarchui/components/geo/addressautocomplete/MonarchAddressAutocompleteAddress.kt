package dev.monarchui.components.geo.addressautocomplete

import androidx.compose.runtime.Immutable

/**
 * A postal address, as a `MonarchAddressAutocomplete` fills and edits it.
 *
 * @property line1 the first street line: number and street.
 * @property line2 the second street line: apartment, suite, floor.
 * @property city the city or town.
 * @property region the state, province or region, as typed or as the provider gives it.
 * @property postalCode the postal or ZIP code.
 * @property countryCode the ISO 3166-1 alpha-2 code of the country, or `null`.
 */
@Immutable
public data class MonarchAddressAutocompleteAddress(
    public val line1: String = "",
    public val line2: String = "",
    public val city: String = "",
    public val region: String = "",
    public val postalCode: String = "",
    public val countryCode: String? = null,
) {
    /** Whether every part is blank and no country is set. */
    public val isEmpty: Boolean
        get() = listOf(line1, line2, city, region, postalCode).all { it.isBlank() } && countryCode.isNullOrBlank()
}
