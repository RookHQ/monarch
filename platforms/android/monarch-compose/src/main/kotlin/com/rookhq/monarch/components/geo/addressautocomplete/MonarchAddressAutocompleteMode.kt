package com.rookhq.monarch.components.geo.addressautocomplete

/** What a `MonarchAddressAutocomplete` shows under its search field. */
public enum class MonarchAddressAutocompleteMode {
    /** The search field only, with "Enter the address manually" under it. */
    Search,

    /** The search field and the form of the address parts, for review or manual entry. */
    Manual,
}
