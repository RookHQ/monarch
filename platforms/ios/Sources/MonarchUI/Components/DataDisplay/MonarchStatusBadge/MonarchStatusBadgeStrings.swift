/// Built-in labels of the shipped statuses, from the `MonarchStatusBadge` strings table.
enum MonarchStatusBadgeStrings {
    /// The label of a built-in status key, such as "Partial outage" for `partial`.
    static func label(_ key: String) -> String {
        MonarchStrings.string("monarch.statusBadge.\(key)", table: "MonarchStatusBadge")
    }
}
