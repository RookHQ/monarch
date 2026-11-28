import SwiftUI

/// A track of segments with one raised segment: the Monarch segmented control.
///
/// For two to four plain-text choices the system `Picker` with
/// `.pickerStyle(.segmented)` is the platform's segmented control and stays
/// the right answer. Use this one when a segment needs an icon, a count or a
/// longer label, when there are more segments than fit, or when the control
/// must look the same on every OS version.
///
/// ```swift
/// MonarchSegmentedControl(
///     "Tickets",
///     selection: $filter,
///     items: [
///         MonarchSegmentedItem("Open", value: Filter.open, count: "12"),
///         MonarchSegmentedItem("Closed", value: Filter.closed),
///     ]
/// )
/// ```
///
/// Accessibility: the control is a container named by `label`; each segment
/// is a button with the selected trait, named by its title and count. The
/// selected segment is marked by a raised, bordered surface as well as by
/// colour. Each segment is at least 44 points high and wide, and the text
/// scales with Dynamic Type. When the segments do not fit, the control
/// scrolls sideways.
public struct MonarchSegmentedControl<Value: Hashable>: View {
    private let label: String
    @Binding private var selection: Value
    private let items: [MonarchSegmentedItem<Value>]
    private let size: MonarchButtonSize
    private let fillsWidth: Bool
    private let wraps: Bool

    @Namespace private var pill
    @Environment(\.monarchTheme) private var theme

    /// A segmented control.
    ///
    /// - Parameters:
    ///   - label: The spoken name of the control, for example "Ticket filter". It is not shown.
    ///   - selection: The value of the selected segment.
    ///   - items: The segments, in order.
    ///   - size: 32, 36 or 40 points high.
    ///   - fillsWidth: Whether the segments share the whole width, or take only what they need.
    ///   - wraps: Whether segments that do not fit one row continue on a second row of the same track,
    ///     so every segment stays in view, instead of scrolling sideways.
    public init(
        _ label: String,
        selection: Binding<Value>,
        items: [MonarchSegmentedItem<Value>],
        size: MonarchButtonSize = .md,
        fillsWidth: Bool = true,
        wraps: Bool = false
    ) {
        self.label = label
        _selection = selection
        self.items = items
        self.size = size
        self.fillsWidth = fillsWidth
        self.wraps = wraps
    }

