import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchComboboxFixtures: ComponentFixtures {
    static let id = "combobox"

    static let countries = [
        MonarchComboboxOption(id: "fr", label: "France", hint: "FR", group: "Europe", keywords: "République"),
        MonarchComboboxOption(id: "de", label: "Germany", hint: "DE", group: "Europe", keywords: "Deutschland"),
        MonarchComboboxOption(id: "in", label: "India", hint: "IN", group: "Asia"),
        MonarchComboboxOption(id: "jp", label: "Japan", hint: "JP", group: "Asia", isDisabled: true),
        MonarchComboboxOption(id: "nz", label: "New Zealand", description: "Aotearoa", hint: "NZ"),
    ]

    static let fixtures = [
        Fixture("chosen, with help text") {
            MonarchCombobox("Country", selection: .constant("in"), options: countries, helper: "Where you pay tax.")
        },
        Fixture("read only") {
            MonarchCombobox("Country", selection: .constant("in"), options: countries, isReadOnly: true)
        },
        Fixture("empty with prompt") {
            MonarchCombobox("Country", selection: .constant(nil), options: countries, prompt: "Choose a country")
        },
        Fixture("required with error") {
            MonarchCombobox(
                "Country",
                selection: .constant(nil),
                options: countries,
                prompt: "Choose a country",
                error: "Choose a country.",
                isRequired: true
            )
        },
        Fixture("value not in the options, not clearable") {
            MonarchCombobox("Owner", selection: .constant("user-42"), options: [], isClearable: false)
        },
        Fixture("long label") {
            MonarchCombobox(
                "Assignee",
                selection: .constant("a"),
                options: [
                    MonarchComboboxOption(
                        id: "a",
                        label: "A person with a very long name that does not fit on one line of the field",
                        systemImage: "person.crop.circle"
                    )
                ]
            )
        },
        Fixture("disabled") {
            MonarchCombobox("Country", selection: .constant("fr"), options: countries).disabled(true)
        },
        Fixture("list: groups, chosen, disabled, clear option") {
            MonarchComboboxList(
                title: "Country",
                selection: "in",
                options: countries,
                query: .constant(""),
                clearOptionTitle: "No country",
                isLoading: false,
                sortsByMatch: false,
                limit: nil,
                labels: MonarchComboboxLabels(),
                filter: nil,
                canCreate: false,
                onChoose: { _ in },
                onCreate: { _ in },
                onCancel: {}
            )
        },
        Fixture("list: no match with create, limited") {
            MonarchComboboxList(
                title: "Tag",
                selection: nil,
                options: countries,
                query: .constant("Zz"),
                clearOptionTitle: nil,
                isLoading: false,
                sortsByMatch: true,
                limit: 1,
                labels: MonarchComboboxLabels(),
                filter: nil,
                canCreate: true,
                onChoose: { _ in },
                onCreate: { _ in },
                onCancel: {}
            )
        },
        Fixture("list: loading and empty") {
            MonarchComboboxList(
                title: "Customer",
                selection: nil,
                options: [],
                query: .constant("acme"),
                clearOptionTitle: nil,
                isLoading: true,
                sortsByMatch: false,
                limit: nil,
                labels: MonarchComboboxLabels(loading: "Searching…"),
                filter: { _, _ in true },
                canCreate: false,
                onChoose: { _ in },
                onCreate: { _ in },
                onCancel: {}
            )
        },
        Fixture("list: failed to load, with retry; leading text and endonyms") {
            VStack {
                MonarchComboboxList(
                    title: "Customer",
                    selection: nil,
                    options: [],
                    query: .constant(""),
                    clearOptionTitle: nil,
                    isLoading: false,
                    loadError: "The server did not answer.",
                    onRetry: {},
                    sortsByMatch: false,
                    limit: nil,
                    labels: MonarchComboboxLabels(),
                    filter: nil,
                    canCreate: false,
                    onChoose: { _ in },
                    onCreate: { _ in },
                    onCancel: {}
                )
                MonarchComboboxList(
                    title: "Language",
                    selection: "de",
                    options: [
                        MonarchComboboxOption(
                            id: "de",
                            label: "Deutsch",
                            hint: "German",
                            leadingText: "🇩🇪",
                            labelLanguage: Locale.Language(identifier: "de")
                        ),
                        MonarchComboboxOption(
                            id: "ja",
                            label: "日本語",
                            hint: "Japanese",
                            leadingText: "🇯🇵",
                            labelLanguage: Locale.Language(identifier: "ja")
                        ),
                    ],
                    query: .constant(""),
                    clearOptionTitle: nil,
                    isLoading: false,
                    sortsByMatch: false,
                    limit: nil,
                    labels: MonarchComboboxLabels(),
                    filter: nil,
                    canCreate: false,
                    onChoose: { _ in },
                    onCreate: { _ in },
                    onCancel: {}
                )
            }
        },
    ]
}

