import SwiftUI

/// What one joined button reports to its group.
struct MonarchButtonGroupSegment {
    /// The visible frame of the button, without the extra touch target.
    let bounds: Anchor<CGRect>
    /// The fill for the button's state, or `nil` for no fill.
    let fill: MonarchColorRole?
}

/// Collects the joined buttons of a group, in order.
struct MonarchButtonGroupSegmentsKey: PreferenceKey {
    static var defaultValue: [MonarchButtonGroupSegment] { [] }

    static func reduce(value: inout [MonarchButtonGroupSegment], nextValue: () -> [MonarchButtonGroupSegment]) {
        value.append(contentsOf: nextValue())
    }
}

/// A rounded rectangle at a fixed frame inside a larger view. The group draws
/// with it because the frames come from the buttons, not from the layout.
struct MonarchButtonGroupOutline: InsettableShape {
    let frame: CGRect
    let cornerRadius: CGFloat
    var insetAmount: CGFloat = 0

    func path(in rect: CGRect) -> Path {
        Path(
            roundedRect: frame.insetBy(dx: insetAmount, dy: insetAmount),
            cornerRadius: max(cornerRadius - insetAmount, 0),
            style: .continuous
        )
    }

    func inset(by amount: CGFloat) -> MonarchButtonGroupOutline {
        var copy = self
        copy.insetAmount += amount
        return copy
    }
}

/// Several plain rectangles at fixed frames, for the lines between buttons.
struct MonarchButtonGroupRectangles: Shape {
    let frames: [CGRect]

    func path(in rect: CGRect) -> Path {
        var path = Path()
        for frame in frames {
            path.addRect(frame)
        }
        return path
    }
}

/// The joined shape behind the buttons of an attached group: one fill per
/// button, clipped to rounded outer corners, the lines between the buttons
/// and the border.
struct MonarchButtonGroupBackground: View {
    let segments: [MonarchButtonGroupSegment]
    let variant: MonarchButtonVariant
    let axis: Axis

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        GeometryReader { proxy in
            if let geometry = MonarchButtonGroupTokens.geometry(
                frames: segments.map { proxy[$0.bounds] },
                axis: axis
            ) {
                let outline = MonarchButtonGroupOutline(
                    frame: geometry.outline,
                    cornerRadius: MonarchButtonGroupTokens.cornerRadius
                )
                ZStack {
                    ForEach(Array(segments.enumerated()), id: \.offset) { index, segment in
                        if let fill = segment.fill {
                            MonarchButtonGroupRectangles(frames: [geometry.fills[index]])
                                .fill(theme.colors[role: fill])
                        }
                    }
                    MonarchButtonGroupRectangles(frames: geometry.dividers)
                        .fill(theme.colors[role: MonarchButtonGroupTokens.divider(variant: variant)])
                }
                .clipShape(outline)
                if let border = MonarchButtonGroupTokens.border(variant: variant) {
                    outline.strokeBorder(theme.colors[role: border], lineWidth: MonarchButtonGroupTokens.hairline)
                }
            }
        }
        .monarchAnimation(.fast, value: segments.map(\.fill))
        .accessibilityHidden(true)
    }
}
