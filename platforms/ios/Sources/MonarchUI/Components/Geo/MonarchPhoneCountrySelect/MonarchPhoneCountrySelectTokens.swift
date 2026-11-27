import Foundation

/// Calling-code data and list logic for ``MonarchPhoneCountrySelect``.
enum MonarchPhoneCountrySelectTokens {
    /// The ISO code, the calling code without "+" and, after "-", the
    /// dot-separated leading digits. The same table as the web.
    private static let data =
        "AD376 AE971 AF93 AG1-268 AI1-264 AL355 AM374 AO244 AR54 AS1-684 AT43 AU61 AW297 AX358-18 AZ994 "
        + "BA387 BB1-246 BD880 BE32 BF226 BG359 BH973 BI257 BJ229 BL590 BM1-441 BN673 BO591 BQ599-3.4.7 "
        + "BR55 BS1-242 BT975 BW267 BY375 BZ501 CA1 CC61 CD243 CF236 CG242 CH41 CI225 CK682 CL56 CM237 "
        + "CN86 CO57 CR506 CU53 CV238 CW599 CX61 CY357 CZ420 DE49 DJ253 DK45 DM1-767 DO1-809.829.849 "
        + "DZ213 EC593 EE372 EG20 EH212 ER291 ES34 ET251 FI358 FJ679 FK500 FM691 FO298 FR33 GA241 GB44 "
        + "GD1-473 GE995 GF594 GG44-1481 GH233 GI350 GL299 GM220 GN224 GP590 GQ240 GR30 GT502 GU1-671 "
        + "GW245 GY592 HK852 HN504 HR385 HT509 HU36 ID62 IE353 IL972 IM44-1624 IN91 IO246 IQ964 IR98 "
        + "IS354 IT39 JE44-1534 JM1-876.658 JO962 JP81 KE254 KG996 KH855 KI686 KM269 KN1-869 KP850 KR82 "
        + "KW965 KY1-345 KZ7-6.7 LA856 LB961 LC1-758 LI423 LK94 LR231 LS266 LT370 LU352 LV371 LY218 "
        + "MA212 MC377 MD373 ME382 MF590 MG261 MH692 MK389 ML223 MM95 MN976 MO853 MP1-670 MQ596 MR222 "
        + "MS1-664 MT356 MU230 MV960 MW265 MX52 MY60 MZ258 NA264 NC687 NE227 NF672 NG234 NI505 NL31 "
        + "NO47 NP977 NR674 NU683 NZ64 OM968 PA507 PE51 PF689 PG675 PH63 PK92 PL48 PM508 PR1-787.939 "
        + "PS970 PT351 PW680 PY595 QA974 RE262 RO40 RS381 RU7 RW250 SA966 SB677 SC248 SD249 SE46 SG65 "
        + "SH290 SI386 SJ47-79 SK421 SL232 SM378 SN221 SO252 SR597 SS211 ST239 SV503 SX1-721 SY963 SZ268 "
        + "TC1-649 TD235 TG228 TH66 TJ992 TK690 TL670 TM993 TN216 TO676 TR90 TT1-868 TV688 TW886 TZ255 "
        + "UA380 UG256 US1 UY598 UZ998 VA39-06698 VC1-784 VE58 VG1-284 VI1-340 VN84 VU678 WF681 WS685 "
        + "XK383 YE967 YT262-269.639 ZA27 ZM260 ZW263"

    /// Every country with its calling code, ordered by ISO code.
    static let countries: [MonarchPhoneCountry] = data.split(separator: " ").map { entry in
        let parts = entry.split(separator: "-", maxSplits: 1)
        let head = parts[0]
        let digits = parts.count > 1 ? parts[1].split(separator: ".").map(String.init) : []
        return MonarchPhoneCountry(
            code: String(head.prefix(2)),
            dialCode: "+" + head.dropFirst(2),
            leadingDigits: digits
        )
    }

    /// The entries by ISO code.
    static let byCode: [String: MonarchPhoneCountry] = Dictionary(
        uniqueKeysWithValues: countries.map { ($0.code, $0) }
    )

    /// The country that owns a shared calling code when the number does not say otherwise.
    private static let mainCountry: [String: String] = [
        "+1": "US", "+7": "RU", "+44": "GB", "+47": "NO", "+61": "AU", "+212": "MA",
        "+262": "RE", "+358": "FI", "+39": "IT", "+590": "GP", "+599": "CW",
    ]

    /// The longest calling code, in digits.
    private static let longestDialCode = 4

