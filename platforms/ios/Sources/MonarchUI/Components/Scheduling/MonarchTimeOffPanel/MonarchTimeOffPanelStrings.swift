/// Built-in strings of the time-off panel, from the `MonarchTimeOffPanel` strings table.
enum MonarchTimeOffPanelStrings {
    private static let table = "MonarchTimeOffPanel"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.timeOffPanel.\(key)", table: table)
    }

    /// The panel's heading.
    static var title: String { string("title") }
    /// The text under the heading.
    static var message: String { string("message") }
    /// The button that opens the add form.
    static var add: String { string("add") }
    /// The checkbox that shows past entries.
    static var includePast: String { string("includePast") }
    /// The badge of a closure.
    static var closure: String { string("closure") }
    /// Who a closure applies to, in the list and in Who.
    static var everyone: String { string("everyone") }
    /// The badge of an entry that has ended.
    static var past: String { string("past") }
    /// Written after the dates of an all-day entry.
    static var allDay: String { string("allDay") }
    /// The switch that makes the new entry whole days.
    static var allDayToggle: String { string("allDayToggle") }
    /// The title of the remove confirmation.
    static var removeTitle: String { string("removeTitle") }
    /// Why removing matters, in the confirmation.
    static var removeMessage: String { string("removeMessage") }
    /// The button that confirms removing.
    static var removeConfirm: String { string("removeConfirm") }
    /// The title when nothing is planned.
    static var emptyTitle: String { string("emptyTitle") }
    /// The message when nothing is planned.
    static var emptyMessage: String { string("emptyMessage") }
    /// The retry button of the error.
    static var retry: String { string("retry") }
    /// The spoken name of the placeholder rows.
    static var loading: String { string("loading") }
    /// The title of the add form.
    static var formTitle: String { string("formTitle") }
    /// The note under the dates of the add form, before the time zone.
    static var formMessage: String { string("formMessage") }
    /// The picker of the person.
    static var who: String { string("who") }
    /// The picker of the kind of time off.
    static var type: String { string("type") }
    /// The notice when Everyone is chosen.
    static var closureNotice: String { string("closureNotice") }
    /// The first day of an all-day entry.
    static var from: String { string("from") }
    /// The last day of an all-day entry.
    static var until: String { string("until") }
    /// The start of a timed entry.
    static var start: String { string("start") }
    /// The end of a timed entry.
    static var end: String { string("end") }
    /// The reason field.
    static var reason: String { string("reason") }
    /// The note under the reason field.
    static var reasonHelper: String { string("reasonHelper") }
    /// The button that leaves the form or the confirmation.
    static var cancel: String { string("cancel") }
    /// The button that adds the entry.
    static var save: String { string("save") }
    /// The error when the end is not after the start.
    static var endBeforeStart: String { string("endBeforeStart") }
    /// The error when adding or removing fails without a description.
    static var failed: String { string("failed") }
    /// "Remove time off for Ana Lima, Mon, Nov 9, 2026 (all day)".
    static func remove(_ who: String, _ when: String) -> String {
        MonarchStrings.string("monarch.timeOffPanel.remove", table: table, who, when)
    }
}
