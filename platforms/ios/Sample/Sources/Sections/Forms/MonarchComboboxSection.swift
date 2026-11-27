import MonarchUI
import SwiftUI

struct MonarchComboboxSection: CataloguePage {
    static let title = "Combobox"

    private static let countries = [
        MonarchComboboxOption(id: "fr", label: "France", hint: "FR", group: "Europe", keywords: "République"),
        MonarchComboboxOption(id: "de", label: "Germany", hint: "DE", group: "Europe", keywords: "Deutschland"),
        MonarchComboboxOption(id: "es", label: "Spain", hint: "ES", group: "Europe", keywords: "España"),
        MonarchComboboxOption(id: "in", label: "India", hint: "IN", group: "Asia", keywords: "Bharat"),
        MonarchComboboxOption(id: "jp", label: "Japan", hint: "JP", group: "Asia", keywords: "Nippon"),
        MonarchComboboxOption(id: "kr", label: "South Korea", hint: "KR", group: "Asia", isDisabled: true),
        MonarchComboboxOption(id: "us", label: "United States", hint: "US", group: "Americas"),
        MonarchComboboxOption(id: "br", label: "Brazil", hint: "BR", group: "Americas", keywords: "Brasil"),
    ]

    private static let languages = [
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
        MonarchComboboxOption(
            id: "pt",
            label: "Português",
            hint: "Portuguese",
            leadingText: "🇵🇹",
            labelLanguage: Locale.Language(identifier: "pt")
        ),
    ]

    @State private var country: String?
    @State private var assignee: String? = "sam"
    @State private var tag: String?
    @State private var language: String?
    @State private var languageError: String? = "The server did not answer."
    @State private var tags = [
        MonarchComboboxOption(id: "design", label: "Design"),
        MonarchComboboxOption(id: "research", label: "Research"),
    ]

    var body: some View {
        CatalogueCaption("Grouped, sorted by match, required")
        MonarchCombobox(
            "Country",
            selection: $country,
            options: Self.countries,
            prompt: "Choose a country",
            error: country == nil ? "Choose a country." : nil,
            isRequired: true,
            sortsByMatch: true
        )

        CatalogueCaption("With icons, descriptions and a clear option")
        MonarchCombobox(
            "Assignee",
            selection: $assignee,
            options: [
                MonarchComboboxOption(
                    id: "sam",
                    label: "Sam Lee",
                    description: "Design",
                    hint: "sam@example.com",
                    systemImage: "person.crop.circle"
                ),
                MonarchComboboxOption(
                    id: "ada",
                    label: "Ada Byron",
                    description: "Engineering",
                    hint: "ada@example.com",
                    systemImage: "person.crop.circle"
                ),
            ],
            helper: "Who works on this next.",
            clearOptionTitle: "Unassigned"
        )

        CatalogueCaption("Create a new option")
        MonarchCombobox(
            "Tag",
            selection: $tag,
            options: tags,
            prompt: "Choose or create a tag",
            onCreate: { text in
                let option = MonarchComboboxOption(id: text.lowercased(), label: text)
                tags.append(option)
                return option
            }
        )

        CatalogueCaption("Endonyms with flags; the list failed to load until Try again")
        MonarchCombobox(
            "Language",
            selection: $language,
            options: languageError == nil ? Self.languages : [],
            loadError: languageError,
            onRetry: { languageError = nil }
        )

        CatalogueCaption("Disabled")
        MonarchCombobox("Country", selection: .constant("fr"), options: Self.countries).disabled(true)
    }
}
