import Foundation
import SwiftUI

/// A searchable picker for an ISO 3166-1 country or territory.
///
/// ```swift
/// MonarchCountrySelect(
///     "Country",
///     selection: $country,
///     prompt: "Choose a country",
///     priority: ["PT", "ES"]
/// )
/// ```
///
/// The value is an upper-case ISO 3166-1 alpha-2 code, such as "PT". Names
/// come from Foundation (`Locale.localizedString(forRegionCode:)`) in the
/// environment's locale, or in `locale`, and are sorted for it; Monarch ships
/// only the list of codes. Each country shows its flag before the name and its code at the end.
/// `priority` countries are listed first under "Suggested" while nothing is
/// typed. Search matches every word typed against the name, the code and,
/// in other languages, the English name. Built on ``MonarchCombobox``.
///
/// Accessibility: the field is the combobox's button, labelled with `label`
/// and valued with the chosen country's name. Each row is read with the
/// name, then the code and the flag emoji as VoiceOver names it.
public struct MonarchCountrySelect: View {
    private let label: String
    @Binding private var selection: String?
    private let prompt: String?
    private let helper: String?
    private let error: String?
    private let countries: [String]?
    private let excluded: [String]
    private let priority: [String]
    private let locale: Locale?
    private let showsFlag: Bool
    private let showsCode: Bool
    private let isRequired: Bool
    private let isClearable: Bool
    private let labels: MonarchCountrySelectLabels
    private let countryName: (@Sendable (String) -> String?)?

    @Environment(\.locale) private var environmentLocale
    @State private var query = ""

    /// A country picker.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - selection: The chosen ISO 3166-1 alpha-2 code, or `nil`. Any case is read; the picker writes upper case.
    ///   - prompt: Shown in the field while nothing is chosen.
    ///   - helper: Help text under the field.
    ///   - error: An error message, shown with an icon and announced.
    ///   - countries: Limits the list to these codes, such as the countries the product ships to. Every country by default.
    ///   - excluded: Codes to leave out.
    ///   - priority: Codes listed first under "Suggested" while nothing is typed.
    ///   - locale: The language of the names and the sort order. The environment's locale by default.
    ///   - showsFlag: Shows each country's flag before its name.
    ///   - showsCode: Shows each country's code at the end of its row.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - isClearable: Shows a clear button in the field while a country is chosen.
    ///   - labels: Replacements for the built-in strings.
    ///   - countryName: Replaces a country's name, for example to follow a product's legal wording. Return `nil` to keep Foundation's name.
    public init(
        _ label: String,
        selection: Binding<String?>,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        countries: [String]? = nil,
        excluded: [String] = [],
        priority: [String] = [],
        locale: Locale? = nil,
        showsFlag: Bool = true,
        showsCode: Bool = true,
        isRequired: Bool = false,
        isClearable: Bool = true,
        labels: MonarchCountrySelectLabels = MonarchCountrySelectLabels(),
        countryName: (@Sendable (String) -> String?)? = nil
    ) {
        self.label = label
        _selection = selection
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.countries = countries
        self.excluded = excluded
        self.priority = priority
        self.locale = locale
        self.showsFlag = showsFlag
        self.showsCode = showsCode
        self.isRequired = isRequired
        self.isClearable = isClearable
        self.labels = labels
        self.countryName = countryName
    }

    /// The 249 ISO 3166-1 alpha-2 codes the picker lists by default.
    public nonisolated static var countryCodes: [String] { MonarchGeoSearch.countryCodes }

    /// The flag emoji of an ISO 3166-1 alpha-2 code ("PT" gives 🇵🇹), or an
    /// empty string when the code is not two letters.
    public nonisolated static func flag(for code: String) -> String { MonarchGeoSearch.flag(code) }

    /// The content of the view.
    public var body: some View {
        let code = MonarchGeoSearch.normalized(selection)
        var comboboxLabels = labels.combobox
        comboboxLabels.empty = comboboxLabels.empty ?? MonarchCountrySelectStrings.empty
        return MonarchCombobox(
            label,
            selection: Binding(get: { code }, set: { selection = $0 }),
            options: MonarchCountrySelectTokens.options(
                countries: countries,
                excluded: excluded,
                priority: priority,
                selection: code,
                query: query,
                locale: locale ?? environmentLocale,
                showsFlag: showsFlag,
                showsCode: showsCode,
                suggested: labels.suggested ?? MonarchCountrySelectStrings.suggested,
                all: labels.all ?? MonarchCountrySelectStrings.all,
                countryName: countryName
            ),
            prompt: prompt,
            helper: helper,
            error: error,
            isRequired: isRequired,
            isClearable: isClearable,
            sortsByMatch: true,
            searchText: $query,
            labels: comboboxLabels,
            filter: { option, query in MonarchGeoSearch.matchesEveryWord(option, query: query) }
        )
    }
}

#if DEBUG
    #Preview("MonarchCountrySelect") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchCountrySelect("Country", selection: .constant("PT"), helper: "Where you pay tax.")
                MonarchCountrySelect(
                    "Ship to",
                    selection: .constant(nil),
                    prompt: "Choose a country",
                    error: "Choose a country.",
                    priority: ["GB", "US"],
                    isRequired: true
                )
                MonarchCountrySelect("Disabled", selection: .constant("DE"), showsFlag: false).disabled(true)
            }
        }
    }
#endif
