/// The connection numbers of one remote person in a ``MonarchCallStatsPanel``,
/// from the app's summary of its media statistics.
public struct MonarchCallStatsPanelPeer: Sendable, Hashable, Identifiable {
    /// A stable identity.
    public let id: String
    /// The person's name, already localised.
    public let name: String
    /// The round-trip time, in milliseconds, or `nil` when not measured.
    public let roundTripMilliseconds: Double?
    /// Packet loss on what you receive from them, in percent.
    public let inboundLossPercent: Double?
    /// Packet loss they report on what you send, in percent.
    public let outboundLossPercent: Double?
    /// The connection grade.
    public let quality: MonarchCallStatsPanelQuality

    /// A person's connection numbers.
    ///
    /// - Parameters:
    ///   - id: A stable identity.
    ///   - name: The person's name, already localised.
    ///   - roundTripMilliseconds: The round-trip time, in milliseconds.
    ///   - inboundLossPercent: Packet loss on what you receive, in percent.
    ///   - outboundLossPercent: Packet loss on what you send, in percent.
    ///   - quality: The connection grade.
    public init(
        id: String,
        name: String,
        roundTripMilliseconds: Double? = nil,
        inboundLossPercent: Double? = nil,
        outboundLossPercent: Double? = nil,
        quality: MonarchCallStatsPanelQuality = .unknown
    ) {
        self.id = id
        self.name = name
        self.roundTripMilliseconds = roundTripMilliseconds
        self.inboundLossPercent = inboundLossPercent
        self.outboundLossPercent = outboundLossPercent
        self.quality = quality
    }
}
