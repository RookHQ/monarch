/// Built-in strings of the incident timeline, from the `MonarchIncidentTimeline` strings table.
enum MonarchIncidentTimelineStrings {
    /// The string for a key after `monarch.incidentTimeline.`.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.incidentTimeline.\(key)", table: "MonarchIncidentTimeline")
    }
}
