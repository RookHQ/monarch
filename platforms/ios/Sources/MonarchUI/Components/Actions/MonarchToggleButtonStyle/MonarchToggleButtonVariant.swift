/// How a toggle button is drawn while it is off.
public enum MonarchToggleButtonVariant: Sendable, Hashable, CaseIterable {
    /// No fill and no border until it is on. The default.
    case ghost
    /// A bordered surface, also while off.
    case outline
}
