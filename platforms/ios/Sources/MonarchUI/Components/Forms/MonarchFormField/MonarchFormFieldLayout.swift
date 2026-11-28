/// How a form field arranges its label, control and supporting text.
public enum MonarchFormFieldLayout: Sendable, Hashable, CaseIterable {
    /// The label above the control, the help or error text below it. The default.
    case stack
    /// The label and help text before the control, on one row, with the error
    /// below both: a switch or a compact picker with a description. At the
    /// accessibility text sizes the row becomes a stack so nothing is squeezed.
    case row
}
