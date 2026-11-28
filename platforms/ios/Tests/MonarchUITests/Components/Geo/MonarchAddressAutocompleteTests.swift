import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchAddressAutocompleteFixtures: ComponentFixtures {
    static let id = "address-autocomplete"

    static let filled = MonarchAddressAutocompleteAddress(
        line1: "10 Market Street",
        line2: "Floor 3",
        city: "Lisbon",
        region: "Lisboa",
        postalCode: "1100-148",
        countryCode: "PT"
    )

    static let suggestions = [
        MonarchAddressAutocompleteSuggestion(id: "1", label: "10 Market Street", description: "Lisbon, Portugal"),
        MonarchAddressAutocompleteSuggestion(id: "2", label: "10 Market Square", description: "Porto, Portugal"),
    ]

    @MainActor
    static func sheet(query: String, minQueryLength: Int = 3) -> MonarchAddressAutocompleteSuggestionPanel {
        MonarchAddressAutocompleteSuggestionPanel(
            query: query,
            minQueryLength: minQueryLength,
            debounce: .zero,
            allowsManualEntry: true,
            labels: MonarchAddressAutocompleteLabels(),
            search: { _ in suggestions },
            onPick: { _ in },
            onManual: {}
        )
    }

    static let fixtures = [
        Fixture("search, empty") {
            MonarchAddressAutocomplete(address: .constant(MonarchAddressAutocompleteAddress()), search: { _ in [] })
        },
        Fixture("search, with help text and required") {
            MonarchAddressAutocomplete(
                "Shipping address",
                address: .constant(MonarchAddressAutocompleteAddress()),
                mode: .constant(.search),
                helper: "Where we send the parcel.",
                isRequired: true,
                search: { _ in [] }
            )
        },
        Fixture("search, with error, no manual entry") {
            MonarchAddressAutocomplete(
                address: .constant(MonarchAddressAutocompleteAddress()),
                error: "Enter an address.",
                allowsManualEntry: false,
                search: { _ in [] }
            )
        },
        Fixture("filled form for review") {
            MonarchAddressAutocomplete(address: .constant(filled), search: { _ in [] })
        },
        Fixture("form with field errors and long text") {
            MonarchAddressAutocomplete(
                address: .constant(
                    MonarchAddressAutocompleteAddress(
                        line1: "Apartment block C, 1234 Very Long Avenue of the Discoverers and Navigators",
                        city: "Vila Nova de Gaia"
                    )
                ),
                mode: .constant(.manual),
                fieldErrors: [.postalCode: "Enter a postal code.", .country: "Choose a country."],
                isRequired: true,
                search: { _ in [] }
            )
        },
        Fixture("disabled") {
            MonarchAddressAutocomplete(address: .constant(filled), mode: .constant(.search), search: { _ in [] })
                .disabled(true)
        },
        Fixture("panel: nothing typed") { sheet(query: "") },
        Fixture("panel: too short") { sheet(query: "10") },
    ]
}

private struct MonarchAddressAutocompleteTestError: Error {}

@Suite("MonarchAddressAutocomplete")
@MainActor
struct MonarchAddressAutocompleteTests {
    private typealias Tokens = MonarchAddressAutocompleteTokens
    private let english = Locale(identifier: "en_US")

    @Test("It starts on the form only when the address has a street line")
    func initialMode() {
        #expect(Tokens.initialMode(MonarchAddressAutocompleteAddress()) == .search)
        #expect(Tokens.initialMode(MonarchAddressAutocompleteAddress(line1: "  ", city: "Lisbon")) == .search)
        #expect(Tokens.initialMode(MonarchAddressAutocompleteFixtures.filled) == .manual)
    }

    @Test("The list content follows the search phase")
    func content() {
        let suggestions = MonarchAddressAutocompleteFixtures.suggestions
        #expect(Tokens.content(phase: .idle, results: []) == .idle)
        #expect(Tokens.content(phase: .tooShort, results: []) == .tooShort)
        #expect(Tokens.content(phase: .loading, results: suggestions) == .searching)
        #expect(Tokens.content(phase: .failed, results: []) == .failed)
        #expect(Tokens.content(phase: .done, results: []) == .empty)
        #expect(Tokens.content(phase: .done, results: suggestions) == .suggestions(["1", "2"]))
    }

