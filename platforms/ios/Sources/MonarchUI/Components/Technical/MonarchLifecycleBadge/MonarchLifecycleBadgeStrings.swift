/// Built-in strings of the lifecycle badge, from the `MonarchLifecycleBadge`
/// strings table.
enum MonarchLifecycleBadgeStrings {
    private static let table = "MonarchLifecycleBadge"

    /// The label of a built-in stage.
    static func label(_ lifecycle: MonarchLifecycle) -> String {
        MonarchStrings.string("monarch.lifecycleBadge." + lifecycle.rawValue, table: table)
    }
}
