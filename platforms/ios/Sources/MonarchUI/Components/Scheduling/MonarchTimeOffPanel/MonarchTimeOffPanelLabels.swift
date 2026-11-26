/// Replacements for the built-in strings of ``MonarchTimeOffPanel``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchTimeOffPanelLabels: Sendable {
    /// The panel's heading. Built in: "Time off".
    public var title: String?
    /// The text under the heading. Built in: "Holidays, sickness, training or closures. Nobody is booked in these periods.".
    public var message: String?
    /// The button that opens the add form. Built in: "Add time off".
    public var add: String?
    /// The checkbox that shows past entries. Built in: "Include past".
    public var includePast: String?
    /// The badge of a closure. Built in: "Closure".
    public var closure: String?
    /// Who a closure applies to, in the list and in Who. Built in: "Everyone".
    public var everyone: String?
    /// The badge of an entry that has ended. Built in: "Past".
    public var past: String?
    /// Written after the dates of an all-day entry. Built in: "all day".
    public var allDay: String?
    /// The remove button of an entry, from who and when. Built in: "Remove time off for Ana Lima, Mon, Nov 9, 2026 (all day)".
    public var remove: (@Sendable (String, String) -> String)?
    /// The switch that makes the new entry whole days. Built in: "All day".
    public var allDayToggle: String?
    /// The title of the remove confirmation. Built in: "Remove this time off?".
    public var removeTitle: String?
    /// Why removing matters, in the confirmation. Built in: "Those times become available again if they fall within working hours.".
    public var removeMessage: String?
    /// The button that confirms removing. Built in: "Remove".
    public var removeConfirm: String?
    /// The title when nothing is planned. Built in: "No time off planned".
    public var emptyTitle: String?
    /// The message when nothing is planned. Built in: "Add holidays or closures so nobody is offered times when no one is available.".
    public var emptyMessage: String?
    /// The retry button of the error. Built in: "Try again".
    public var retry: String?
    /// The spoken name of the placeholder rows. Built in: "Loading time off".
    public var loading: String?
    /// The title of the add form. Built in: "Add time off".
    public var formTitle: String?
    /// The note under the dates of the add form, before the time zone. Built in: "Dates and times are in this time zone:".
    public var formMessage: String?
    /// The picker of the person. Built in: "Who".
    public var who: String?
    /// The picker of the kind of time off. Built in: "Type".
    public var type: String?
    /// The notice when Everyone is chosen. Built in: "A closure blocks bookings for everyone.".
    public var closureNotice: String?
    /// The first day of an all-day entry. Built in: "From".
    public var from: String?
    /// The last day of an all-day entry. Built in: "Until (inclusive)".
    public var until: String?
    /// The start of a timed entry. Built in: "Start".
    public var start: String?
    /// The end of a timed entry. Built in: "End".
    public var end: String?
    /// The reason field. Built in: "Reason (optional)".
    public var reason: String?
    /// The note under the reason field. Built in: "Visible to your team.".
    public var reasonHelper: String?
    /// The button that leaves the form or the confirmation. Built in: "Cancel".
    public var cancel: String?
    /// The button that adds the entry. Built in: "Add".
    public var save: String?
    /// The error when the end is not after the start. Built in: "The end must be after the start.".
    public var endBeforeStart: String?
    /// The error when adding or removing fails without a description. Built in: "That did not work. Try again.".
    public var failed: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        message: String? = nil,
        add: String? = nil,
        includePast: String? = nil,
        closure: String? = nil,
        everyone: String? = nil,
        past: String? = nil,
        allDay: String? = nil,
        remove: (@Sendable (String, String) -> String)? = nil,
        allDayToggle: String? = nil,
        removeTitle: String? = nil,
        removeMessage: String? = nil,
        removeConfirm: String? = nil,
        emptyTitle: String? = nil,
        emptyMessage: String? = nil,
        retry: String? = nil,
        loading: String? = nil,
        formTitle: String? = nil,
        formMessage: String? = nil,
        who: String? = nil,
        type: String? = nil,
        closureNotice: String? = nil,
        from: String? = nil,
        until: String? = nil,
        start: String? = nil,
        end: String? = nil,
        reason: String? = nil,
        reasonHelper: String? = nil,
        cancel: String? = nil,
        save: String? = nil,
        endBeforeStart: String? = nil,
        failed: String? = nil
    ) {
        self.title = title
        self.message = message
        self.add = add
        self.includePast = includePast
        self.closure = closure
        self.everyone = everyone
        self.past = past
        self.allDay = allDay
        self.remove = remove
        self.allDayToggle = allDayToggle
        self.removeTitle = removeTitle
        self.removeMessage = removeMessage
        self.removeConfirm = removeConfirm
        self.emptyTitle = emptyTitle
        self.emptyMessage = emptyMessage
        self.retry = retry
        self.loading = loading
        self.formTitle = formTitle
        self.formMessage = formMessage
        self.who = who
        self.type = type
        self.closureNotice = closureNotice
        self.from = from
        self.until = until
        self.start = start
        self.end = end
        self.reason = reason
        self.reasonHelper = reasonHelper
        self.cancel = cancel
        self.save = save
        self.endBeforeStart = endBeforeStart
        self.failed = failed
    }
}
