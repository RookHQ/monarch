import Foundation

/// List logic for ``MonarchLanguageSelect``.
enum MonarchLanguageSelectTokens {
    /// About 80 widely used BCP 47 language tags, the same list as the web.
    static let languageTags: [String] =
        ("af am ar az be bg bn bs ca cs cy da de el en en-GB en-US es es-419 et eu fa fi fil fr fr-CA "
        + "ga gl gu ha he hi hr hu hy id ig is it ja jv ka kk km kn ko ky lo lt lv mk ml mn mr ms mt my "
        + "nb ne nl pa pl ps pt pt-BR pt-PT ro ru si sk sl so sq sr sv sw ta te th tr uk ur uz vi xh yo "
        + "zh-Hans zh-Hant zu").split(separator: " ").map(String.init)

    /// The tag Foundation gives a string it cannot read as a language.
    private static let undetermined = "und"

    /// The canonical BCP 47 form of a tag ("EN-us" gives "en-US"), or `nil`
    /// when it is blank or not a language.
    static func canonical(_ tag: String?) -> String? {
        guard let tag = tag?.trimmingCharacters(in: .whitespaces), !tag.isEmpty else { return nil }
        let canonical = Locale(identifier: tag).identifier(.bcp47)
        return canonical.isEmpty || canonical == undetermined ? nil : canonical
    }

    /// The text with its first letter in upper case for the locale ("français" gives "Français").
    static func capitalized(_ text: String, locale: Locale) -> String {
        guard let first = text.first else { return text }
        return String(first).uppercased(with: locale) + text.dropFirst()
    }

    /// The language's name in `locale`, capitalised, or the tag when Foundation has none.
    static func name(_ tag: String, in locale: Locale) -> String {
        capitalized(locale.localizedString(forIdentifier: tag) ?? tag, locale: locale)
    }

    /// The language's name in the language itself ("Deutsch" for "de").
    static func endonym(_ tag: String) -> String {
        name(tag, in: Locale(identifier: tag))
    }

    /// The canonical tags of a list, without duplicates or invalid tags.
    static func tags(_ languages: [String]?) -> [String] {
        var seen = Set<String>()
        return (languages ?? languageTags).compactMap(canonical).filter { seen.insert($0).inserted }
    }

    /// The options of the language picker: the `priority` languages under
    /// `suggested` while nothing is typed, then every language (under `all`
    /// when there is a suggested group), sorted by the name in `locale`.
    static func options(
        languages: [String]?,
        priority: [String],
        selection: String?,
        query: String,
        locale: Locale,
        labelStyle: MonarchLanguageSelectLabelStyle,
        showsTag: Bool,
        suggested: String,
        all allTitle: String
    ) -> [MonarchComboboxOption] {
        let englishLocale = Locale(identifier: "en")
        let tags = tags(languages)
        let all = MonarchGeoSearch.sorted(tags, locale: locale) { name($0, in: locale) }
        let groups = MonarchGeoSearch.split(
            all: all,
            priority: priority.compactMap(canonical),
            selection: selection,
            isSearching: MonarchGeoSearch.isSearching(query)
        )
        func option(_ tag: String, group: String?) -> MonarchComboboxOption {
            let own = endonym(tag)
            let localized = name(tag, in: locale)
            let label = labelStyle == .endonym ? own : localized
            let other = labelStyle == .endonym ? localized : own
            return MonarchComboboxOption(
                id: tag,
                label: label,
                description: other == label ? nil : other,
                hint: showsTag ? tag : nil,
                group: group,
                keywords: "\(tag) \(name(tag, in: englishLocale))"
            )
        }
        let hasTop = !groups.top.isEmpty
        return groups.top.map { option($0, group: suggested) }
            + groups.rest.map { option($0, group: hasTop ? allTitle : nil) }
    }
}
