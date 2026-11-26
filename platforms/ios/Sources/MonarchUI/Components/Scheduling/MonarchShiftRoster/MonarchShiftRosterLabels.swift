/// Replacements for the built-in strings of ``MonarchShiftRoster``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchShiftRosterLabels: Sendable {
    /// The name of the roster read by VoiceOver. Built in: "Shift roster".
    public var roster: String?
    /// The header of the people column in the grid and the name of the person tabs. Built in: "Person".
    public var person: String?
    /// The name of the day tabs. Built in: "Day".
    public var day: String?
    /// A day off. Built in: "Off".
    public var off: String?
    /// A day with no assignment. Built in: "Not assigned".
    public var unassigned: String?
    /// The menu item that removes an assignment. Built in: "Clear".
    public var clear: String?
    /// The heading of a cell's menu, from the person's name and the day. Built in:
    /// "Alex Kim, Wednesday, November 4".
    public var menuTitle: (@Sendable (String, String) -> String)?
    /// What VoiceOver reads for a cell, from the person's name, the day and the assignment.
    /// Built in: "Alex Kim, Wednesday, November 4: Morning, 6:00 AM to 2:00 PM".
    public var cell: (@Sendable (String, String, String) -> String)?
    /// A shift's start and end as VoiceOver reads them. Built in: "6:00 AM to 2:00 PM".
    public var range: (@Sendable (String, String) -> String)?
    /// Added to a shift that ends the next day. Built in: "overnight".
    public var overnight: String?
    /// The header of the hours column and the name of a person's total. Built in: "Hours".
    public var hours: String?
    /// A total of hours, from the number already rounded to one decimal. Built in: "37.5 h".
    public var hoursValue: (@Sendable (String) -> String)?
    /// The label of the grid's working-count row. Built in: "Working".
    public var working: String?
    /// The number of people working on a day. Built in: "1 working", "3 working".
    public var workingValue: (@Sendable (Int) -> String)?
    /// The marker of today. Built in: "Today".
    public var today: String?
    /// The title of the empty state. Built in: "No one to schedule".
    public var emptyTitle: String?
    /// The message of the empty state. Built in: "People appear here with their shift for each day."
    public var emptyDescription: String?
    /// What VoiceOver reads while the roster loads. Built in: "Loading roster".
    public var loading: String?

    /// Replacements for built-in strings. Every parameter is optional.
    ///
    /// - Parameters:
    ///   - roster: The name of the roster read by VoiceOver.
    ///   - person: The header of the people column and the name of the person tabs.
    ///   - day: The name of the day tabs.
    ///   - off: A day off.
    ///   - unassigned: A day with no assignment.
    ///   - clear: The menu item that removes an assignment.
    ///   - menuTitle: The heading of a cell's menu, from the person's name and the day.
    ///   - cell: What VoiceOver reads for a cell, from the name, the day and the assignment.
    ///   - range: A shift's start and end as VoiceOver reads them.
    ///   - overnight: Added to a shift that ends the next day.
    ///   - hours: The header of the hours column and the name of a person's total.
    ///   - hoursValue: A total of hours, from the rounded number.
    ///   - working: The label of the grid's working-count row.
    ///   - workingValue: The number of people working on a day.
    ///   - today: The marker of today.
    ///   - emptyTitle: The title of the empty state.
    ///   - emptyDescription: The message of the empty state.
    ///   - loading: What VoiceOver reads while the roster loads.
    public init(
        roster: String? = nil,
        person: String? = nil,
        day: String? = nil,
        off: String? = nil,
        unassigned: String? = nil,
        clear: String? = nil,
        menuTitle: (@Sendable (String, String) -> String)? = nil,
        cell: (@Sendable (String, String, String) -> String)? = nil,
        range: (@Sendable (String, String) -> String)? = nil,
        overnight: String? = nil,
        hours: String? = nil,
        hoursValue: (@Sendable (String) -> String)? = nil,
        working: String? = nil,
        workingValue: (@Sendable (Int) -> String)? = nil,
        today: String? = nil,
        emptyTitle: String? = nil,
        emptyDescription: String? = nil,
        loading: String? = nil
    ) {
        self.roster = roster
        self.person = person
        self.day = day
        self.off = off
        self.unassigned = unassigned
        self.clear = clear
        self.menuTitle = menuTitle
        self.cell = cell
        self.range = range
        self.overnight = overnight
        self.hours = hours
        self.hoursValue = hoursValue
        self.working = working
        self.workingValue = workingValue
        self.today = today
        self.emptyTitle = emptyTitle
        self.emptyDescription = emptyDescription
        self.loading = loading
    }
}
