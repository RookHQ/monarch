import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchDragHandle``.
enum MonarchDragHandleTokens {
    /// The side of the grip glyph before Dynamic Type is applied.
    static let glyphSize: CGFloat = MonarchTokens.Space.space4
    /// The side of the grid the dots are placed on.
    static let gridUnits: CGFloat = 16
    /// The radius of a dot, in grid units.
    static let dotRadius: CGFloat = 1.25
    /// The grid positions the dots sit on, along and across the axis.
    private static let three: [CGFloat] = [4, 8, 12]
    private static let two: [CGFloat] = [6, 10]

    /// The shape of the visible handle.
    static var shape: RoundedRectangle {
        RoundedRectangle(cornerRadius: MonarchTokens.Radius.sm, style: .continuous)
    }

    /// The side of the visible handle before Dynamic Type is applied.
    static func boxSize(_ size: MonarchDragHandleSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space6
        case .md: MonarchTokens.Control.sm
        }
    }

    /// The centres of the grip dots on the 16-unit grid.
    static func dots(axis: MonarchDragHandleAxis) -> [CGPoint] {
        let columns: [CGFloat]
        let rows: [CGFloat]
        switch axis {
        case .vertical:
            columns = two
            rows = three
        case .horizontal:
            columns = three
            rows = two
        case .both:
            columns = three
            rows = three
        }
        return columns.flatMap { x in rows.map { y in CGPoint(x: x, y: y) } }
    }

    /// The colour roles for a state. The grabbed handle has a border, so
    /// the state is visible without colour.
    static func appearance(state: MonarchDragHandleState, isEnabled: Bool) -> MonarchSurfaceAppearance {
        guard isEnabled else {
            return MonarchSurfaceAppearance(background: nil, foreground: \.textDisabled, border: nil)
        }
        switch state {
        case .idle:
            return MonarchSurfaceAppearance(background: nil, foreground: \.textMuted, border: nil)
        case .grabbed:
            return MonarchSurfaceAppearance(
                background: \.accentSubtle,
                foreground: \.accentStrong,
                border: \.accentStrong
            )
        case .dropped:
            return MonarchSurfaceAppearance(background: \.successSubtle, foreground: \.successStrong, border: nil)
        }
    }

    /// The accessibility actions an axis offers, in order.
    static func directions(axis: MonarchDragHandleAxis) -> [MonarchDragHandleDirection] {
        switch axis {
        case .vertical: [.up, .down]
        case .horizontal: [.backward, .forward]
        case .both: [.up, .down, .backward, .forward]
        }
    }

    /// The name of the action for a direction: the override if there is a
    /// non-empty one, otherwise the built-in string.
    static func actionName(
        _ direction: MonarchDragHandleDirection,
        overrides: [MonarchDragHandleDirection: String] = [:]
    ) -> String {
        if let override = overrides[direction], !override.isEmpty { return override }
        return switch direction {
        case .up: MonarchDragHandleStrings.moveUp
        case .down: MonarchDragHandleStrings.moveDown
        case .backward: MonarchDragHandleStrings.moveBackward
        case .forward: MonarchDragHandleStrings.moveForward
        }
    }

    /// What VoiceOver reads as the value: nothing at rest, otherwise the
    /// override or the built-in name of the state.
    static func accessibilityValue(
        state: MonarchDragHandleState,
        overrides: [MonarchDragHandleState: String] = [:]
    ) -> String {
        if state == .idle { return "" }
        if let override = overrides[state], !override.isEmpty { return override }
        return state == .grabbed ? MonarchDragHandleStrings.grabbed : MonarchDragHandleStrings.dropped
    }
}
