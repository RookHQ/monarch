import MonarchTokens
import SwiftUI

/// Component tokens and number logic for ``MonarchPhoneInput``.
enum MonarchPhoneInputTokens {
    /// The least height of the country button and the number field, which is also the touch target.
    static let minHeight: CGFloat = MonarchTextFieldTokens.minHeight
    /// Padding before and after the content of the button and the field.
    static let horizontalPadding: CGFloat = MonarchTextFieldTokens.horizontalPadding
    /// Padding above and below the text of the field.
    static let verticalPadding: CGFloat = MonarchTextFieldTokens.verticalPadding
    /// The gap between the label, the row and the supporting text.
    static let stackSpacing: CGFloat = MonarchTextFieldTokens.stackSpacing
    /// The gap between the country button and the number field.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the flag, the calling code and the chevron.
    static let buttonSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The SF Symbol after the calling code.
    static let chevronSymbol = "chevron.down"
    /// The SF Symbol before an error message.
    static let errorSymbol = MonarchFormFieldTokens.errorSymbol

    /// The least and most digits of an E.164 number after the "+".
    static let digitRange = 8...15
    /// The fewest digits after "+" or "00" that make a pasted text a full international number.
    static let pastedMinimumDigits = 7
    /// Countries whose national numbers keep their leading 0 after the calling code.
    static let keepsLeadingZero: Set<String> = ["IT", "SM", "VA"]

    // MARK: Number logic

    /// The ASCII digits of a text, from any script: "(020) ٧٩٤٦" gives "0207946".
    static func digits(_ text: String) -> String {
        MonarchPhoneCountrySelectTokens.compact(text).filter { $0.isASCII && $0.isNumber }
    }

    /// See ``MonarchPhoneInput/toE164(dialCode:national:country:)``.
    static func toE164(dialCode: String, national: String, country: String?) -> String? {
        var national = digits(national)
        guard !national.isEmpty else { return nil }
        if national.hasPrefix("0"), !keepsLeadingZero.contains((country ?? "").uppercased()) {
            national.removeFirst()
        }
        return "+" + digits(dialCode) + national
    }

    /// See ``MonarchPhoneInput/isValidE164(_:)``.
    static func isValidE164(_ value: String) -> Bool {
        guard value.hasPrefix("+") else { return false }
        let rest = value.dropFirst()
        guard digitRange.contains(rest.count), rest.allSatisfy({ $0.isASCII && $0.isNumber }) else { return false }
        return rest.first != "0"
    }

    /// See ``MonarchPhoneInput/parse(_:preferred:)``.
    static func parse(_ phone: String, preferred: String?) -> MonarchPhoneInputDetails? {
        guard let match = MonarchPhoneCountry.matching(phone, preferred: preferred) else { return nil }
        let compact = MonarchPhoneCountrySelectTokens.compact(phone)
        let all = compact.hasPrefix("+") ? compact.dropFirst() : compact.dropFirst(2)
        let national = String(all.dropFirst(digits(match.dialCode).count))
        return MonarchPhoneInputDetails(country: match.code, dialCode: match.dialCode, national: national)
    }

    /// The country and the national digits to show for a value: the parts
    /// of an international value, or its digits under `country` when no
    /// calling code matches.
    static func split(_ value: String?, country: String) -> (country: String, national: String) {
        guard let value, !value.isEmpty else { return (country, "") }
        if let parsed = parse(value, preferred: country) { return (parsed.country, parsed.national) }
        return (country, digits(value))
    }

    /// When typed or pasted text is a full international number ("+44 20
    /// 7946 0958", "0044…"), its country and national digits; otherwise `nil`.
    static func pasted(_ text: String, country: String) -> (country: String, national: String)? {
        let compact = MonarchPhoneCountrySelectTokens.compact(text)
        let digitsAfterPrefix: Substring
        if compact.hasPrefix("+") {
            digitsAfterPrefix = compact.dropFirst()
        } else if compact.hasPrefix("00") {
            digitsAfterPrefix = compact.dropFirst(2)
        } else {
            return nil
        }
        guard digitsAfterPrefix.count >= pastedMinimumDigits,
            digitsAfterPrefix.allSatisfy({ $0.isASCII && $0.isNumber }),
            let parsed = parse(compact, preferred: country)
        else { return nil }
        return (parsed.country, parsed.national)
    }

