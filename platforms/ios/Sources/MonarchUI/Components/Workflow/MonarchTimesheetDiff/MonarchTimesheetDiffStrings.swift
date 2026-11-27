import Foundation

/// Built-in strings of the timesheet diff, from the `MonarchTimesheetDiff`
/// strings table, with the replacements of a labels value applied.
enum MonarchTimesheetDiffStrings {
    private static let table = "MonarchTimesheetDiff"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.timesheetDiff." + key, table: table)
    }

    private static func string(_ key: String, _ arguments: String...) -> String {
        String(format: string(key), arguments: arguments)
    }

    static func title(_ labels: MonarchTimesheetDiffLabels) -> String { labels.title ?? string("title") }
    static func added(_ labels: MonarchTimesheetDiffLabels) -> String { labels.added ?? string("added") }
    static func removed(_ labels: MonarchTimesheetDiffLabels) -> String { labels.removed ?? string("removed") }
    static func changed(_ fields: [String], _ labels: MonarchTimesheetDiffLabels) -> String {
        labels.changed?(fields) ?? string("changed", fields.joined(separator: string("fieldSeparator")))
    }
    static func field(_ field: MonarchTimesheetDiffField, _ labels: MonarchTimesheetDiffLabels) -> String {
        if let name = labels.fields[field] { return name }
        let key = "monarch.timesheetDiff.field." + field.rawValue
        let name = MonarchStrings.string(key, table: table)
        return name == key ? field.rawValue : name
    }
    static func before(_ labels: MonarchTimesheetDiffLabels) -> String { labels.before ?? string("before") }
    static func after(_ labels: MonarchTimesheetDiffLabels) -> String { labels.after ?? string("after") }
    static func noProject(_ labels: MonarchTimesheetDiffLabels) -> String {
        labels.noProject ?? string("noProject")
    }
    static func nonBillable(_ labels: MonarchTimesheetDiffLabels) -> String {
        labels.nonBillable ?? string("nonBillable")
    }
    static func noChanges(_ labels: MonarchTimesheetDiffLabels) -> String {
        labels.noChanges ?? string("noChanges")
    }
    static func totals(_ total: String, billable: String?, _ labels: MonarchTimesheetDiffLabels) -> String {
        if let custom = labels.totals { return custom(total, billable) }
        guard let billable else { return string("total", total) }
        return string("totalBillable", total, billable)
    }
    static func increase(_ duration: String) -> String { string("increase", duration) }
    static func decrease(_ duration: String) -> String { string("decrease", duration) }
    static var unchanged: String { string("unchanged") }
}