    @Test("Messages and announcements, built in and replaced")
    func messages() {
        let labels = MonarchAddressAutocompleteLabels()
        #expect(Tokens.message(.idle, minQueryLength: 3, labels: labels) == nil)
        #expect(Tokens.message(.tooShort, minQueryLength: 3, labels: labels) == "Type at least 3 characters to search.")
        #expect(Tokens.message(.searching, minQueryLength: 3, labels: labels) == "Searching…")
        #expect(Tokens.message(.failed, minQueryLength: 3, labels: labels) == "Couldn’t search addresses.")
        #expect(Tokens.message(.empty, minQueryLength: 3, labels: labels) == "No addresses found")
        #expect(Tokens.announcement(.suggestions(["1"]), labels: labels) == "1 address found")
        #expect(Tokens.announcement(.suggestions(["1", "2", "3"]), labels: labels) == "3 addresses found")
        #expect(Tokens.announcement(.empty, labels: labels) == "No addresses found")
        #expect(Tokens.announcement(.searching, labels: labels) == nil)
        let custom = MonarchAddressAutocompleteLabels(tooShort: { "Min \($0)" }, results: { "\($0) found" })
        #expect(Tokens.message(.tooShort, minQueryLength: 4, labels: custom) == "Min 4")
        #expect(Tokens.announcement(.suggestions(["1", "2"]), labels: custom) == "2 found")
    }

    @Test("The one-line summary skips empty parts and names the country in the locale")
    func summary() {
        let filled = MonarchAddressAutocompleteFixtures.filled
        #expect(
            Tokens.summary(filled, locale: english) == "10 Market Street, Floor 3, Lisbon, Lisboa, 1100-148, Portugal"
        )
        #expect(Tokens.summary(MonarchAddressAutocompleteAddress(), locale: english) == nil)
        #expect(
            Tokens.summary(
                MonarchAddressAutocompleteAddress(city: "Berlin", countryCode: "DE"),
                locale: Locale(identifier: "de_DE")
            )
                == "Berlin, Deutschland"
        )
    }

    @Test("Countries are two-letter regions, named and sorted in the locale")
    func countries() {
        let countries = Tokens.countries(locale: english)
        #expect(countries.count > 200)
        #expect(countries.allSatisfy { $0.id.count == 2 })
        #expect(countries.first { $0.id == "PT" }?.label == "Portugal")
        #expect(countries.first { $0.id == "PT" }?.hint == "PT")
        let labels = countries.map(\.label)
        #expect(labels.firstIndex(of: "Austria")! < labels.firstIndex(of: "Brazil")!)
        #expect(!countries.contains { $0.id == "001" })
    }

    @Test("Manual entry starts with the typed text unless a street is there")
    func manualLine1() {
        #expect(Tokens.manualLine1(current: "", query: " 10 Market ") == "10 Market")
        #expect(Tokens.manualLine1(current: "5 Rua Augusta", query: "10 Market") == "5 Rua Augusta")
    }

    @Test("VoiceOver reads a suggestion's label and description")
    func accessibility() {
        let suggestions = MonarchAddressAutocompleteFixtures.suggestions
        #expect(Tokens.accessibilityLabel(suggestions[0]) == "10 Market Street, Lisbon, Portugal")
        #expect(
            Tokens.accessibilityLabel(MonarchAddressAutocompleteSuggestion(id: "x", label: "Somewhere")) == "Somewhere"
        )
        #expect(Tokens.valueColor(hasAddress: false, isEnabled: true) == \MonarchColorTokens.textSubtle)
        #expect(Tokens.valueColor(hasAddress: true, isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(Tokens.valueColor(hasAddress: true, isEnabled: false) == \MonarchColorTokens.textDisabled)
    }

    @Test("The address is empty only when every part is")
    func isEmpty() {
        #expect(MonarchAddressAutocompleteAddress().isEmpty)
        #expect(MonarchAddressAutocompleteAddress(line1: " ").isEmpty)
        #expect(!MonarchAddressAutocompleteAddress(countryCode: "PT").isEmpty)
    }

    @Test("The provider's search runs through the shared debounced search")
    func search() async {
        let search = MonarchDebouncedSearch<MonarchAddressAutocompleteSuggestion>()
        await search.run("10 Market", minLength: 3, debounce: .zero) { query in
            MonarchAddressAutocompleteFixtures.suggestions.filter { $0.label.hasPrefix(query) }
        }
        #expect(Tokens.content(phase: search.phase, results: search.results) == .suggestions(["1", "2"]))
        await search.run("10 Market", minLength: 3, debounce: .zero) { _ in throw MonarchAddressAutocompleteTestError()
        }
        #expect(Tokens.content(phase: search.phase, results: search.results) == .failed)
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchAddressAutocompleteStrings.label == "Address")
        #expect(MonarchAddressAutocompleteStrings.placeholder == "Start typing an address")
        #expect(
            MonarchAddressAutocompleteStrings.resolveFailed
                == "Couldn’t get that address. Enter it manually or try another."
        )
        #expect(MonarchAddressAutocompleteStrings.enterManually == "Enter the address manually")
        #expect(MonarchAddressAutocompleteStrings.backToSearch == "Search for an address")
        #expect(MonarchAddressAutocompleteStrings.filled == "Address filled in. Check the details.")
        #expect(MonarchAddressAutocompleteStrings.line1 == "Street address")
        #expect(MonarchAddressAutocompleteStrings.country == "Country")
        #expect(MonarchAddressAutocompleteStrings.noAddress == "None")
        #expect(MonarchAddressAutocompleteStrings.optional == "Optional")
    }
}
