/// The parts of an international phone number: its country, its calling
/// code and the national digits that follow the calling code.
///
/// ``MonarchPhoneInput/parse(_:preferred:)`` returns it.
public struct MonarchPhoneInputDetails: Sendable, Hashable {
    /// The ISO 3166-1 alpha-2 code of the country, upper case, such as "GB".
    public let country: String
    /// The calling code with a leading plus, such as "+44".
    public let dialCode: String
    /// The digits after the calling code, as typed after it (a trunk 0 is kept), such as "2079460958".
    public let national: String

    /// The parts of a phone number.
    ///
    /// - Parameters:
    ///   - country: The ISO 3166-1 alpha-2 code. Any case is read; it is stored upper case.
    ///   - dialCode: The calling code with a leading plus.
    ///   - national: The digits after the calling code.
    public init(country: String, dialCode: String, national: String) {
        self.country = country.uppercased()
        self.dialCode = dialCode
        self.national = national
    }
}
