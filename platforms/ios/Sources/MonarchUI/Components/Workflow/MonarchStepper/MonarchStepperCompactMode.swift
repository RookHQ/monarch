/// When a stepper shows the compact form, "Step 2 of 5" with a progress
/// bar, in place of the list of steps.
public enum MonarchStepperCompactMode: Sendable, Hashable, CaseIterable {
    /// A horizontal stepper is compact when the row of steps does not fit
    /// the width; a vertical stepper is never compact. The default.
    case automatic
    /// Always compact.
    case always
    /// Never compact. Names wrap where the row does not fit.
    case never
}
