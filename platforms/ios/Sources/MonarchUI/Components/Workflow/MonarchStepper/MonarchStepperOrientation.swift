/// The direction in which a stepper lists its steps.
public enum MonarchStepperOrientation: Sendable, Hashable, CaseIterable {
    /// Steps in a row, joined by lines. The default.
    case horizontal
    /// Steps in a column, for a side bar or a long setup list.
    case vertical
}
