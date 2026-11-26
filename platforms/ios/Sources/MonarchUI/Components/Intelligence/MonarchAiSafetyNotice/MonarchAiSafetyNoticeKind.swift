/// What a safety system did to a request or an answer, for ``MonarchAiSafetyNotice``.
public enum MonarchAiSafetyNoticeKind: String, Sendable, Hashable, CaseIterable {
    /// The request was not answered.
    case blocked
    /// The answer was changed before it was shown.
    case modified
    /// Parts of the answer were removed.
    case filtered
}
