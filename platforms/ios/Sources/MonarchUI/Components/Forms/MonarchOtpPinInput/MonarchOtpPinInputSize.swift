/// The size of the cells of a one-time code input.
public enum MonarchOtpPinInputSize: Sendable, Hashable, CaseIterable {
    /// Cells 44 points wide and high. The default.
    case md
    /// Cells 52 points wide and 48 points high.
    case lg
}
