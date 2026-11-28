import Foundation

/// List logic for ``MonarchCountrySelect``. The search, sorting and grouping
/// helpers the geo pickers share are `Internal/MonarchGeoSearch`.
enum MonarchCountrySelectTokens {
    /// The codes the picker offers: `countries` (or every code), upper case,
    /// without duplicates and without `excluded`.
    static func codes(countries: [String]?, excluded: [String]) -> [String] {
        let skip = Set(excluded.compactMap(MonarchGeoSearch.normalized))
        var seen = Set<String>()
        return (countries ?? MonarchGeoSearch.countryCodes).compactMap(MonarchGeoSearch.normalized).filter {
            !skip.contains($0) && seen.insert($0).inserted
        }
    }

    /// The trailing hint of a country: its code, its flag, or both.
    static func hint(_ code: String, showsCode: Bool, showsFlag: Bool) -> String? {
        let parts = [showsCode ? code : "", showsFlag ? MonarchGeoSearch.flag(code) : ""].filter { !$0.isEmpty }
        return parts.isEmpty ? nil : parts.joined(separator: " ")
    }

    /// The options of the country picker: the `priority` countries under
    /// `suggested` while nothing is typed, then every country (under `all`
    /// when there is a suggested group), sorted by name in `locale`.
    static func options(
        countries: [String]?,
        excluded: [String],
        priority: [String],
        selection: String?,
        query: String,
        locale: Locale,
        showsFlag: Bool,
        showsCode: Bool,
        suggested: String,
        all allTitle: String,
        countryName: (@Sendable (String) -> String?)?
    ) -> [MonarchComboboxOption] {
        let english = MonarchGeoSearch.isEnglish(locale)
        let englishLocale = Locale(identifier: "en")
        let names = Dictionary(
            (codes(countries: countries, excluded: excluded) + [selection].compactMap { $0 })
                .map { ($0, MonarchGeoSearch.name($0, locale: locale, override: countryName)) },
            uniquingKeysWith: { first, _ in first }
        )
        let all = MonarchGeoSearch.sorted(
            codes(countries: countries, excluded: excluded),
            locale: locale
        ) { names[$0] ?? $0 }
        let groups = MonarchGeoSearch.split(
            all: all,
            priority: priority.compactMap(MonarchGeoSearch.normalized),
            selection: selection,
            isSearching: MonarchGeoSearch.isSearching(query)
        )
        func option(_ code: String, group: String?) -> MonarchComboboxOption {
            MonarchComboboxOption(
                id: code,
                label: names[code] ?? MonarchGeoSearch.name(code, locale: locale, override: countryName),
                hint: showsCode ? code : nil,
                group: group,
                keywords: english ? nil : englishLocale.localizedString(forRegionCode: code),
                leadingText: showsFlag ? MonarchGeoSearch.flag(code) : nil
            )
        }
        let hasTop = !groups.top.isEmpty
        return groups.top.map { option($0, group: suggested) }
            + groups.rest.map { option($0, group: hasTop ? allTitle : nil) }
    }
}
