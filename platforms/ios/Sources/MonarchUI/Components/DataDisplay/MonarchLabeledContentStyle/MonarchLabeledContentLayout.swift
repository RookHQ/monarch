/// How ``MonarchLabeledContentStyle`` places a label and its value.
public enum MonarchLabeledContentLayout: Sendable, Hashable, CaseIterable {
    /// The label above the value. The default, and the layout for long values.
    case stacked
    /// The label at the leading edge and the value at the trailing edge of
    /// one row. Falls back to stacked when the two do not fit side by side.
    case inline
}
