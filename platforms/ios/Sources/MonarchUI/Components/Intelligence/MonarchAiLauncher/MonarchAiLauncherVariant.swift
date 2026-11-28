/// How strongly an assistant launcher is drawn.
public enum MonarchAiLauncherVariant: Sendable, Hashable, CaseIterable {
    /// A round, filled, raised button that floats over the content. The default.
    case solid
    /// A quieter, tinted button with its label, for a toolbar or a rail.
    case subtle
}
