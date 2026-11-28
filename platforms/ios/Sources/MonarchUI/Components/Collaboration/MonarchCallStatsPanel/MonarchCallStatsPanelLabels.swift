/// Replacements for the built-in strings of ``MonarchCallStatsPanel``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchCallStatsPanelLabels: Sendable {
    /// The heading. Built in: "Data used this call".
    public var dataUsed: String?
    /// The usage line, from the total, the upload rate and the download rate.
    /// Built in: "15 MB total · up 420 kbps · down 1.3 Mbps".
    public var usageSummary: (@Sendable (String, String, String) -> String)?
    /// The low data note. Built in: "Low data mode is on: audio only."
    public var lowData: String?
    /// The spoken name of the list of people. Built in: "Connection to each person".
    public var connections: String?
    /// The round-trip label. Built in: "Round trip".
    public var roundTrip: String?
    /// The packet loss label. Built in: "Loss".
    public var loss: String?
    /// Shown when no one else is on the call. Built in: "No one else is connected yet."
    public var noPeers: String?
    /// The names of the grades, also spoken for a number not measured.
    /// Built in: "Good connection", "Unstable connection", "Poor connection", "Not measured yet".
    public var qualityNames: [MonarchCallStatsPanelQuality: String]

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        dataUsed: String? = nil,
        usageSummary: (@Sendable (String, String, String) -> String)? = nil,
        lowData: String? = nil,
        connections: String? = nil,
        roundTrip: String? = nil,
        loss: String? = nil,
        noPeers: String? = nil,
        qualityNames: [MonarchCallStatsPanelQuality: String] = [:]
    ) {
        self.dataUsed = dataUsed
        self.usageSummary = usageSummary
        self.lowData = lowData
        self.connections = connections
        self.roundTrip = roundTrip
        self.loss = loss
        self.noPeers = noPeers
        self.qualityNames = qualityNames
    }
}
