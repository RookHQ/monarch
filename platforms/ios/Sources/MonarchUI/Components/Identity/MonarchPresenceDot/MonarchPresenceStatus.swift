/// A person's presence.
public enum MonarchPresenceStatus: Sendable, Hashable, CaseIterable {
    /// Available now.
    case online
    /// Signed in but not active.
    case away
    /// In a call or a meeting.
    case busy
    /// Does not want notifications. Shares the busy colour and adds a bar.
    case doNotDisturb
    /// Not signed in.
    case offline
}
