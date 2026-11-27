/// The strings of ``MonarchShiftRoster``: the app's replacements, or the built-in strings.
struct MonarchShiftRosterText {
    let labels: MonarchShiftRosterLabels

    var roster: String { labels.roster ?? MonarchShiftRosterStrings.roster }
    var person: String { labels.person ?? MonarchShiftRosterStrings.person }
    var day: String { labels.day ?? MonarchShiftRosterStrings.day }
    var off: String { labels.off ?? MonarchShiftRosterStrings.off }
    var unassigned: String { labels.unassigned ?? MonarchShiftRosterStrings.unassigned }
    var clear: String { labels.clear ?? MonarchShiftRosterStrings.clear }
    var overnight: String { labels.overnight ?? MonarchShiftRosterStrings.overnight }
    var hours: String { labels.hours ?? MonarchShiftRosterStrings.hours }
    var working: String { labels.working ?? MonarchShiftRosterStrings.working }
    var today: String { labels.today ?? MonarchShiftRosterStrings.today }
    var emptyTitle: String { labels.emptyTitle ?? MonarchShiftRosterStrings.emptyTitle }
    var emptyDescription: String { labels.emptyDescription ?? MonarchShiftRosterStrings.emptyDescription }
    var loading: String { labels.loading ?? MonarchShiftRosterStrings.loading }

    func menuTitle(_ name: String, _ day: String) -> String {
        labels.menuTitle?(name, day) ?? MonarchShiftRosterStrings.menuTitle(name, day)
    }

    func cell(_ name: String, _ day: String, _ assignment: String) -> String {
        labels.cell?(name, day, assignment) ?? MonarchShiftRosterStrings.cell(name, day, assignment)
    }

    func range(_ start: String, _ end: String) -> String {
        labels.range?(start, end) ?? MonarchShiftRosterStrings.range(start, end)
    }

    func hoursValue(_ hours: String) -> String {
        labels.hoursValue?(hours) ?? MonarchShiftRosterStrings.hoursValue(hours)
    }

    /// "3 working". `formatted` is the count in the locale's digits.
    func workingValue(_ count: Int, formatted: String) -> String {
        if let custom = labels.workingValue { return custom(count) }
        return count == 1 ? MonarchShiftRosterStrings.workingOne : MonarchShiftRosterStrings.workingOther(formatted)
    }
}
