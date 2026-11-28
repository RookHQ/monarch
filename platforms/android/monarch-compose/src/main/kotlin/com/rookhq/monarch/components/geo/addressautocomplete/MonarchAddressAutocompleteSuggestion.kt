package com.rookhq.monarch.components.geo.addressautocomplete

import androidx.compose.runtime.Immutable

/**
 * One suggestion of the app's address provider for a `MonarchAddressAutocomplete`.
 *
 * @property id the provider's id, for `resolve`.
 * @property label the first line people read, usually the street address. Already localised.
 * @property description the second line, such as "Lisbon, Portugal".
 * @property address the full address when the provider already has it. `null` asks `resolve` for it after the pick.
 */
@Immutable
public class MonarchAddressAutocompleteSuggestion(
    public val id: String,
    public val label: String,
    public val description: String? = null,
    public val address: MonarchAddressAutocompleteAddress? = null,
)
