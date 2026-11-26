/// Built-in strings of the username availability input, from the `MonarchUsernameAvailabilityInput` strings table.
enum MonarchUsernameAvailabilityInputStrings {
    private static let table = "MonarchUsernameAvailabilityInput"

    /// Shown while the check is waiting or running.
    static var checking: String { MonarchStrings.string("monarch.usernameAvailabilityInput.checking", table: table) }
    /// Shown, and announced, when the value can be used.
    static var valid: String { MonarchStrings.string("monarch.usernameAvailabilityInput.valid", table: table) }
    /// Shown, and announced, when the value cannot be used.
    static var invalid: String { MonarchStrings.string("monarch.usernameAvailabilityInput.invalid", table: table) }
    /// Shown, and announced, when the check itself failed.
    static var error: String { MonarchStrings.string("monarch.usernameAvailabilityInput.error", table: table) }
    /// The title of the button that runs a failed check again.
    static var retry: String { MonarchStrings.string("monarch.usernameAvailabilityInput.retry", table: table) }
}
