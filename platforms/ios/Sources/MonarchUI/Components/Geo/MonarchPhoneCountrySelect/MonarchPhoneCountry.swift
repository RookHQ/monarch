/// A country or territory and its international calling code (ITU-T E.164).
///
/// Neither iOS nor Android exposes calling codes publicly, so Monarch
/// bundles them, the same table as the web. Names are not stored: they come
/// from Foundation in the reader's language.
public struct MonarchPhoneCountry: Sendable, Hashable, Identifiable {
    /// The ISO 3166-1 alpha-2 code, upper case, such as "GB".
    public let code: String
    /// The calling code with a leading plus, such as "+44". Several countries can share one.
    public let dialCode: String
    /// The digits that follow the calling code for this country when it
    /// shares the code with others, such as "242" for the Bahamas under "+1".
    public let leadingDigits: [String]

    /// The ISO code, which identifies the country.
    public var id: String { code }

    /// A calling code entry.
    ///
    /// - Parameters:
    ///   - code: The ISO 3166-1 alpha-2 code.
    ///   - dialCode: The calling code with a leading plus.
    ///   - leadingDigits: The digits after a shared calling code that pick this country.
    public init(code: String, dialCode: String, leadingDigits: [String] = []) {
        self.code = code.uppercased()
        self.dialCode = dialCode
        self.leadingDigits = leadingDigits
    }

    /// Every country and territory with its calling code, ordered by ISO code.
    public static var all: [MonarchPhoneCountry] { MonarchPhoneCountrySelectTokens.countries }

    /// The entry for an ISO 3166-1 alpha-2 code in any case, or `nil`.
    public static func country(code: String) -> MonarchPhoneCountry? {
        MonarchPhoneCountrySelectTokens.byCode[code.uppercased()]
    }

    /// The country an international number belongs to, from its first digits:
    /// "+44 20 7946 0000" gives GB and "+1 242 555 0100" gives BS.
    ///
    /// The number must start with "+" or "00". Digits from other scripts and
    /// full-width characters are read too. A calling code shared by several
    /// countries resolves to `preferred` when it shares that code, otherwise
    /// to the code's main country (US for +1).
    ///
    /// - Parameters:
    ///   - phone: The number as typed.
    ///   - preferred: The ISO code to prefer for a shared calling code.
    /// - Returns: The country, or `nil` when the number has no known calling code.
    public static func matching(_ phone: String, preferred: String? = nil) -> MonarchPhoneCountry? {
        MonarchPhoneCountrySelectTokens.match(phone, preferred: preferred)
    }
}
