/// How full the context window is, for ``MonarchAiContextWindowMeter``.
public enum MonarchAiContextWindowState: Sendable, Hashable, CaseIterable {
    /// Below the warning share: there is room left.
    case ok
    /// At or above the warning share: earlier messages may soon be left out.
    case warning
    /// At or over the limit: earlier messages are being left out.
    case full

    /// The state for an amount, a window size and a warning share.
    ///
    /// `full` at or over the limit, `warning` at or above `warningAt`
    /// (0 to 1), otherwise `ok`. A limit of zero or less is always `ok`.
    public static func of(used: Int, limit: Int, warningAt: Double = 0.8) -> MonarchAiContextWindowState {
        guard limit > 0 else { return .ok }
        let share = Double(max(0, used)) / Double(limit)
        if share >= 1 { return .full }
        if share >= warningAt { return .warning }
        return .ok
    }
}
