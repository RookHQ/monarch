/// Built-in strings of ``MonarchAppAccessPicker`` and ``MonarchAppAccessSummary``,
/// from the `MonarchAppAccessPicker` strings table.
enum MonarchAppAccessPickerStrings {
    private static let table = "MonarchAppAccessPicker"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.appAccessPicker.\(key)", table: table)
    }

    /// The group's label.
    static var label: String { string("label") }
    /// The bulk button while not everything is selected.
    static var selectAll: String { string("selectAll") }
    /// The bulk button while everything is selected.
    static var clearAll: String { string("clearAll") }
    /// Read while the apps load.
    static var loading: String { string("loading") }
    /// Shown when there are no apps.
    static var empty: String { string("empty") }
    /// The summary when nothing is granted.
    static var none: String { string("none") }

    /// The selection count.
    static func count(selected: Int, total: Int) -> String {
        MonarchStrings.string("monarch.appAccessPicker.count", table: table, selected, total)
    }
    /// The summary's visible count of hidden apps.
    static func moreCount(_ count: Int) -> String {
        MonarchStrings.string("monarch.appAccessPicker.moreCount", table: table, count)
    }
    /// The summary's spoken end.
    static func more(_ count: Int, names: String) -> String {
        MonarchStrings.string("monarch.appAccessPicker.more", table: table, count, names)
    }
}
