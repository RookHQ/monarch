/// Where a ``MonarchAddressAutocomplete`` is in its flow.
@_spi(Experimental) public enum MonarchAddressAutocompleteMode: Sendable, Hashable, CaseIterable {
    /// Only the search field.
    case search
    /// The address fields, filled in after a pick for review, or typed by hand.
    case manual
}
