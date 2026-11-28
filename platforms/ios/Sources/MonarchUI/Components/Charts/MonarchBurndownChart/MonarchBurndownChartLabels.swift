/// Replacements for the built-in strings of ``MonarchBurndownChart``.
/// Every value is optional; leave one out to keep the built-in English text.
public struct MonarchBurndownChartLabels: Sendable {
    /// The name of the actual line. Built in: "Remaining".
    public var remaining: String
    /// The name of the ideal line. Built in: "Ideal".
    public var ideal: String
    /// The name of the scope line. Built in: "Scope".
    public var scope: String
    /// The heading of the day column of the data table. Built in: "Day".
    public var day: String
    /// The label of the today marker. Built in: "Today".
    public var today: String
    /// The heading of the scope change column and the popover footer. Built in: "Scope change".
    public var scopeChange: String
    /// A scope change with its sign, from the formatted amount and whether it was added: "+5" or "−2".
    public var change: @Sendable (_ amount: String, _ added: Bool) -> String
    /// Where the sprint stands: "Mar 6: 24 remaining, ideal 17.8".
    public var status: @Sendable (_ day: String, _ remaining: String, _ ideal: String) -> String
    /// Appended to the status when ahead: "2 ahead of ideal".
    public var ahead: @Sendable (_ amount: String) -> String
    /// Appended to the status when behind: "6.2 behind ideal".
    public var behind: @Sendable (_ amount: String) -> String
    /// Appended to the status when on the ideal line. Built in: "on the ideal line".
    public var onTrack: String
    /// The scope sentence: "Scope 43, 5 added and 2 removed since the start".
    public var scopeSummary: @Sendable (_ scope: String, _ added: String, _ removed: String) -> String
    /// The summary when no day has a remaining value. Built in: "Not started".
    public var notStarted: String
    /// Replacements for the strings of the chart frame.
    public var frame: MonarchChartFrameLabels

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - remaining: Replaces "Remaining".
    ///   - ideal: Replaces "Ideal".
    ///   - scope: Replaces "Scope".
    ///   - day: Replaces "Day".
    ///   - today: Replaces "Today".
    ///   - scopeChange: Replaces "Scope change".
    ///   - change: Replaces "+…" and "−…".
    ///   - status: Replaces "…: … remaining, ideal …".
    ///   - ahead: Replaces "… ahead of ideal".
    ///   - behind: Replaces "… behind ideal".
    ///   - onTrack: Replaces "on the ideal line".
    ///   - scopeSummary: Replaces "Scope …, … added and … removed since the start".
    ///   - notStarted: Replaces "Not started".
    ///   - frame: Replacements for the strings of the chart frame.
    public init(
        remaining: String? = nil,
        ideal: String? = nil,
        scope: String? = nil,
        day: String? = nil,
        today: String? = nil,
        scopeChange: String? = nil,
        change: (@Sendable (_ amount: String, _ added: Bool) -> String)? = nil,
        status: (@Sendable (_ day: String, _ remaining: String, _ ideal: String) -> String)? = nil,
        ahead: (@Sendable (_ amount: String) -> String)? = nil,
        behind: (@Sendable (_ amount: String) -> String)? = nil,
        onTrack: String? = nil,
        scopeSummary: (@Sendable (_ scope: String, _ added: String, _ removed: String) -> String)? = nil,
        notStarted: String? = nil,
        frame: MonarchChartFrameLabels = MonarchChartFrameLabels()
    ) {
        typealias Strings = MonarchBurndownChartStrings
        self.remaining = remaining ?? Strings.remaining
        self.ideal = ideal ?? Strings.ideal
        self.scope = scope ?? Strings.scope
        self.day = day ?? Strings.day
        self.today = today ?? Strings.today
        self.scopeChange = scopeChange ?? Strings.scopeChange
        self.change = change ?? { Strings.change($0, added: $1) }
        self.status = status ?? { Strings.status(day: $0, remaining: $1, ideal: $2) }
        self.ahead = ahead ?? { Strings.ahead($0) }
        self.behind = behind ?? { Strings.behind($0) }
        self.onTrack = onTrack ?? Strings.onTrack
        self.scopeSummary = scopeSummary ?? { Strings.scopeSummary(scope: $0, added: $1, removed: $2) }
        self.notStarted = notStarted ?? Strings.notStarted
        self.frame = frame
    }

    /// The name of a line.
    func name(_ line: MonarchBurndownChartLine) -> String {
        switch line {
        case .remaining: remaining
        case .ideal: ideal
        case .scope: scope
        }
    }
}
