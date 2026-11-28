import MonarchTokens

/// The curve of a transition.
public enum MonarchMotionEasing: Sendable, Hashable, CaseIterable {
    /// Changes that stay on screen.
    case standard
    /// Elements that appear.
    case enter
    /// Elements that leave.
    case exit
    /// A small overshoot for emphasis.
    case emphasized

    /// The control points of the curve.
    public var curve: MonarchCubicBezier {
        switch self {
        case .standard: MonarchTokens.Easing.standard
        case .enter: MonarchTokens.Easing.enter
        case .exit: MonarchTokens.Easing.exit
        case .emphasized: MonarchTokens.Easing.emphasized
        }
    }
}
