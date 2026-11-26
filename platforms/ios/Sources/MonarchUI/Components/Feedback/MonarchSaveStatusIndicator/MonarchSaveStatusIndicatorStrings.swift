/// Built-in strings of the save status, from the `MonarchSaveStatusIndicator` strings table.
enum MonarchSaveStatusIndicatorStrings {
    private static let table = "MonarchSaveStatusIndicator"

    /// Edits are not saved yet.
    static var unsaved: String { MonarchStrings.string("monarch.saveStatusIndicator.unsaved", table: table) }
    /// A save is in flight.
    static var saving: String { MonarchStrings.string("monarch.saveStatusIndicator.saving", table: table) }
    /// The last save finished.
    static var saved: String { MonarchStrings.string("monarch.saveStatusIndicator.saved", table: table) }
    /// The last save failed.
    static var error: String { MonarchStrings.string("monarch.saveStatusIndicator.error", table: table) }
    /// The retry button title.
    static var retry: String { MonarchStrings.string("monarch.saveStatusIndicator.retry", table: table) }
}
