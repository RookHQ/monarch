import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchButtonGroup``.
enum MonarchButtonGroupTokens {
    /// The gap between buttons that are not attached.
    static let gap: CGFloat = MonarchTokens.Space.space2
    /// The corner radius of the joined shape.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The width of the border and of the line between two buttons.
    static let hairline: CGFloat = MonarchTokens.Space.px
    /// The least width of a joined button, so each one is a full touch target.
    static let minSegmentWidth: CGFloat = MonarchAccessibility.minimumHitTarget

    /// The least height of the visible part of a joined button. In a column
    /// the buttons touch, so each is as tall as the touch target.
    static func minSegmentHeight(_ size: MonarchButtonSize, axis: Axis) -> CGFloat {
        switch axis {
        case .horizontal: MonarchButtonTokens.minHeight(size)
        case .vertical: max(MonarchButtonTokens.minHeight(size), MonarchAccessibility.minimumHitTarget)
        }
    }

    /// The border around the joined shape, or `nil` for none.
    static func border(variant: MonarchButtonVariant) -> MonarchColorRole? {
        switch variant {
        case .secondary: \.borderDefault
        case .dangerSubtle: \.dangerBorder
        case .primary, .ghost, .danger, .success, .warning: nil
        }
    }

    /// The line between two joined buttons.
    static func divider(variant: MonarchButtonVariant) -> MonarchColorRole {
        switch variant {
        case .secondary: \.borderDefault
        case .primary, .danger, .success, .warning: \.bgSurface
        case .dangerSubtle: \.dangerBorder
        case .ghost: \.borderSubtle
        }
    }

    /// Where the joined shape, the fill of each button and the lines between
    /// them are drawn.
    struct Geometry: Equatable {
        /// The whole joined shape.
        let outline: CGRect
        /// One fill per button, in the order the frames were given.
        let fills: [CGRect]
        /// One hairline between each pair of neighbours.
        let dividers: [CGRect]
    }

    /// The geometry for the visible frames of the buttons, or `nil` when
    /// there are none. Pure, so it is tested without a view.
    ///
    /// Every fill spans the whole group across the axis, so buttons of
    /// different heights still make one shape. A hairline is centred on the
    /// start of every button except the first along the axis.
    static func geometry(frames: [CGRect], axis: Axis) -> Geometry? {
        guard let first = frames.first else { return nil }
        let outline = frames.dropFirst().reduce(first) { $0.union($1) }
        let fills = frames.map { frame in
            switch axis {
            case .horizontal:
                CGRect(x: frame.minX, y: outline.minY, width: frame.width, height: outline.height)
            case .vertical:
                CGRect(x: outline.minX, y: frame.minY, width: outline.width, height: frame.height)
            }
        }
        let dividers: [CGRect] =
            switch axis {
            case .horizontal:
                fills.map(\.minX).sorted().dropFirst().map { edge in
                    CGRect(x: edge - hairline / 2, y: outline.minY, width: hairline, height: outline.height)
                }
            case .vertical:
                fills.map(\.minY).sorted().dropFirst().map { edge in
                    CGRect(x: outline.minX, y: edge - hairline / 2, width: outline.width, height: hairline)
                }
            }
        return Geometry(outline: outline, fills: fills, dividers: dividers)
    }
}
