import Foundation

/// Built-in strings of ``MonarchWeeklyHoursEditor``, from the `MonarchWeeklyHoursEditor` strings table.
enum MonarchWeeklyHoursEditorStrings {
    private static let table = "MonarchWeeklyHoursEditor"

    /// The string for a key without placeholders, such as `title`.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.weeklyHoursEditor.\(key)", table: table)
    }

    /// The string for a key with placeholders, such as "Remove %@ interval %ld".
    static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }
}

/// The resolved strings of ``MonarchWeeklyHoursEditor``: the app's replacements, else the built-in ones.
struct MonarchWeeklyHoursEditorText {
    let labels: MonarchWeeklyHoursEditorLabels

    private func pick(_ value: String?, _ key: String) -> String {
        value ?? MonarchWeeklyHoursEditorStrings.string(key)
    }

    var title: String { pick(labels.title, "title") }
    var unavailable: String { pick(labels.unavailable, "unavailable") }
    var start: String { pick(labels.start, "start") }
    var end: String { pick(labels.end, "end") }
    var add: String { pick(labels.add, "add") }
    var remove: String { pick(labels.remove, "remove") }
    var copyToAll: String { pick(labels.copyToAll, "copyToAll") }
    var midnight: String { pick(labels.midnight, "midnight") }

    func interval(_ day: String, _ position: Int) -> String {
        labels.interval?(day, position) ?? MonarchWeeklyHoursEditorStrings.string("interval", day, position)
    }

    func addItem(_ day: String) -> String {
        labels.addItem?(day) ?? MonarchWeeklyHoursEditorStrings.string("addItem", day)
    }

    func removeItem(_ day: String, _ position: Int) -> String {
        labels.removeItem?(day, position) ?? MonarchWeeklyHoursEditorStrings.string("removeItem", day, position)
    }

    func copyToAllItem(_ day: String) -> String {
        labels.copyToAllItem?(day) ?? MonarchWeeklyHoursEditorStrings.string("copyToAllItem", day)
    }

    func copied(_ day: String) -> String {
        labels.copied?(day) ?? MonarchWeeklyHoursEditorStrings.string("copied", day)
    }

    func range(_ start: String, _ end: String) -> String {
        labels.range?(start, end) ?? MonarchWeeklyHoursEditorStrings.string("range", start, end)
    }

    func error(_ error: MonarchWeeklyHoursError) -> String {
        switch error {
        case .range: pick(labels.rangeError, "rangeError")
        case .order: pick(labels.orderError, "orderError")
        case .overlap: pick(labels.overlapError, "overlapError")
        }
    }

    /// A day's hours as text: "9:00 AM – 12:00 PM, 1:00 PM – 5:00 PM", or "Unavailable".
    func summary(_ ranges: [MonarchWeeklyHoursInterval], formats: MonarchWeeklyHoursEditorFormats) -> String {
        guard !ranges.isEmpty else { return unavailable }
        return ranges.map { range(formats.time($0.start), formats.time($0.end)) }
            .formatted(.list(type: .and, width: .narrow).locale(formats.locale))
    }
}
