/// Which ratings of a ``MonarchAiFeedback`` open the sheet with reasons and a comment.
public enum MonarchAiFeedbackDetails: Sendable, Hashable, CaseIterable {
    /// Only a thumbs down. A thumbs up is sent at once. The default.
    case down
    /// Both ratings.
    case both
    /// Neither: every rating is sent at once.
    case none
}
