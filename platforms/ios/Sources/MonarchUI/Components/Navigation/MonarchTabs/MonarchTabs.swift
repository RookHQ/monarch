import SwiftUI

/// A strip of tabs that switches between views of related content in the
/// same place. SwiftUI has no tab strip for the top of a screen.
///
/// The strip owns the tabs and the selection. The app shows the content for
/// the selection below it.
///
/// ```swift
/// MonarchTabs(
///     "Ticket filters",
///     selection: $filter,
///     items: [
///         MonarchTabsItem("Open", value: Filter.open, count: "12"),
///         MonarchTabsItem("Closed", value: Filter.closed),
///     ]
/// )
/// ```
///
/// For two to four short, equal choices in a form or a toolbar, use the
/// system `Picker` with `.pickerStyle(.segmented)`. For the top-level
/// destinations of an app, use `TabView`.
///
/// Accessibility: the strip is a tab bar named by `label`, and each tab is a
/// button with the selected trait, so VoiceOver reads "Open, 12, selected,
/// tab, 1 of 2". The count is part of the tab's name. The selected tab is
/// marked by a line or a bordered pill as well as by colour. Each tab has a
/// touch target of at least 44 points and labels scale with Dynamic Type.
/// Labels stay on one line: when the tabs do not fit the width, the strip
/// scrolls sideways and brings a newly selected tab into view. The indicator
/// moves without animation when Reduce Motion is on.
public struct MonarchTabs<Value: Hashable>: View {
    private let label: String
    @Binding private var selection: Value
    private let items: [MonarchTabsItem<Value>]
    private let variant: MonarchTabsVariant

    @Namespace private var indicator
    @Environment(\.monarchTheme) private var theme

    /// A tab strip.
    ///
    /// - Parameters:
    ///   - label: The spoken name of the strip, for example "Project sections".
    ///   - selection: The value of the selected tab.
    ///   - items: The tabs, in order.
    ///   - variant: A line under the selected tab, or a pill on a track.
    public init(
        _ label: String,
        selection: Binding<Value>,
        items: [MonarchTabsItem<Value>],
        variant: MonarchTabsVariant = .line
    ) {
        self.label = label
        _selection = selection
        self.items = items
        self.variant = variant
    }

    /// The content of the view.
    public var body: some View {
        Group {
            switch variant {
            case .line:
                tabs
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(alignment: .bottom) {
                        Rectangle()
                            .fill(theme.colors[role: MonarchTabsTokens.borderColor])
                            .frame(height: MonarchTabsTokens.borderWidth)
                    }
            case .pill:
                tabs
                    .padding(MonarchTabsTokens.trackPadding)
                    .background(theme.colors[role: MonarchTabsTokens.trackColor], in: MonarchTabsTokens.shape)
                    .clipShape(MonarchTabsTokens.shape)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .monarchAnimation(.fast, value: selection)
        .sensoryFeedback(.selection, trigger: selection)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(label)
        .accessibilityAddTraits(.isTabBar)
    }

    /// The strip at its natural width when it fits, and in a sideways scroll view when it does not.
    private var tabs: some View {
        ViewThatFits(in: .horizontal) {
            strip(isTight: false)
            // Tabs that only just do not fit lose some of their side padding before the strip scrolls.
            strip(isTight: true)
            ScrollViewReader { proxy in
                ScrollView(.horizontal, showsIndicators: false) {
                    strip(isTight: false)
                }
                .onChange(of: selection) { _, selected in
                    proxy.scrollTo(selected)
                }
            }
        }
    }

    private func strip(isTight: Bool) -> some View {
        HStack(spacing: MonarchTabsTokens.spacing(variant)) {
            ForEach(items) { item in
                let isSelected = item.value == selection
                Button {
                    selection = item.value
                } label: {
                    MonarchTabsLabel(
                        item: item,
                        variant: variant,
                        isSelected: isSelected,
                        isTight: isTight,
                        indicator: indicator
                    )
                }
                .buttonStyle(.plain)
                .disabled(item.isDisabled)
                .accessibilityLabel(MonarchTabsTokens.accessibilityLabel(title: item.title, count: item.count))
                .accessibilityAddTraits(isSelected ? .isSelected : [])
                .id(item.value)
            }
        }
    }
}

/// The label of one tab. A separate view, so it reads whether the tab is enabled.
private struct MonarchTabsLabel<Value: Hashable>: View {
    let item: MonarchTabsItem<Value>
    let variant: MonarchTabsVariant
    let isSelected: Bool
    let isTight: Bool
    let indicator: Namespace.ID

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let color = MonarchTabsTokens.labelColor(isSelected: isSelected, isEnabled: isEnabled)
        HStack(spacing: MonarchTabsTokens.countSpacing) {
            Text(item.title)
                .monarchTextStyle(MonarchTabsTokens.textStyle)
                .fixedSize()
            if let count = item.count, MonarchTabsTokens.showsCount(count) {
                MonarchBadge(count, tone: item.countTone)
                    .fixedSize()
            }
        }
        .foregroundStyle(theme.colors[role: color])
        .padding(.horizontal, isTight ? MonarchTabsTokens.tightHorizontalPadding : MonarchTabsTokens.horizontalPadding)
        .frame(minHeight: MonarchTabsTokens.minHeight(variant))
        .background {
            if variant == .pill, isSelected {
                Color.clear
                    .monarchSurface(MonarchTabsTokens.pillAppearance, in: MonarchTabsTokens.shape)
                    .matchedGeometryEffect(id: "selection", in: indicator)
            }
        }
        .monarchMinimumHitTarget()
        .overlay(alignment: .bottom) {
            if variant == .line, isSelected {
                Rectangle()
                    .fill(theme.colors[role: MonarchTabsTokens.indicatorColor])
                    .frame(height: MonarchTabsTokens.indicatorHeight)
                    .matchedGeometryEffect(id: "selection", in: indicator)
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchTabs") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchTabsVariant.allCases, id: \.self) { variant in
                    MonarchTabs(
                        "Tickets",
                        selection: .constant("open"),
                        items: [
                            MonarchTabsItem("Open", value: "open", count: "12", countTone: .info),
                            MonarchTabsItem("Closed", value: "closed"),
                            MonarchTabsItem("Archived", value: "archived", isDisabled: true),
                        ],
                        variant: variant
                    )
                }
            }
        }
    }
#endif