    /// The content of the view.
    public var body: some View {
        ViewThatFits(in: .horizontal) {
            track
            if wraps {
                wrappedTrack
            }
            ScrollViewReader { proxy in
                ScrollView(.horizontal, showsIndicators: false) { track }
                    .onChange(of: selection) { _, selected in proxy.scrollTo(selected) }
                    // A soft edge where the segments run on, so the cut is not abrupt.
                    .mask(
                        LinearGradient(
                            stops: [
                                .init(color: .black, location: 0),
                                .init(color: .black, location: 0.9),
                                .init(color: .clear, location: 1),
                            ],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
            }
        }
        .frame(maxWidth: fillsWidth ? .infinity : nil, alignment: .leading)
        .monarchAnimation(.fast, value: selection)
        .sensoryFeedback(.selection, trigger: selection)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(label)
    }

    private var wrappedTrack: some View {
        let half = (items.count + 1) / 2
        return VStack(spacing: MonarchSegmentedControlTokens.spacing) {
            segments(Array(items.prefix(half)), pads: 0, fills: true)
            segments(Array(items.dropFirst(half)), pads: half - (items.count - half), fills: true)
        }
        .padding(MonarchSegmentedControlTokens.trackPadding)
        .background(
            theme.colors[role: MonarchSegmentedControlTokens.trackColor],
            in: MonarchSegmentedControlTokens.trackShape
        )
        .frame(maxWidth: .infinity)
    }

    private var track: some View {
        segments(items, pads: 0, fills: fillsWidth)
            .padding(MonarchSegmentedControlTokens.trackPadding)
            .background(
                theme.colors[role: MonarchSegmentedControlTokens.trackColor],
                in: MonarchSegmentedControlTokens.trackShape
            )
            .frame(maxWidth: fillsWidth ? .infinity : nil)
    }

    private func segments(_ list: [MonarchSegmentedItem<Value>], pads: Int, fills: Bool) -> some View {
        HStack(spacing: MonarchSegmentedControlTokens.spacing) {
            ForEach(list) { item in
                let isSelected = item.value == selection
                Button {
                    selection = item.value
                } label: {
                    MonarchSegmentLabel(
                        item: item,
                        size: size,
                        isSelected: isSelected,
                        fillsWidth: fills,
                        pill: pill
                    )
                }
                .buttonStyle(.plain)
                .disabled(item.isDisabled)
                .accessibilityLabel(
                    MonarchSegmentedControlTokens.accessibilityLabel(title: item.title, count: item.count)
                )
                .accessibilityAddTraits(isSelected ? .isSelected : [])
                .id(item.value)
            }
            ForEach(0..<max(pads, 0), id: \.self) { _ in Color.clear.frame(maxWidth: .infinity) }
        }
    }
}

/// One segment. A separate view, so it reads whether the control is enabled.
private struct MonarchSegmentLabel<Value: Hashable>: View {
    let item: MonarchSegmentedItem<Value>
    let size: MonarchButtonSize
    let isSelected: Bool
    let fillsWidth: Bool
    let pill: Namespace.ID

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let color = MonarchSegmentedControlTokens.contentColor(isSelected: isSelected, isEnabled: isEnabled)
        let iconOnly = MonarchSegmentedControlTokens.showsIconOnly(
            isIconOnly: item.isIconOnly,
            systemImage: item.systemImage
        )
        HStack(spacing: MonarchSegmentedControlTokens.contentSpacing) {
            if let image = item.systemImage {
                MonarchIcon(systemName: image, size: .sm)
            }
            if !iconOnly {
                Text(item.title)
                    .monarchTextStyle(
                        MonarchSegmentedControlTokens.textStyle(size),
                        weight: MonarchSegmentedControlTokens.fontWeight
                    )
                    .lineLimit(1)
                    .fixedSize()
            }
            if MonarchSegmentedControlTokens.showsCount(item.count), !iconOnly {
                Text(item.count ?? "")
                    .monarchTextStyle(.caption)
                    .monospacedDigit()
            }
        }
        .foregroundStyle(theme.colors[role: color])
        .padding(.horizontal, MonarchSegmentedControlTokens.horizontalPadding(size))
        .frame(maxWidth: fillsWidth ? .infinity : nil, minHeight: MonarchSegmentedControlTokens.segmentHeight(size))
        .background {
            if isSelected {
                Color.clear
                    .monarchSurface(
                        MonarchSegmentedControlTokens.pillAppearance(isDark: theme.isDark),
                        in: MonarchSegmentedControlTokens.segmentShape
                    )
                    .monarchElevation(
                        MonarchSegmentedControlTokens.pillElevation(isDark: theme.isDark),
                        in: MonarchSegmentedControlTokens.segmentShape
                    )
                    .matchedGeometryEffect(id: "pill", in: pill)
            }
        }
        .monarchMinimumHitTarget()
    }
}

#if DEBUG
    #Preview("MonarchSegmentedControl") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchSegmentedControl(
                    "View",
                    selection: .constant("list"),
                    items: [
                        MonarchSegmentedItem("List", value: "list", systemImage: "list.bullet"),
                        MonarchSegmentedItem("Board", value: "board", systemImage: "rectangle.split.3x1"),
                        MonarchSegmentedItem("Grid", value: "grid", systemImage: "square.grid.2x2"),
                    ]
                )
                MonarchSegmentedControl(
                    "Range",
                    selection: .constant(7),
                    items: [1, 7, 30, 90].map { MonarchSegmentedItem("\($0) days", value: $0, isDisabled: $0 == 90) },
                    size: .sm
                )
            }
        }
    }
#endif
