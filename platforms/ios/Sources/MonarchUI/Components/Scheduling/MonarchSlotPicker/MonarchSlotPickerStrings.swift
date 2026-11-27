/// Built-in strings of the slot picker, from the `MonarchSlotPicker` strings table.
enum MonarchSlotPickerStrings {
    private static let table = "MonarchSlotPicker"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.slotPicker.\(key)", table: table)
    }

    static var chooseDay: String { string("chooseDay") }
    static var empty: String { string("empty") }
    static var loading: String { string("loading") }
    static var timeZone: String { string("timeZone") }
    static var chooseOutside: String { string("chooseOutside") }
    static var retry: String { string("retry") }

    /// Announced when the times arrive.
    static func loaded(_ count: Int, _ day: String) -> String {
        switch count {
        case 0: MonarchStrings.string("monarch.slotPicker.loadedNone", table: table, day)
        case 1: MonarchStrings.string("monarch.slotPicker.loadedOne", table: table, day)
        default: MonarchStrings.string("monarch.slotPicker.loadedOther", table: table, count, day)
        }
    }

    /// The name of the group of times.
    static func times(_ day: String) -> String { MonarchStrings.string("monarch.slotPicker.times", table: table, day) }

    /// Under a time with limited places.
    static func seatsLeft(_ count: Int) -> String {
        MonarchStrings.string("monarch.slotPicker.seatsLeft", table: table, count)
    }

    /// The note when the zone cannot be changed.
    static func shownIn(_ zone: String) -> String {
        MonarchStrings.string("monarch.slotPicker.shownIn", table: table, zone)
    }

    /// The note when the business works in another zone.
    static func convertedFrom(_ zone: String) -> String {
        MonarchStrings.string("monarch.slotPicker.convertedFrom", table: table, zone)
    }

    /// The warning when the selection is not an open time.
    static func notOpen(_ time: String) -> String {
        MonarchStrings.string("monarch.slotPicker.notOpen", table: table, time)
    }
}
