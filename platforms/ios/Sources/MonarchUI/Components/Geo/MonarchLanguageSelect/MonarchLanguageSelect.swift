import Foundation
import SwiftUI

/// A searchable picker for a language, each listed under its own name.
///
/// ```swift
/// MonarchLanguageSelect(
///     "Language",
///     selection: $language,
///     languages: ["en", "fr", "de", "pt-BR", "ja"]
/// )
/// ```
///
/// The value is a canonical BCP 47 tag, such as "pt-BR". Each row shows the
/// language's own name ("Deutsch") with its name in the interface's language
/// ("German") under it, both from Foundation
/// (`Locale.localizedString(forIdentifier:)`), so people find their own
/// language whatever language the interface is in. `labelStyle: .localized`
/// swaps the two. `priority` languages are listed first under "Suggested"
/// while nothing is typed. Search matches every word typed against both
/// names, the tag and the English name. Built on ``MonarchCombobox``.
///
/// Accessibility: the field is the combobox's button, labelled with `label`
/// and valued with the chosen language's name. Each row is read with both
/// names, then the tag when it is shown. VoiceOver reads every name in the
/// device's voice, not in the language it is written in.
public struct MonarchLanguageSelect: View {
    private let label: String
    @Binding private var selection: String?
    private let prompt: String?
    private let helper: String?
    private let error: String?
    private let languages: [String]?
    private let priority: [String]
    private let locale: Locale?
    private let labelStyle: MonarchLanguageSelectLabelStyle
    private let showsTag: Bool
    private let isRequired: Bool
    private let isClearable: Bool
    private let labels: MonarchLanguageSelectLabels

    @Environment(\.locale) private var environmentLocale
    @State private var query = ""

    /// A language picker.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - selection: The chosen BCP 47 tag, or `nil`. Any form is read; the picker writes the canonical tag.
    ///   - prompt: Shown in the field while nothing is chosen.
    ///   - helper: Help text under the field.
    ///   - error: An error message, shown with an icon and announced.
    ///   - languages: Limits the list to these tags, such as the languages the product is translated into. About 80 common languages by default.
    ///   - priority: Tags listed first under "Suggested" while nothing is typed, such as `Locale.preferredLanguages`.
    ///   - locale: The interface's language: the second name of each row and the sort order use it. The environment's locale by default.
    ///   - labelStyle: Which name each row shows first.
    ///   - showsTag: Shows each language's tag at the end of its row.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - isClearable: Shows a clear button in the field while a language is chosen.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ label: String,
        selection: Binding<String?>,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        languages: [String]? = nil,
        priority: [String] = [],
        locale: Locale? = nil,
        labelStyle: MonarchLanguageSelectLabelStyle = .endonym,
        showsTag: Bool = false,
        isRequired: Bool = false,
        isClearable: Bool = true,
        labels: MonarchLanguageSelectLabels = MonarchLanguageSelectLabels()
    ) {
        self.label = label
        _selection = selection
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.languages = languages
        self.priority = priority
        self.locale = locale
        self.labelStyle = labelStyle
        self.showsTag = showsTag
        self.isRequired = isRequired
        self.isClearable = isClearable
        self.labels = labels
    }

    /// The BCP 47 tags the picker lists by default.
    public nonisolated static var languageTags: [String] { MonarchLanguageSelectTokens.languageTags }

    /// The content of the view.
    public var body: some View {
        let tag = MonarchLanguageSelectTokens.canonical(selection) ?? selection
        var comboboxLabels = labels.combobox
        comboboxLabels.empty = comboboxLabels.empty ?? MonarchLanguageSelectStrings.empty
        return MonarchCombobox(
            label,
            selection: Binding(get: { tag }, set: { selection = $0 }),
            options: MonarchLanguageSelectTokens.options(
                languages: languages,
                priority: priority,
                selection: tag,
                query: query,
                locale: locale ?? environmentLocale,
                labelStyle: labelStyle,
                showsTag: showsTag,
                suggested: labels.suggested ?? MonarchLanguageSelectStrings.suggested,
                all: labels.all ?? MonarchLanguageSelectStrings.all
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
    #Preview("MonarchLanguageSelect") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchLanguageSelect("Language", selection: .constant("de"), helper: "For emails and the app.")
                MonarchLanguageSelect(
                    "Subtitles",
                    selection: .constant(nil),
                    prompt: "Choose a language",
                    error: "Choose a language.",
                    priority: ["en", "es"],
                    isRequired: true
                )
                MonarchLanguageSelect("Disabled", selection: .constant("ja"), labelStyle: .localized).disabled(true)
            }
        }
    }
#endif
