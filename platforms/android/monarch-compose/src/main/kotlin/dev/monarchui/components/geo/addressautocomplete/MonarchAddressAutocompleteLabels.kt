package dev.monarchui.components.geo.addressautocomplete

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAddressAutocomplete`.
 * Every `null` keeps the built-in string `monarch_addressautocomplete_<key>`.
 *
 * @property label the label of the search field and the title of the sheet: "Address".
 * @property placeholder shown in the empty search field and the sheet's search field: "Start typing an address".
 * @property searching shown while suggestions load: "Searching…".
 * @property resolving shown while the details of the picked address load: "Getting the address details…".
 * @property empty shown when the search finds nothing: "No addresses found".
 * @property searchFailed shown when the search fails, with a retry button: "Couldn’t search addresses.".
 * @property resolveFailed shown when the details of the picked address could not be loaded: "Couldn’t get that
 * address. Enter it manually or try another.".
 * @property enterManually the button and list row that open the form: "Enter the address manually".
 * @property backToSearch the button that closes the form: "Search for an address".
 * @property filled announced after a suggestion fills in the form: "Address filled in. Check the details.".
 * @property cancel the button that closes the sheet: "Cancel".
 * @property line1 the label of the first street line: "Street address".
 * @property line2 the label of the second street line: "Apartment, suite, etc.".
 * @property city the label of the city field: "City".
 * @property region the label of the region field: "State or region".
 * @property postalCode the label of the postal code field: "Postal code".
 * @property country the label of the country field: "Country".
 * @property optional the helper text of the optional second street line: "Optional".
 * @property tooShort shown until the search text is long enough, from the length needed: "Type at least 3
 * characters to search.".
 * @property results shown and announced with the number of suggestions: "1 address found", "3 addresses found".
 */
@Immutable
public class MonarchAddressAutocompleteLabels(
    public val label: String? = null,
    public val placeholder: String? = null,
    public val searching: String? = null,
    public val resolving: String? = null,
    public val empty: String? = null,
    public val searchFailed: String? = null,
    public val resolveFailed: String? = null,
    public val enterManually: String? = null,
    public val backToSearch: String? = null,
    public val filled: String? = null,
    public val cancel: String? = null,
    public val line1: String? = null,
    public val line2: String? = null,
    public val city: String? = null,
    public val region: String? = null,
    public val postalCode: String? = null,
    public val country: String? = null,
    public val optional: String? = null,
    public val tooShort: ((length: Int) -> String)? = null,
    public val results: ((count: Int) -> String)? = null,
)
