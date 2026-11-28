import Foundation

/// Country codes, flags, localised names and search for the geo pickers
/// (country, phone country, currency, language, region, location fields,
/// phone input), so every picker lists and searches the same way.
enum MonarchGeoSearch {
    /// The 249 officially assigned ISO 3166-1 alpha-2 codes, the same list as the web.
    ///
    /// `Locale.Region.isoRegions` is not used: it also lists continents, UN M.49
    /// numeric areas and reserved codes, and changes between OS versions.
    static let countryCodes: [String] =
        ("AD AE AF AG AI AL AM AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BJ BL BM BN BO BQ BR "
        + "BS BT BV BW BY BZ CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CW CX CY CZ DE DJ DK DM DO DZ "
        + "EC EE EG EH ER ES ET FI FJ FK FM FO FR GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW "
        + "GY HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP KE KG KH KI KM KN KP KR KW KY "
        + "KZ LA LB LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU MV "
        + "MW MX MY MZ NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN PR PS PT PW PY "
        + "QA RE RO RS RU RW SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR SS ST SV SX SY SZ TC TD TF TG "
        + "TH TJ TK TL TM TN TO TR TT TV TW TZ UA UG UM US UY UZ VA VC VE VG VI VN VU WF WS YE YT ZA ZM ZW").split(
            separator: " "
        ).map(String.init)

    /// The Unicode scalar of the regional indicator symbol letter A.
    private static let regionalIndicatorA: UInt32 = 0x1F1E6

    /// The Unicode scalar of the Latin capital letter A.
    private static let latinCapitalA: UInt32 = 0x41

    /// The flag emoji of a two-letter code, from regional indicator symbols
    /// ("PT" gives 🇵🇹), or an empty string for anything else.
    static func flag(_ code: String) -> String {
        let scalars = Array(code.uppercased().unicodeScalars)
        guard scalars.count == 2, scalars.allSatisfy({ ("A"..."Z").contains($0) }) else { return "" }
        var flag = String.UnicodeScalarView()
        for scalar in scalars {
            guard let indicator = Unicode.Scalar(regionalIndicatorA + scalar.value - latinCapitalA) else { return "" }
            flag.append(indicator)
        }
        return String(flag)
    }

    /// The upper-case form of a code, or `nil` when it is blank.
    static func normalized(_ code: String?) -> String? {
        guard let code = code?.trimmingCharacters(in: .whitespaces), !code.isEmpty else { return nil }
        return code.uppercased()
    }

    /// The country's name in `locale`, from the override or from Foundation,
    /// or the code when neither has one.
    static func name(
        _ code: String,
        locale: Locale,
        override: (@Sendable (String) -> String?)? = nil
    ) -> String {
        if let custom = override?(code) { return custom }
        let key = "\(locale.identifier)|\(code)" as NSString
        if let cached = nameCache.object(forKey: key) { return cached as String }
        let found = locale.localizedString(forRegionCode: code) ?? code
        nameCache.setObject(found as NSString, forKey: key)
        return found
    }

    /// Names already looked up, by locale and code. Foundation's lookup is
    /// slow enough to show when a list of 249 countries is drawn and
    /// searched; NSCache is safe to use from any thread.
    nonisolated(unsafe) private static let nameCache = NSCache<NSString, NSString>()

    /// Whether `locale` is an English locale, where English names add nothing to search.
    static func isEnglish(_ locale: Locale) -> Bool {
        locale.language.languageCode == .english
    }

    /// Sorts values by a name in the locale's order.
    static func sorted<Value>(_ values: [Value], locale: Locale, by name: (Value) -> String) -> [Value] {
        values
            .map { (value: $0, name: name($0)) }
            .sorted {
                $0.name.compare($1.name, options: [.caseInsensitive], range: nil, locale: locale) == .orderedAscending
            }
            .map(\.value)
    }

    /// The default search of the geo pickers: every word typed appears in the
    /// option's label, id, description, hint or keywords, ignoring case and accents.
    static func matchesEveryWord(_ option: MonarchComboboxOption, query: String) -> Bool {
        let words = query.split(whereSeparator: \.isWhitespace)
        guard !words.isEmpty else { return true }
        let haystack = [option.label, option.id, option.description, option.hint, option.keywords]
            .compactMap { $0 }
            .joined(separator: " ")
        return words.allSatisfy { haystack.localizedStandardContains($0) }
    }

    /// Whether the person is searching: the query has more than spaces.
    static func isSearching(_ query: String) -> Bool {
        !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    /// The codes to list first and the rest. `priority` codes (already
    /// normalised) that are in `all` come first while nothing is typed; a selection outside `all` is
    /// kept at the start of the rest, so it can still be shown and chosen.
    static func split(
        all: [String],
        priority: [String],
        selection: String?,
        isSearching: Bool
    ) -> (top: [String], rest: [String]) {
        var seen = Set<String>()
        let top =
            isSearching
            ? []
            : priority.filter { all.contains($0) && seen.insert($0).inserted }
        let extra = selection.flatMap { all.contains($0) ? nil : $0 }.map { [$0] } ?? []
        return (top, extra + all)
    }
}
