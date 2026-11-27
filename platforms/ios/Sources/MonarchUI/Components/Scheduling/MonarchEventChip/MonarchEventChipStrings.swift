/// Built-in strings of the event chip, from the `MonarchEventChip` strings
/// table.
enum MonarchEventChipStrings {
    private static let table = "MonarchEventChip"

    /// The name of a status, shown in bar and block chips and spoken in all.
    static func status(_ status: MonarchEventChipStatus) -> String {
        MonarchStrings.string("monarch.eventChip.status." + status.rawValue, table: table)
    }
}
