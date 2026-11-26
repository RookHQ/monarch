import SwiftUI

/// Related buttons shown as one control, such as Previous and Next or a set
/// of zoom controls.
///
/// Put standard `Button`s in it. The group gives them the Monarch look and,
/// when attached, joins them into one shape with a hairline between them.
///
/// ```swift
/// MonarchButtonGroup("Zoom") {
///     Button("Zoom out", systemImage: "minus") { zoomOut() }
///     Button("Zoom in", systemImage: "plus") { zoomIn() }
/// }
/// ```
///
/// The group sets the button style of its content. A ``MonarchButton`` inside
/// leaves its style to the group, so it is joined like a standard `Button`.
/// Another view that sets its own style keeps it and is not joined.
///
/// Accessibility: the group is a container with the label you give it, so
/// VoiceOver reads "Zoom" before the first button. Every button stays its own
/// element with a touch target of at least 44 by 44 points.
public struct MonarchButtonGroup<Content: View>: View {
    private let label: String?
    private let axis: Axis
    private let variant: MonarchButtonVariant
    private let size: MonarchButtonSize
    private let isAttached: Bool
    private let fullWidth: Bool
    private let content: Content

    /// A button group.
    ///
    /// - Parameters:
    ///   - label: What the buttons have in common, read by VoiceOver. Already localised.
    ///   - axis: A row or a column.
    ///   - variant: The visual weight of every button.
    ///   - size: The height and padding of every button.
    ///   - isAttached: Whether the buttons are joined into one shape. When
    ///     `false` they keep a small gap.
    ///   - fullWidth: Whether the group fills the available width. In a row
    ///     the buttons share it.
    ///   - content: The buttons.
    public init(
        _ label: String? = nil,
        axis: Axis = .horizontal,
        variant: MonarchButtonVariant = .secondary,
        size: MonarchButtonSize = .md,
        isAttached: Bool = true,
        fullWidth: Bool = false,
        @ViewBuilder content: () -> Content
    ) {
        self.label = label
        self.axis = axis
        self.variant = variant
        self.size = size
        self.isAttached = isAttached
        self.fullWidth = fullWidth
        self.content = content()
    }

    /// The content of the view.
    public var body: some View {
        buttons
            .accessibilityElement(children: .contain)
            .accessibilityLabel(label ?? "")
    }

    @ViewBuilder private var buttons: some View {
        if isAttached {
            stack(spacing: 0)
                .buttonStyle(
                    MonarchButtonGroupSegmentStyle(variant: variant, size: size, axis: axis, fullWidth: fullWidth)
                )
                .backgroundPreferenceValue(MonarchButtonGroupSegmentsKey.self) { segments in
                    MonarchButtonGroupBackground(segments: segments, variant: variant, axis: axis)
                }
                .fixedSize(horizontal: axis == .vertical && !fullWidth, vertical: false)
        } else {
            stack(spacing: MonarchButtonGroupTokens.gap)
                .buttonStyle(.monarch(variant, size: size, fullWidth: fullWidth))
        }
    }

    private func stack(spacing: CGFloat) -> some View {
        let layout =
            axis == .horizontal
            ? AnyLayout(HStackLayout(spacing: spacing)) : AnyLayout(VStackLayout(spacing: spacing))
        return layout { content }
            .monarchButtonDefersStyle()
    }
}

#if DEBUG
    #Preview("MonarchButtonGroup") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchButtonVariant.allCases, id: \.self) { variant in
                    MonarchButtonGroup("Pages", variant: variant) {
                        Button("Previous") {}
                        Button("Next") {}
                        Button("Last") {}.disabled(true)
                    }
                }
                MonarchButtonGroup("Zoom", axis: .vertical) {
                    Button("Zoom in", systemImage: "plus") {}
                    Button("Zoom out", systemImage: "minus") {}
                }
                .labelStyle(.iconOnly)
                MonarchButtonGroup("Pages", isAttached: false, fullWidth: true) {
                    Button("Previous") {}
                    Button("Next") {}
                }
            }
        }
    }
#endif