@Suite("MonarchCombobox")
struct MonarchComboboxTests {
    private typealias Tokens = MonarchComboboxTokens
    private let countries = MonarchComboboxFixtures.countries

    @Test("The default filter searches the label, hint and keywords, ignoring case and accents")
    func matching() {
        let france = countries[0]
        #expect(Tokens.matches(france, query: "fran"))
        #expect(Tokens.matches(france, query: "FR"))
        #expect(Tokens.matches(france, query: "republique"))
        #expect(Tokens.matches(france, query: "  "))
        #expect(!Tokens.matches(france, query: "Aotearoa"))
        // The description is shown but not searched, as on the web.
        #expect(!Tokens.matches(countries[4], query: "Aotearoa"))
    }

    @Test("Ranking puts label starts first, then word starts")
    func ranking() {
        let zealand = countries[4]
        #expect(Tokens.rank(zealand, query: "new") == 0)
        #expect(Tokens.rank(zealand, query: "zea") == 1)
        #expect(Tokens.rank(zealand, query: "eal") == 2)
    }

    @Test("Results keep ungrouped options first, then groups in order of appearance")
    func grouping() {
        let results = Tokens.results(countries, query: "", filter: nil, sortsByMatch: false, limit: nil)
        #expect(results.sections.map(\.title) == [nil, "Europe", "Asia"])
        #expect(results.sections[0].options.map(\.id) == ["nz"])
        #expect(results.sections[1].options.map(\.id) == ["fr", "de"])
        #expect(results.shown == 5)
        #expect(results.total == 5)
        #expect(!results.isTruncated)
    }

    @Test("Sorting by match is stable and only with a query")
    func sorting() {
        let options = ["Oka", "Alaska", "Kalamata", "Port-Kar"].map { MonarchComboboxOption(id: $0, label: $0) }
        let sorted = Tokens.results(options, query: "ka", filter: nil, sortsByMatch: true, limit: nil)
        #expect(sorted.sections.flatMap(\.options).map(\.id) == ["Kalamata", "Port-Kar", "Oka", "Alaska"])
        let unsorted = Tokens.results(options, query: "", filter: nil, sortsByMatch: true, limit: nil)
        #expect(unsorted.sections.flatMap(\.options).map(\.id) == ["Oka", "Alaska", "Kalamata", "Port-Kar"])
    }

    @Test("A limit cuts the matches and reports how many there are")
    func limit() {
        let results = Tokens.results(countries, query: "a", filter: nil, sortsByMatch: false, limit: 2)
        #expect(results.shown == 2)
        #expect(results.total == 5)
        #expect(results.isTruncated)
        let none = Tokens.results(countries, query: "qqq", filter: nil, sortsByMatch: false, limit: nil)
        #expect(none.isEmpty)
        #expect(none.sections.isEmpty)
    }

    @Test("A custom filter replaces the default one")
    func customFilter() {
        let results = Tokens.results(countries, query: "zzz", filter: { _, _ in true }, sortsByMatch: false, limit: nil)
        #expect(results.total == countries.count)
    }

    @Test("Create is offered for new, non-blank text when there is a handler")
    func create() {
        #expect(Tokens.showsCreate(query: "Peru", options: countries, canCreate: true))
        #expect(!Tokens.showsCreate(query: "india ", options: countries, canCreate: true))
        #expect(!Tokens.showsCreate(query: "  ", options: countries, canCreate: true))
        #expect(!Tokens.showsCreate(query: "Peru", options: countries, canCreate: false))
    }

