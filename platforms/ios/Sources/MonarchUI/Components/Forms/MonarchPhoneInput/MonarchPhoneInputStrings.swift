/// Built-in strings of ``MonarchPhoneInput``, from the `MonarchPhoneInput` strings table.
enum MonarchPhoneInputStrings {
    private static let table = "MonarchPhoneInput"

    /// The spoken name of the country button and the title of the country list.
    static var country: String { MonarchStrings.string("monarch.phoneInput.country", table: table) }
    /// Shown when the number is too short or too long.
    static var invalid: String { MonarchStrings.string("monarch.phoneInput.invalid", table: table) }
}
