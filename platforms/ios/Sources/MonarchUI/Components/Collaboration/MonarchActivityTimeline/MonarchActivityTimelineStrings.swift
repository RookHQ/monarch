/// Built-in strings of the activity timeline, from the `MonarchActivityTimeline` strings table.
enum MonarchActivityTimelineStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.activityTimeline.\(key)", table: "MonarchActivityTimeline")
    }

    /// The spoken name of an ungrouped feed.
    static var list: String { string("list") }
    /// The heading of today's events.
    static var today: String { string("today") }
    /// The heading of yesterday's events.
    static var yesterday: String { string("yesterday") }
    /// The tag of automation and system actors.
    static var automated: String { string("automated") }
    /// The disclosure of an event's details.
    static var details: String { string("details") }
    /// The empty state.
    static var showDetails: String { string("showDetails") }
    static var hideDetails: String { string("hideDetails") }
    static var empty: String { string("empty") }
    /// Spoken while the first page loads.
    static var loading: String { string("loading") }
    /// The error message.
    static var error: String { string("error") }
}
