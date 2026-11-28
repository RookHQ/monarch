/// One period of a ``MonarchUptimeBar``, usually a day.
public struct MonarchUptimeBarSegment: Sendable, Hashable, Identifiable {
    /// A stable identity, such as the ISO date.
    public let id: String
    /// The period as text, such as "Mar 4". Shown in the details and read by VoiceOver.
    public let label: String
    /// The state, or `nil` to derive it from `uptime` with the bar's thresholds.
    public let status: MonarchUptimeBarStatus?
    /// The share of checks that passed, in percent (0 to 100), or `nil` when nothing was checked.
    public let uptime: Double?
    /// Extra lines for the details.
    public let details: [MonarchUptimeBarDetail]
    /// A short note, such as the title of an incident.
    public let note: String?

    /// A period.
    ///
    /// - Parameters:
    ///   - id: A stable identity, such as the ISO date.
    ///   - label: The period as text, already localised.
    ///   - status: The state, or `nil` to derive it from `uptime`.
    ///   - uptime: The share of checks that passed, in percent, or `nil` when nothing was checked.
    ///   - details: Extra lines for the details.
    ///   - note: A short note, such as the title of an incident.
    public init(
        id: String,
        label: String,
        status: MonarchUptimeBarStatus? = nil,
        uptime: Double? = nil,
        details: [MonarchUptimeBarDetail] = [],
        note: String? = nil
    ) {
        self.id = id
        self.label = label
        self.status = status
        self.uptime = uptime
        self.details = details
        self.note = note
    }
}
