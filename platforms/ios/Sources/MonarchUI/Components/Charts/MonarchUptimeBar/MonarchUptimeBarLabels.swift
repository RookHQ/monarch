/// Replacements for the built-in strings of ``MonarchUptimeBar``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchUptimeBarLabels: Sendable {
    /// Names of the states, in the details, the legend and what VoiceOver reads.
    /// Built in: "Operational", "Degraded", "Partial outage", "Down", "Maintenance", "No data".
    public var statusNames: [MonarchUptimeBarStatus: String]
    /// The name of the uptime value. Built in: "Uptime".
    public var uptime: String?
    /// The overall uptime under the bar, from the formatted percentage. Built in: "99.95% uptime".
    public var overall: (@Sendable (String) -> String)?
    /// The name of the period axis for VoiceOver's chart details. Built in: "Period".
    public var period: String?
    /// The name of the state axis for VoiceOver's chart details. Built in: "Status".
    public var status: String?
    /// The spoken name of the legend. Built in: "Legend".
    public var legend: String?
    /// The spoken summary, from the number of periods, the count of each
    /// state and the formatted overall uptime. Built in: "Periods: 30.
    /// Operational 28, Down 2. Uptime 99.95%."
    public var summary: (@Sendable (Int, [MonarchUptimeBarStatus: Int], String?) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        statusNames: [MonarchUptimeBarStatus: String] = [:],
        uptime: String? = nil,
        overall: (@Sendable (String) -> String)? = nil,
        period: String? = nil,
        status: String? = nil,
        legend: String? = nil,
        summary: (@Sendable (Int, [MonarchUptimeBarStatus: Int], String?) -> String)? = nil
    ) {
        self.statusNames = statusNames
        self.uptime = uptime
        self.overall = overall
        self.period = period
        self.status = status
        self.legend = legend
        self.summary = summary
    }
}
