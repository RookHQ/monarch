/// A swipe action of a ``MonarchInboxRow``, placed on the side Mail uses.
public enum MonarchInboxRowSwipeAction {
    /// Marks the conversation read or unread. On the leading edge; a full
    /// swipe runs it. Named after the row's current state.
    case toggleRead(@MainActor () -> Void)
    /// Archives the conversation. On the trailing edge; a full swipe runs
    /// it when it is the first trailing action.
    case archive(@MainActor () -> Void)
    /// Deletes the conversation. On the trailing edge, with the destructive role.
    case delete(@MainActor () -> Void)
}
