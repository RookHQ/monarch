import SwiftUI

/// The names and colours of a chart's series, optionally as toggles that
/// show and hide each series.
///
/// ```swift
/// MonarchChartLegend([
///     MonarchChartLegendItem("revenue", label: "Revenue", value: "$48k"),
///     MonarchChartLegendItem("target", label: "Target", color: .neutral, isDashed: true),
/// ])
///
/// MonarchChartLegend(series, hiddenSeries: $hidden, marker: .square)
/// ```
///
/// Colours are the theme's categorical data slots, given in order (the
/// sixth series and later are neutral), so a legend matches the chart that
/// uses the same order. Entries wrap onto new rows.
///
/// With `hiddenSeries`, each entry is a toggle: a hidden series has a grey
/// marker and a struck-through label, and a Show all button appears. The
/// last visible series cannot be hidden unless `keepsOneVisible` is off.
///
/// Accessibility: the legend is a container named "Legend". A plain entry is
/// one element read as its label and value; the marker is decorative. A
/// toggle entry is read as a switch that is on while the series is shown.
/// Labels scale with Dynamic Type and toggles keep a 44-point target.
public struct MonarchChartLegend: View {
    private let items: [MonarchChartLegendItem]
    private let hiddenSeries: Binding<Set<String>>?
    private let marker: MonarchChartLegendMarker
    private let axis: Axis
    private let keepsOneVisible: Bool
    private let showsShowAll: Bool
    private let legendLabel: String?
    private let showAllLabel: String?

    @Environment(\.isEnabled) private var isEnabled

    /// A legend that only names the series.
    ///
    /// - Parameters:
    ///   - items: The entries, in series order.
    ///   - marker: The mark beside each label.
    ///   - axis: A wrapping row, or a column.
    ///   - legendLabel: Replaces the built-in spoken name "Legend".
    public init(
        _ items: [MonarchChartLegendItem],
        marker: MonarchChartLegendMarker = .line,
        axis: Axis = .horizontal,
        legendLabel: String? = nil
    ) {
        self.items = items
        self.hiddenSeries = nil
        self.marker = marker
        self.axis = axis
        self.keepsOneVisible = true
        self.showsShowAll = false
        self.legendLabel = legendLabel
        self.showAllLabel = nil
    }

    /// A legend whose entries show and hide their series.
    ///
    /// - Parameters:
    ///   - items: The entries, in series order.
    ///   - hiddenSeries: The keys of the hidden series.
    ///   - marker: The mark beside each label.
    ///   - axis: A wrapping row, or a column.
    ///   - keepsOneVisible: Whether the last visible series cannot be hidden.
    ///   - showsShowAll: Whether a Show all button appears while a series is hidden.
    ///   - legendLabel: Replaces the built-in spoken name "Legend".
    ///   - showAllLabel: Replaces the built-in button title "Show all".
    public init(
        _ items: [MonarchChartLegendItem],
        hiddenSeries: Binding<Set<String>>,
        marker: MonarchChartLegendMarker = .line,
        axis: Axis = .horizontal,
        keepsOneVisible: Bool = true,
        showsShowAll: Bool = true,
        legendLabel: String? = nil,
        showAllLabel: String? = nil
    ) {
        self.items = items
        self.hiddenSeries = hiddenSeries
        self.marker = marker
        self.axis = axis
        self.keepsOneVisible = keepsOneVisible
        self.showsShowAll = showsShowAll
        self.legendLabel = legendLabel
        self.showAllLabel = showAllLabel
    }

