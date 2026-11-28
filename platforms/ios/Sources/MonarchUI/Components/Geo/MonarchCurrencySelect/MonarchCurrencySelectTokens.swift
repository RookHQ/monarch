import Foundation

/// List logic for ``MonarchCurrencySelect``.
enum MonarchCurrencySelectTokens {
    /// Active ISO 4217 currencies in circulation, the same list as the web:
    /// no funds, metals, test codes or retired currencies.
    ///
    /// `Locale.Currency.isoCurrencies` is not used: it also lists funds,
    /// precious metals, test codes and retired currencies.
    static let currencyCodes: [String] =
        ("AED AFN ALL AMD AOA ARS AUD AWG AZN BAM BBD BDT BHD BIF BMD BND BOB BRL BSD BTN BWP BYN BZD "
        + "CAD CDF CHF CLP CNY COP CRC CUP CVE CZK DJF DKK DOP DZD EGP ERN ETB EUR FJD FKP GBP GEL GHS "
        + "GIP GMD GNF GTQ GYD HKD HNL HTG HUF IDR ILS INR IQD IRR ISK JMD JOD JPY KES KGS KHR KMF KPW "
        + "KRW KWD KYD KZT LAK LBP LKR LRD LSL LYD MAD MDL MGA MKD MMK MNT MOP MRU MUR MVR MWK MXN MYR "
        + "MZN NAD NGN NIO NOK NPR NZD OMR PAB PEN PGK PHP PKR PLN PYG QAR RON RSD RUB RWF SAR SBD SCR "
        + "SDG SEK SGD SHP SLE SOS SRD SSP STN SVC SYP SZL THB TJS TMT TND TOP TRY TTD TWD TZS UAH UGX "
        + "USD UYU UZS VED VES VND VUV WST XAF XCD XCG XOF XPF YER ZAR ZMW ZWG").split(separator: " ").map(String.init)

    /// The separator between the code and the name in the field.
    static let codeNameSeparator = " · "

    /// The currency's name in `locale`, or the code when Foundation has none.
    static func name(_ code: String, locale: Locale) -> String {
        locale.localizedString(forCurrencyCode: code) ?? code
    }

    /// The currency's narrow symbol in `locale` ("$" for USD, "€" for EUR),
    /// read from Foundation's currency format; the code when there is none.
    static func symbol(_ code: String, locale: Locale) -> String {
        let formatted = Decimal.zero.formatted(
            .currency(code: code).locale(locale).presentation(.narrow).attributed
        )
        let symbol = formatted.runs
            .filter { $0.numberSymbol == .currency }
            .map { String(formatted[$0.range].characters) }
            .joined()
            .trimmingCharacters(in: .whitespaces)
        return symbol.isEmpty ? code : symbol
    }

    /// The text the field and a row show as the label.
    static func label(code: String, name: String, display: MonarchCurrencySelectDisplay) -> String {
        switch display {
        case .name: name
        case .code: code
        case .codeAndName: code + codeNameSeparator + name
        }
    }

    /// The trailing hint of a row: the code, and the symbol when it differs.
    static func hint(code: String, symbol: String, showsSymbol: Bool) -> String {
        showsSymbol && symbol != code ? "\(code) \(symbol)" : code
    }

    /// The options of the currency picker: the `priority` currencies under
    /// `suggested` while nothing is typed, then every currency (under `all`
    /// when there is a suggested group), sorted by name in `locale`.
    static func options(
        currencies: [String]?,
        priority: [String],
        selection: String?,
        query: String,
        locale: Locale,
        display: MonarchCurrencySelectDisplay,
        showsSymbol: Bool,
        suggested: String,
        all allTitle: String
    ) -> [MonarchComboboxOption] {
        let english = MonarchGeoSearch.isEnglish(locale)
        let englishLocale = Locale(identifier: "en")
        var seen = Set<String>()
        let codes = (currencies ?? currencyCodes).compactMap(MonarchGeoSearch.normalized)
            .filter { seen.insert($0).inserted }
        let names = Dictionary(
            uniqueKeysWithValues: (codes + [selection].compactMap { $0 }.filter { !seen.contains($0) })
                .map { ($0, name($0, locale: locale)) }
        )
        let all = MonarchGeoSearch.sorted(codes, locale: locale) { names[$0] ?? $0 }
        let groups = MonarchGeoSearch.split(
            all: all,
            priority: priority.compactMap(MonarchGeoSearch.normalized),
            selection: selection,
            isSearching: MonarchGeoSearch.isSearching(query)
        )
        func option(_ code: String, group: String?) -> MonarchComboboxOption {
            let name = names[code] ?? code
            let symbol = symbol(code, locale: locale)
            let keywords = [symbol, english ? "" : self.name(code, locale: englishLocale)].filter { !$0.isEmpty }
            return MonarchComboboxOption(
                id: code,
                label: label(code: code, name: name, display: display),
                description: display == .name ? nil : name,
                hint: hint(code: code, symbol: symbol, showsSymbol: showsSymbol),
                group: group,
                keywords: keywords.joined(separator: " ")
            )
        }
        let hasTop = !groups.top.isEmpty
        return groups.top.map { option($0, group: suggested) }
            + groups.rest.map { option($0, group: hasTop ? allTitle : nil) }
    }
}
