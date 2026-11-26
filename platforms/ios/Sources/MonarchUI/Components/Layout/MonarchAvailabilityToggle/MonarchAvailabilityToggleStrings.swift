/// Built-in strings of the availability toggle, from the `MonarchAvailabilityToggle` strings table.
enum MonarchAvailabilityToggleStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.availabilityToggle." + key, table: "MonarchAvailabilityToggle")
    }

    /// The spoken name of the switch.
    static var label: String { string("label") }
    /// The status word when on.
    static var available: String { string("available") }
    /// The status word when off.
    static var away: String { string("away") }
    /// The explanation when on.
    static var availableHint: String { string("availableHint") }
    /// The explanation when off.
    static var awayHint: String { string("awayHint") }
    /// Spoken before the hint while a change is being saved.
    static var pending: String { string("pending") }
}
