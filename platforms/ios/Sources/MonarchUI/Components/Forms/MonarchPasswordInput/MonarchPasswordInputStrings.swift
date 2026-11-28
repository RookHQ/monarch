/// Built-in strings of the password input, from the `MonarchPasswordInput` strings table.
enum MonarchPasswordInputStrings {
    /// The spoken name of the reveal toggle. It is the same in both states.
    static var show: String { MonarchStrings.string("monarch.passwordInput.show", table: "MonarchPasswordInput") }

    /// Spoken as the hint, and announced, when the input has an error.
    static func error(_ message: String) -> String {
        MonarchStrings.string("monarch.passwordInput.error", table: "MonarchPasswordInput", message)
    }
}
