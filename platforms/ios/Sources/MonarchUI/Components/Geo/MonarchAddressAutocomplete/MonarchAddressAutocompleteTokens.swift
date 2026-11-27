import Foundation
import MonarchTokens
import SwiftUI

/// What the suggestion list of a ``MonarchAddressAutocomplete`` shows.
enum MonarchAddressAutocompleteContent: Equatable {
    /// Nothing typed yet: only the manual entry row.
    case idle
    /// "Type at least N characters to search."
    case tooShort
    /// A progress row.
    case searching
    /// The error with a retry button.
    case failed
    /// "No addresses found".
    case empty
    /// Suggestions, by id.
    case suggestions([String])
}

/// Component tokens and pure logic of ``MonarchAddressAutocomplete``.
enum MonarchAddressAutocompleteTokens {
    /// The gap between the fields of the form.
    static let fieldSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between the parts of a row or of the field.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The inner padding of the suggestion panel.
    static let panelPadding: CGFloat = MonarchTokens.Space.space3
    /// The SF Symbol in the search field.
    static let searchSymbol = "magnifyingglass"
    /// The SF Symbol before a suggestion.
    static let suggestionSymbol = "mappin.and.ellipse"
    /// The SF Symbol of the manual entry row.
    static let manualSymbol = "square.and.pencil"
    /// The default number of characters before searching.
    static let defaultMinQueryLength = 3
    /// The default wait after typing.
    static let defaultDebounce: Duration = .milliseconds(300)

    /// The mode to start in: the form when the address already has a street line.
    static func initialMode(_ address: MonarchAddressAutocompleteAddress) -> MonarchAddressAutocompleteMode {
        address.line1.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? .search : .manual
    }

    /// What the list shows for a search phase.
    static func content(
        phase: MonarchDebouncedSearchPhase,
        results: [MonarchAddressAutocompleteSuggestion]
    ) -> MonarchAddressAutocompleteContent {
        switch phase {
        case .idle: .idle
        case .tooShort: .tooShort
        case .loading: .searching
        case .failed: .failed
        case .done: results.isEmpty ? .empty : .suggestions(results.map(\.id))
        }
    }

    /// The message of a content state that has one.
    static func message(
        _ content: MonarchAddressAutocompleteContent,
        minQueryLength: Int,
        labels: MonarchAddressAutocompleteLabels
    ) -> String? {
        switch content {
        case .idle, .suggestions: nil
        case .tooShort: labels.tooShort?(minQueryLength) ?? MonarchAddressAutocompleteStrings.tooShort(minQueryLength)
        case .searching: labels.searching ?? MonarchAddressAutocompleteStrings.searching
        case .failed: labels.searchFailed ?? MonarchAddressAutocompleteStrings.searchFailed
        case .empty: labels.empty ?? MonarchAddressAutocompleteStrings.empty
        }
    }

    /// What is announced when suggestions arrive, or `nil` for other states.
    static func announcement(_ content: MonarchAddressAutocompleteContent, labels: MonarchAddressAutocompleteLabels)
        -> String?
    {
        switch content {
        case .suggestions(let ids): labels.results?(ids.count) ?? MonarchAddressAutocompleteStrings.results(ids.count)
        case .empty: labels.empty ?? MonarchAddressAutocompleteStrings.empty
        default: nil
        }
    }

    /// What VoiceOver reads for a suggestion: the label, then the description.
    static func accessibilityLabel(_ suggestion: MonarchAddressAutocompleteSuggestion) -> String {
        [suggestion.label, suggestion.description ?? ""].filter { !$0.isEmpty }.joined(separator: ", ")
    }

    /// The address on one line, for the search field and VoiceOver: the
    /// non-empty parts and the country's name in `locale`.
    static func summary(_ address: MonarchAddressAutocompleteAddress, locale: Locale) -> String? {
        let country = address.countryCode.map { countryName($0, locale: locale) }
        let parts = [address.line1, address.line2, address.city, address.region, address.postalCode, country ?? ""]
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        return parts.isEmpty ? nil : parts.joined(separator: ", ")
    }

    /// The name of a country in `locale`, or the code when it has none.
    static func countryName(_ code: String, locale: Locale) -> String {
        locale.localizedString(forRegionCode: code) ?? code
    }

    /// Every country and territory as a combobox option, named and sorted in
    /// `locale`, with the code as the hint.
    static func countries(locale: Locale) -> [MonarchComboboxOption] {
        Locale.Region.isoRegions
            .map(\.identifier)
            .filter { $0.count == 2 && $0.allSatisfy(\.isLetter) }
            .map { MonarchComboboxOption(id: $0, label: countryName($0, locale: locale), hint: $0) }
            .sorted {
                $0.label.compare($1.label, options: [.caseInsensitive], range: nil, locale: locale) == .orderedAscending
            }
    }

    /// The street line to start the form with when someone leaves the
    /// search for manual entry: the typed text, unless a street is already there.
    static func manualLine1(current: String, query: String) -> String {
        let typed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        return current.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? typed : current
    }

    /// The help text of an optional field.
    static func optionalText(labels: MonarchAddressAutocompleteLabels) -> String {
        labels.optional ?? MonarchAddressAutocompleteStrings.optional
    }

    /// The colour role of the search field's text: the address, or the placeholder.
    static func valueColor(hasAddress: Bool, isEnabled: Bool) -> MonarchColorRole {
        guard isEnabled else { return \.textDisabled }
        return hasAddress ? \.textDefault : \.textSubtle
    }
}
