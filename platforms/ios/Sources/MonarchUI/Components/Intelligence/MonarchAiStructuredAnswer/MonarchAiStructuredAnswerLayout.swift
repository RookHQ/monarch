/// How ``MonarchAiStructuredAnswer`` lays out its fields.
public enum MonarchAiStructuredAnswerLayout: Sendable, Hashable, CaseIterable {
    /// One field per row: the label beside a short value, above a long one. The default.
    case list
    /// Each field as a small card, two to a row; one to a row at accessibility text sizes.
    case grid
}
