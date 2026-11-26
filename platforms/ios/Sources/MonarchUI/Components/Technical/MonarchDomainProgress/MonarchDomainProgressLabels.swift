/// Replacements for the built-in strings of ``MonarchDomainProgress``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchDomainProgressLabels: Sendable {
    /// The title of a step. Receives the step, its state and the stage.
    /// Built in: "DNS record found", "Ownership record needed", "Waiting for
    /// your DNS record", "HTTPS certificate issued", "HTTPS certificate", "Live".
    public var title: (@Sendable (MonarchDomainStep, MonarchDomainStepState, MonarchDomainStage) -> String)?
    /// The detail under a step. Receives the step, its state and the host.
    /// Built in: such as "Being issued now, usually 1 to 5 minutes."
    public var detail: (@Sendable (MonarchDomainStep, MonarchDomainStepState, String) -> String)?
    /// The spoken state of a step. Built in: "done", "in progress",
    /// "needs attention", "not started".
    public var state: (@Sendable (MonarchDomainStepState) -> String)?
    /// The spoken name of the list of steps. Receives the host. Built in:
    /// "Progress for book.example.com".
    public var listLabel: (@Sendable (String) -> String)?
    /// Announced when the active step changes. Receives the position (from
    /// 1), the count, the title and the spoken state. Built in:
    /// "Step 2 of 3: HTTPS certificate, in progress."
    public var announcement: (@Sendable (Int, Int, String, String) -> String)?
    /// The DNS line. Receives the host and what was found. Built in:
    /// "Public DNS for book.example.com shows CNAME → domains.example.net."
    public var dnsSeen: (@Sendable (String, String) -> String)?
    /// Used when public DNS shows no record. Built in: "no record yet".
    public var noRecord: String?
    /// The "keeps checking" sentence. Receives how the user is told.
    public var rechecking: (@Sendable (MonarchDomainNotify?) -> String)?
    /// The last check line. Receives the relative time. Built in:
    /// "Checked 2 minutes ago."
    public var checked: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: (@Sendable (MonarchDomainStep, MonarchDomainStepState, MonarchDomainStage) -> String)? = nil,
        detail: (@Sendable (MonarchDomainStep, MonarchDomainStepState, String) -> String)? = nil,
        state: (@Sendable (MonarchDomainStepState) -> String)? = nil,
        listLabel: (@Sendable (String) -> String)? = nil,
        announcement: (@Sendable (Int, Int, String, String) -> String)? = nil,
        dnsSeen: (@Sendable (String, String) -> String)? = nil,
        noRecord: String? = nil,
        rechecking: (@Sendable (MonarchDomainNotify?) -> String)? = nil,
        checked: (@Sendable (String) -> String)? = nil
    ) {
        self.title = title
        self.detail = detail
        self.state = state
        self.listLabel = listLabel
        self.announcement = announcement
        self.dnsSeen = dnsSeen
        self.noRecord = noRecord
        self.rechecking = rechecking
        self.checked = checked
    }

    func titleText(_ step: MonarchDomainStep, state: MonarchDomainStepState, stage: MonarchDomainStage) -> String {
        title?(step, state, stage) ?? MonarchDomainProgressStrings.title(step, state: state, stage: stage)
    }

    func detailText(_ step: MonarchDomainStep, state: MonarchDomainStepState, host: String) -> String {
        detail?(step, state, host) ?? MonarchDomainProgressStrings.detail(step, state: state, host: host)
    }

    func stateText(_ value: MonarchDomainStepState) -> String {
        state?(value) ?? MonarchDomainProgressStrings.state(value)
    }

    func listLabelText(_ host: String) -> String {
        listLabel?(host) ?? MonarchDomainProgressStrings.string("listLabel", host)
    }

    func announcementText(position: Int, count: Int, title: String, state: String) -> String {
        announcement?(position, count, title, state)
            ?? MonarchDomainProgressStrings.string("announcement", position, count, title, state)
    }

    func dnsSeenText(host: String, seen: String) -> String {
        dnsSeen?(host, seen) ?? MonarchDomainProgressStrings.string("dnsSeen", host, seen)
    }

    var resolvedNoRecord: String { noRecord ?? MonarchDomainProgressStrings.string("noRecord") }

    func recheckingText(_ notify: MonarchDomainNotify?) -> String {
        rechecking?(notify) ?? MonarchDomainProgressStrings.rechecking(notify)
    }

    func checkedText(_ time: String) -> String {
        checked?(time) ?? MonarchDomainProgressStrings.string("checked", time)
    }
}
