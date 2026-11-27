import Foundation

/// Built-in strings of the timesheet grid, from the `MonarchTimesheetGrid`
/// strings table, with the replacements of a labels value applied.
enum MonarchTimesheetGridStrings {
    private static let table = "MonarchTimesheetGrid"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.timesheetGrid." + key, table: table)
    }

    private static func string(_ key: String, _ arguments: String...) -> String {
        String(format: string(key), arguments: arguments)
    }

    static func caption(_ labels: MonarchTimesheetGridLabels) -> String { labels.caption ?? string("caption") }
    static func rowHeader(_ labels: MonarchTimesheetGridLabels) -> String { labels.rowHeader ?? string("rowHeader") }
    static func total(_ labels: MonarchTimesheetGridLabels) -> String { labels.total ?? string("total") }
    static func dayTotal(_ labels: MonarchTimesheetGridLabels) -> String { labels.dayTotal ?? string("dayTotal") }
    static func rowActions(_ row: String, _ labels: MonarchTimesheetGridLabels) -> String {
        labels.rowActions?(row) ?? string("rowActions", row)
    }
    static func cellLabel(_ row: String, _ day: String, _ labels: MonarchTimesheetGridLabels) -> String {
        labels.cellLabel?(row, day) ?? string("cellLabel", row, day)
    }
    static func locked(_ labels: MonarchTimesheetGridLabels) -> String { labels.locked ?? string("locked") }
    static func noTime(_ labels: MonarchTimesheetGridLabels) -> String { labels.noTime ?? string("noTime") }
    static func invalid(_ labels: MonarchTimesheetGridLabels) -> String { labels.invalid ?? string("invalid") }
    static func tooLong(_ maximum: String, _ labels: MonarchTimesheetGridLabels) -> String {
        labels.tooLong?(maximum) ?? string("tooLong", maximum)
    }
    static func saveFailed(_ labels: MonarchTimesheetGridLabels) -> String {
        labels.saveFailed ?? string("saveFailed")
    }
    static func expected(_ duration: String, _ labels: MonarchTimesheetGridLabels) -> String {
        labels.expected?(duration) ?? string("expected", duration)
    }
    static func overLimit(_ labels: MonarchTimesheetGridLabels) -> String { labels.overLimit ?? string("overLimit") }
    static func empty(_ labels: MonarchTimesheetGridLabels) -> String { labels.empty ?? string("empty") }
    static func today(_ labels: MonarchTimesheetGridLabels) -> String { labels.today ?? string("today") }
    static func saving(_ labels: MonarchTimesheetGridLabels) -> String { labels.saving ?? string("saving") }
    static func previousDay(_ labels: MonarchTimesheetGridLabels) -> String {
        labels.previousDay ?? string("previousDay")
    }
    static func nextDay(_ labels: MonarchTimesheetGridLabels) -> String { labels.nextDay ?? string("nextDay") }
    static func rowTotal(_ duration: String, _ labels: MonarchTimesheetGridLabels) -> String {
        labels.rowTotal?(duration) ?? string("rowTotal", duration)
    }
    static func error(_ message: String) -> String { string("error", message) }
}
