/// What the badge on an assistant launcher shows.
public enum MonarchAiLauncherBadge: Sendable, Hashable {
    /// A dot: there are new messages.
    case unread
    /// A ring that moves around the button: the assistant is working. The ring is still under Reduce Motion.
    case working
    /// A count of unread messages. Nothing is shown for zero or less; above 99 it reads "99+".
    case count(Int)
}