    /// What committing the typed text gives.
    struct Commit: Equatable {
        /// The E.164 value to write, or `nil` for an empty number. Not used when `isInvalid`.
        var value: String?
        /// The country after the commit, which an international number or a shared calling code can change.
        var country: String
        /// The text to keep in the field.
        var draft: String
        /// Whether the number is incomplete or too long, so nothing is written.
        var isInvalid: Bool
    }

    /// Reads the typed text for a country, as the field does when it loses
    /// focus or Return is pressed: an international number picks its
    /// country, separators and one trunk 0 are removed, an empty text is
    /// `nil`, and a number outside 8 to 15 digits is invalid. A number
    /// under a shared calling code (+1 242…) moves to the country it
    /// belongs to.
    static func commit(_ text: String, country: String) -> Commit {
        var code = country.uppercased()
        var national = text
        var draft = text
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        let compact = MonarchPhoneCountrySelectTokens.compact(trimmed)
        if compact.hasPrefix("+") || compact.hasPrefix("00") {
            guard let parsed = parse(compact, preferred: code) else {
                return Commit(value: nil, country: code, draft: draft, isInvalid: true)
            }
            code = parsed.country
            national = parsed.national
            draft = parsed.national
        }
        let dialCode = MonarchPhoneCountry.country(code: code)?.dialCode ?? ""
        guard let e164 = toE164(dialCode: dialCode, national: national, country: code) else {
            return Commit(value: nil, country: code, draft: draft, isInvalid: false)
        }
        guard isValidE164(e164) else {
            return Commit(value: nil, country: code, draft: draft, isInvalid: true)
        }
        if let owner = MonarchPhoneCountry.matching(e164, preferred: code), owner.code != code,
            owner.dialCode == dialCode
        {
            code = owner.code
        }
        return Commit(value: e164, country: code, draft: draft, isInvalid: false)
    }

    // MARK: Text and appearance

    /// What the country button shows: the flag (when shown) and the calling code.
    static func buttonText(country: String, showsFlags: Bool) -> String {
        let dialCode = MonarchPhoneCountry.country(code: country)?.dialCode ?? ""
        let flag = showsFlags ? MonarchGeoSearch.flag(country) : ""
        return [flag, dialCode].filter { !$0.isEmpty }.joined(separator: " ")
    }

    /// The spoken value of the country button: the row text of the list,
    /// such as "Portugal (+351)".
    static func countryValue(
        country: String,
        locale: Locale,
        option: (@Sendable (String, String) -> String)?
    ) -> String {
        let dialCode = MonarchPhoneCountry.country(code: country)?.dialCode ?? ""
        let name = MonarchGeoSearch.name(country, locale: locale)
        return option?(name, dialCode) ?? MonarchPhoneCountrySelectStrings.option(name: name, dialCode: dialCode)
    }

    /// The error to show: the component's own "incomplete number" message
    /// first, then the app's error.
    static func activeError(isInvalid: Bool, error: String?, labels: MonarchPhoneInputLabels) -> String? {
        if isInvalid { return labels.invalid ?? MonarchPhoneInputStrings.invalid }
        return MonarchFormFieldTokens.activeError(error)
    }

    /// The fill role of the button and the field.
    static func background(isEnabled: Bool) -> MonarchColorRole {
        MonarchTextFieldTokens.background(isEnabled: isEnabled)
    }

    /// The border role of the button and the field.
    static func border(hasError: Bool, isEnabled: Bool) -> MonarchColorRole {
        MonarchTextFieldTokens.border(hasError: hasError, isEnabled: isEnabled)
    }

    /// The colour role of the calling code and the typed number.
    static func textColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }
}
