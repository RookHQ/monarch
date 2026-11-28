/// How a ``MonarchCheckboxGroup`` arranges its choices.
public enum MonarchCheckboxGroupLayout: Sendable, Hashable, CaseIterable {
    /// One choice per line. The default.
    case vertical
    /// The choices side by side, wrapping onto more lines when they do not
    /// fit. At the accessibility text sizes they are stacked instead.
    case horizontal
}
