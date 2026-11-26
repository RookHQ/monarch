/// The data a call has used so far, for a ``MonarchCallStatsPanel``.
public struct MonarchCallStatsPanelUsage: Sendable, Hashable {
    /// Bytes sent.
    public let sentBytes: Int64
    /// Bytes received.
    public let receivedBytes: Int64
    /// The current upload rate, in bits per second.
    public let upBitsPerSecond: Double?
    /// The current download rate, in bits per second.
    public let downBitsPerSecond: Double?

    /// Data use.
    ///
    /// - Parameters:
    ///   - sentBytes: Bytes sent.
    ///   - receivedBytes: Bytes received.
    ///   - upBitsPerSecond: The current upload rate, in bits per second.
    ///   - downBitsPerSecond: The current download rate, in bits per second.
    public init(
        sentBytes: Int64,
        receivedBytes: Int64,
        upBitsPerSecond: Double? = nil,
        downBitsPerSecond: Double? = nil
    ) {
        self.sentBytes = sentBytes
        self.receivedBytes = receivedBytes
        self.upBitsPerSecond = upBitsPerSecond
        self.downBitsPerSecond = downBitsPerSecond
    }
}
