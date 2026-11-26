import Foundation
import SwiftUI

/// A searchable picker for the country calling code of a phone number.
///
/// ```swift
/// MonarchPhoneCountrySelect("Country code", selection: $country, preferred: ["PT"])
/// let dialCode = country.flatMap { MonarchPhoneCountry.country(code: $0)?.dialCode }
/// ```
///
/// The value is the country's upper-case ISO 3166-1 alpha-2 code, because
/// several countries share a calling code; read the calling code with
/// ``MonarchPhoneCountry/country(code:)``. Each row is "Portugal (+351)" with
/// the flag at the end; names come from Foundation in the environment's
/// locale or in `locale`, and the calling codes are bundled. Typing matches
/// the name, the ISO code or the digits ("351", "+351"). `preferred`
/// countries are listed first under "Suggested". Clearing is off by default,
/// because a phone number always needs a country. Built on
/// ``MonarchCombobox``.
///
/// Accessibility: the field is the combobox's button, labelled with `label`
/// and valued with the chosen row's text, so the calling code is read too.
/// Each row is read with its text and then the flag emoji as VoiceOver names it.
public struct MonarchPhoneCountrySelect: View {
    private let label: String
    @Binding private var selection: String?
    private let prompt: String?
    private let helper: String?
    private let error: String?
    private let countries: [String]?
    private let preferred: [String]
    private let locale: Locale?
    private let showsFlags: Bool
    private let isRequired: Bool
    private let isClearable: Bool
    private let labels: MonarchPhoneCountrySelectLabels

    @Environment(\.locale) private var environmentLocale

    /// A calling-code picker.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - selection: The chosen ISO 3166-1 alpha-2 code, or `nil`. Any case is read; the picker writes upper case.
    ///   - prompt: Shown in the field while nothing is chosen.
    ///   - helper: Help text under the field.
    ///   - error: An error message, shown with an icon and announced.
    ///   - countries: Limits the list to these ISO codes. Every country with a calling code by default.
    ///   - preferred: ISO codes listed first under "Suggested", such as the person's own country.
    ///   - locale: The language of the names and the sort order. The environment's locale by default.
    ///   - showsFlags: Shows each country's flag at the end of its row.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - isClearable: Shows a clear button in the field. Off by default: a phone number needs a country.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ label: String,
        selection: Binding<String?>,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        countries: [String]? = nil,
        preferred: [String] = [],
        locale: Locale? = nil,
        showsFlags: Bool = true,
        isRequired: Bool = false,
        isClearable: Bool = false,
        labels: MonarchPhoneCountrySelectLabels = MonarchPhoneCountrySelectLabels()
    ) {
        self.label = label
        _selection = selection
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.countries = countries
        self.preferred = preferred
        self.locale = locale
        self.showsFlags = showsFlags
        self.isRequired = isRequired
        self.isClearable = isClearable
        self.labels = labels
    }

    /// The content of the view.
    public var body: some View {
        let code = MonarchGeoSearch.normalized(selection)
        var comboboxLabels = labels.combobox
        comboboxLabels.empty = comboboxLabels.empty ?? MonarchPhoneCountrySelectStrings.empty
        let option = labels.option
        return MonarchCombobox(
            label,
            selection: Binding(get: { code }, set: { selection = $0 }),
            options: MonarchPhoneCountrySelectTokens.options(
                countries: countries,
                preferred: preferred,
                locale: locale ?? environmentLocale,
                showsFlags: showsFlags,
                preferredTitle: labels.suggested ?? MonarchPhoneCountrySelectStrings.suggested,
                allTitle: labels.all ?? MonarchPhoneCountrySelectStrings.all,
                optionText: { name, dialCode in
                    option?(name, dialCode) ?? MonarchPhoneCountrySelectStrings.option(name: name, dialCode: dialCode)
                }
            ),
            prompt: prompt,
            helper: helper,
            error: error,
            isRequired: isRequired,
            isClearable: isClearable,
            sortsByMatch: true,
            labels: comboboxLabels,
            filter: { option, query in MonarchGeoSearch.matchesEveryWord(option, query: query) }
        )
    }
}

#if DEBUG
    #Preview("MonarchPhoneCountrySelect") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchPhoneCountrySelect("Country code", selection: .constant("PT"), preferred: ["PT", "ES"])
                MonarchPhoneCountrySelect(
                    "Country code",
                    selection: .constant(nil),
                    prompt: "Choose a country",
                    error: "Choose a country code.",
                    isRequired: true
                )
                MonarchPhoneCountrySelect("Disabled", selection: .constant("US"), showsFlags: false).disabled(true)
            }
        }
    }
#endif