    /// The content of the view.
    public var body: some View {
        let layout =
            axis == .horizontal
            ? AnyLayout(MonarchFlowLayout(spacing: MonarchChartLegendTokens.entrySpacing))
            : AnyLayout(VStackLayout(alignment: .leading, spacing: MonarchChartLegendTokens.columnSpacing))
        layout {
            ForEach(Array(items.enumerated()), id: \.element.key) { index, item in
                if let hiddenSeries {
                    toggle(item, index: index, hidden: hiddenSeries)
                } else {
                    MonarchChartLegendEntry(item: item, index: index, marker: marker, isHidden: false)
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel(
                            MonarchChartLegendTokens.accessibilityLabel(label: item.label, value: item.value)
                        )
                }
            }
            if let hiddenSeries, showsShowAll,
                MonarchChartLegendTokens.showsShowAll(
                    hidden: hiddenSeries.wrappedValue,
                    keys: keys,
                    isEnabled: isEnabled
                )
            {
                MonarchButton(showAllLabel ?? MonarchChartLegendStrings.showAll, variant: .ghost, size: .sm) {
                    hiddenSeries.wrappedValue = MonarchChartLegendTokens.showingAll(
                        hiddenSeries.wrappedValue,
                        keys: keys
                    )
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(legendLabel ?? MonarchChartLegendStrings.legend)
    }

    private var keys: [String] { items.map(\.key) }

    private func toggle(_ item: MonarchChartLegendItem, index: Int, hidden: Binding<Set<String>>) -> some View {
        let isHidden = hidden.wrappedValue.contains(item.key)
        let isVisible = Binding(
            get: { !hidden.wrappedValue.contains(item.key) },
            set: {
                hidden.wrappedValue = MonarchChartLegendTokens.toggled(
                    hidden.wrappedValue,
                    key: item.key,
                    isVisible: $0,
                    keys: keys,
                    keepsOneVisible: keepsOneVisible
                )
            }
        )
        return Toggle(isOn: isVisible) {
            MonarchChartLegendEntry(item: item, index: index, marker: marker, isHidden: isHidden)
        }
        .toggleStyle(MonarchChartLegendToggleStyle())
        .accessibilityLabel(MonarchChartLegendTokens.accessibilityLabel(label: item.label, value: item.value))
    }
}

/// The marker, label and value of one entry.
private struct MonarchChartLegendEntry: View {
    let item: MonarchChartLegendItem
    let index: Int
    let marker: MonarchChartLegendMarker
    let isHidden: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let color = MonarchChartLegendTokens.color(item.color, index: index)
        HStack(spacing: MonarchChartLegendTokens.contentSpacing) {
            MonarchChartLegendSwatch(
                marker: marker,
                role: MonarchChartLegendTokens.markerRole(color, isHidden: isHidden || !isEnabled),
                isDashed: item.isDashed
            )
            Text(item.label)
                .strikethrough(isHidden)
                .foregroundStyle(
                    theme.colors[
                        role: isEnabled ? MonarchChartLegendTokens.labelRole(isHidden: isHidden) : \.textDisabled
                    ]
                )
                .multilineTextAlignment(.leading)
            if let value = item.value, !value.isEmpty {
                Text(value)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors[role: isEnabled ? \.textDefault : \.textDisabled])
            }
        }
        .monarchTextStyle(MonarchChartLegendTokens.textStyle)
    }
}

/// A toggle drawn as its label with a pressed background. VoiceOver is
/// given the standard toggle.
private struct MonarchChartLegendToggleStyle: ToggleStyle {
    func makeBody(configuration: Configuration) -> some View {
        Button {
            configuration.isOn.toggle()
        } label: {
            configuration.label
        }
        .buttonStyle(MonarchChartLegendPressStyle())
        .accessibilityRepresentation {
            Toggle(isOn: configuration.$isOn) { configuration.label }
        }
    }
}

/// The pressed state of a toggle entry.
private struct MonarchChartLegendPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        MonarchChartLegendPressBody(configuration: configuration)
    }
}

/// Reads the theme, which a style value cannot do itself.
private struct MonarchChartLegendPressBody: View {
    let configuration: ButtonStyleConfiguration

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        configuration.label
            .padding(.horizontal, MonarchChartLegendTokens.togglePadding)
            .monarchMinimumHitTarget()
            .background {
                if configuration.isPressed {
                    MonarchShapes.mark.fill(theme.colors.bgMuted)
                }
            }
            .contentShape(Rectangle())
    }
}

#if DEBUG
    #Preview("MonarchChartLegend") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchChartLegendMarker.allCases, id: \.self) { marker in
                    MonarchChartLegend(
                        [
                            MonarchChartLegendItem("revenue", label: "Revenue", value: "$48k"),
                            MonarchChartLegendItem("costs", label: "Costs"),
                            MonarchChartLegendItem("target", label: "Target", color: .neutral, isDashed: true),
                        ],
                        marker: marker
                    )
                }
                MonarchChartLegend(
                    [
                        MonarchChartLegendItem("web", label: "Web"),
                        MonarchChartLegendItem("ios", label: "iOS"),
                    ],
                    hiddenSeries: .constant(["ios"])
                )
            }
        }
    }
#endif
