/// Built-in strings of the agenda list, from the `MonarchAgendaList` strings table.
enum MonarchAgendaListStrings {
    private static let table = "MonarchAgendaList"

    /// The marker in today's heading.
    static var today: String { MonarchStrings.string("monarch.agendaList.today", table: table) }
    /// The time of an all-day event.
    static var allDay: String { MonarchStrings.string("monarch.agendaList.allDay", table: table) }
    /// The title of the empty state.
    static var emptyTitle: String { MonarchStrings.string("monarch.agendaList.emptyTitle", table: table) }
    /// "1 event".
    static var countOne: String { MonarchStrings.string("monarch.agendaList.countOne", table: table) }
    /// "3 events".
    static func countOther(_ count: String) -> String {
        MonarchStrings.string("monarch.agendaList.countOther", table: table, count)
    }
    /// "until 11:00".
    static func until(_ end: String) -> String {
        MonarchStrings.string("monarch.agendaList.until", table: table, end)
    }
    /// "9:30 to 10:15".
    static func range(_ start: String, _ end: String) -> String {
        MonarchStrings.string("monarch.agendaList.range", table: table, start, end)
    }
}
