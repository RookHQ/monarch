/// Built-in strings of the event popover, from the `MonarchEventPopover` strings table.
enum MonarchEventPopoverStrings {
    private static let table = "MonarchEventPopover"

    /// The spoken name of the time row.
    static var time: String { MonarchStrings.string("monarch.eventPopover.time", table: table) }
    /// Shown instead of a time for an all-day event.
    static var allDay: String { MonarchStrings.string("monarch.eventPopover.allDay", table: table) }
}
