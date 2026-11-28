/// Built-in strings of the delta indicator, from the `MonarchDeltaIndicator` strings table.
enum MonarchDeltaIndicatorStrings {
    private static let table = "MonarchDeltaIndicator"

    /// Spoken before an increase.
    static var up: String { MonarchStrings.string("monarch.deltaIndicator.up", table: table) }
    /// Spoken before a decrease.
    static var down: String { MonarchStrings.string("monarch.deltaIndicator.down", table: table) }
    /// Shown when there is no change.
    static var flat: String { MonarchStrings.string("monarch.deltaIndicator.flat", table: table) }
    /// Spoken after a change in the good direction.
    static var better: String { MonarchStrings.string("monarch.deltaIndicator.better", table: table) }
    /// Spoken after a change in the bad direction.
    static var worse: String { MonarchStrings.string("monarch.deltaIndicator.worse", table: table) }
    /// Shown when there is nothing to compare with.
    static var noData: String { MonarchStrings.string("monarch.deltaIndicator.noData", table: table) }
}
