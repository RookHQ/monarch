/// How participants are arranged on the call stage.
public enum MonarchCallControlsLayout: Sendable, Hashable, CaseIterable {
    /// The app chooses grid or speaker.
    case automatic
    /// Every participant in a grid.
    case grid
    /// The person speaking large, the others small.
    case speaker
}