    @Test("The field shows the option's label, a remembered label or the raw value")
    func selectedLabel() {
        #expect(Tokens.selectedLabel("in", options: countries, remembered: [:]) == "India")
        #expect(Tokens.selectedLabel("pe", options: countries, remembered: ["pe": "Peru"]) == "Peru")
        #expect(Tokens.selectedLabel("xx", options: countries, remembered: [:]) == "xx")
        #expect(Tokens.selectedLabel(nil, options: countries, remembered: [:]) == nil)
    }

    @Test("VoiceOver reads the value, or None, and the option's parts")
    func accessibility() {
        #expect(Tokens.accessibilityValue(selectedLabel: "India", labels: MonarchComboboxLabels()) == "India")
        #expect(Tokens.accessibilityValue(selectedLabel: nil, labels: MonarchComboboxLabels()) == "None")
        #expect(
            Tokens.accessibilityValue(selectedLabel: nil, labels: MonarchComboboxLabels(noSelection: "Nobody"))
                == "Nobody"
        )
        #expect(Tokens.accessibilityLabel(countries[4]) == "New Zealand, Aotearoa, NZ")
        #expect(Tokens.accessibilityLabel(countries[2]) == "India, IN")
    }

    @Test("Colours for the value, the prompt and disabled options")
    func colors() {
        #expect(Tokens.valueColor(hasSelection: true, isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(Tokens.valueColor(hasSelection: false, isEnabled: true) == \MonarchColorTokens.textSubtle)
        #expect(Tokens.valueColor(hasSelection: true, isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(Tokens.optionColor(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(Tokens.secondaryColor(isEnabled: true) == \MonarchColorTokens.textMuted)
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchComboboxStrings.search == "Search")
        #expect(MonarchComboboxStrings.loading == "Loading…")
        #expect(MonarchComboboxStrings.empty == "No matches")
        #expect(MonarchComboboxStrings.create("Peru") == "Create “Peru”")
        #expect(MonarchComboboxStrings.more(shown: 50, total: 120) == "Showing 50 of 120. Type to narrow the list.")
        #expect(MonarchComboboxStrings.cancel == "Cancel")
        #expect(MonarchComboboxStrings.clear == "Clear")
        #expect(MonarchComboboxStrings.noSelection == "None")
    }
}

@Suite("MonarchCombobox options and loading")
struct MonarchComboboxLeadingTests {
    @Test("An endonym is marked with its language for VoiceOver; the rest is not")
    func spokenLanguage() {
        let option = MonarchComboboxOption(
            id: "de",
            label: "Deutsch",
            hint: "German",
            leadingText: "🇩🇪",
            labelLanguage: Locale.Language(identifier: "de")
        )
        let spoken = MonarchComboboxTokens.spokenLabel(option)
        #expect(String(spoken.characters) == "Deutsch, German")
        let runs = spoken.runs.map { $0.languageIdentifier }
        #expect(runs.first == "de-Latn-DE")
        #expect(runs.last == .some(nil))
        #expect(MonarchComboboxTokens.accessibilityLabel(option) == "Deutsch, German", "the flag is not read")
    }

    @Test("Without a language the spoken label is plain")
    func plain() {
        let option = MonarchComboboxOption(id: "fr", label: "France")
        #expect(MonarchComboboxTokens.spokenLabel(option).runs.allSatisfy { $0.languageIdentifier == nil })
    }

    @Test("A load failure shows the app's message, else the built-in one")
    func loadFailed() {
        #expect(MonarchComboboxTokens.loadFailedMessage("Offline", labels: MonarchComboboxLabels()) == "Offline")
        let builtIn = MonarchComboboxTokens.loadFailedMessage("", labels: MonarchComboboxLabels())
        #expect(builtIn == "Couldn’t load the options.")
        let custom = MonarchComboboxTokens.loadFailedMessage("", labels: MonarchComboboxLabels(loadFailed: "Nope"))
        #expect(custom == "Nope")
    }
}