    /// Bidirectional marks that phones and chat apps wrap around a number.
    private static let bidiMarks: Set<Unicode.Scalar> = Set(
        ["\u{200E}", "\u{200F}", "\u{202A}", "\u{202B}", "\u{202C}", "\u{202D}", "\u{202E}"]
            + ["\u{2066}", "\u{2067}", "\u{2068}", "\u{2069}"]
    )

    /// Plus signs and dashes from other keyboards.
    private static let plusSigns: Set<Character> = ["+", "\u{FF0B}"]

    /// The number with ASCII digits and "+", and without spaces, brackets,
    /// dots, dashes and bidirectional marks, so "＋٤٤ (20)" reads as "+4420".
    static func compact(_ phone: String) -> String {
        var result = ""
        for character in phone {
            if character.unicodeScalars.allSatisfy(bidiMarks.contains) { continue }
            if plusSigns.contains(character) {
                result.append("+")
            } else if let digit = character.wholeNumberValue, character.isNumber, digit < 10 {
                result.append(String(digit))
            } else if character.isWhitespace
                || "().-\u{2010}\u{2011}\u{2012}\u{2013}\u{2014}\u{2015}\u{2212}\u{FF0D}".contains(character)
            {
                continue
            } else {
                result.append(character)
            }
        }
        return result
    }

    /// The country an international number belongs to. See ``MonarchPhoneCountry/matching(_:preferred:)``.
    static func match(_ phone: String, preferred: String?) -> MonarchPhoneCountry? {
        let compact = compact(phone)
        let digits: Substring
        if compact.hasPrefix("+") {
            digits = compact.dropFirst()
        } else if compact.hasPrefix("00") {
            digits = compact.dropFirst(2)
        } else {
            return nil
        }
        guard !digits.isEmpty, digits.allSatisfy(\.isASCII), digits.allSatisfy(\.isNumber) else { return nil }
        for length in stride(from: min(longestDialCode, digits.count), through: 1, by: -1) {
            let dialCode = "+" + digits.prefix(length)
            let candidates = countries.filter { $0.dialCode == dialCode }
            guard !candidates.isEmpty else { continue }
            let rest = digits.dropFirst(length)
            if let byDigits = candidates.first(where: { $0.leadingDigits.contains { rest.hasPrefix($0) } }) {
                return byDigits
            }
            let preferred = preferred?.uppercased()
            return candidates.first { $0.code == preferred && $0.leadingDigits.isEmpty }
                ?? candidates.first { $0.code == mainCountry[dialCode] }
                ?? candidates.first { $0.leadingDigits.isEmpty }
                ?? candidates.first
        }
        return nil
    }

    /// The words a country is searched by besides its label: the ISO code,
    /// the calling code with and without "+", and the calling code with each
    /// of its leading digits.
    static func keywords(_ country: MonarchPhoneCountry) -> String {
        ([country.code, country.dialCode, String(country.dialCode.dropFirst())]
            + country.leadingDigits.map { country.dialCode + $0 })
            .joined(separator: " ")
    }

    /// The options of the picker: the `preferred` countries under
    /// `preferredTitle`, then the others (under `allTitle` when there are
    /// preferred ones) sorted by label in `locale`. Each country is listed
    /// once, so a preferred country is not repeated below.
    static func options(
        countries codes: [String]?,
        preferred: [String],
        locale: Locale,
        showsFlags: Bool,
        preferredTitle: String,
        allTitle: String,
        optionText: (String, String) -> String
    ) -> [MonarchComboboxOption] {
        var seen = Set<String>()
        let list =
            codes.map { $0.compactMap { byCode[$0.uppercased()] }.filter { seen.insert($0.code).inserted } }
            ?? countries
        func option(_ country: MonarchPhoneCountry, group: String?) -> MonarchComboboxOption {
            let name = MonarchGeoSearch.name(country.code, locale: locale)
            return MonarchComboboxOption(
                id: country.code,
                label: optionText(name, country.dialCode),
                hint: showsFlags ? MonarchGeoSearch.flag(country.code) : nil,
                group: group,
                keywords: keywords(country)
            )
        }
        var preferredSeen = Set<String>()
        let top = preferred.map { $0.uppercased() }
            .compactMap { code in list.first { $0.code == code } }
            .filter { preferredSeen.insert($0.code).inserted }
            .map { option($0, group: preferredTitle) }
        let rest = MonarchGeoSearch.sorted(
            list.filter { !preferredSeen.contains($0.code) }.map { option($0, group: top.isEmpty ? nil : allTitle) },
            locale: locale,
            by: \.label
        )
        return top + rest
    }
}
