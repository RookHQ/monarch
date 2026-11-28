import SwiftUI

/// Values by country on a simplified world map: a choropleth in one data
/// colour with a "Less … More" legend, or bubbles sized by value.
///
/// ```swift
/// MonarchWorldMap(
///     "Workspaces by country",
///     data: [
///         MonarchWorldMapDatum("US", value: 1200, details: [MonarchWorldMapDetail("Users", value: "340")]),
///         MonarchWorldMapDatum("DE", value: 300),
///         MonarchWorldMapDatum("SG", value: 40),
///     ],
///     extras: [MonarchWorldMapExtra("Global", value: 75)]
/// )
/// ```
///
/// The map is drawn with a `Canvas` from a bundled, simplified Natural Earth
/// outline (1:110m, Antarctica left out), not a map SDK: MapKit does not fill
/// countries from data. Countries too small for an outline (Singapore,
/// Malta) get a dot. Values are binned into the heatmap's five steps;
/// codes the map cannot place are listed under it, and values that belong to
/// no country ("Global", "EU") are tiles under the map and rows of the table.
/// It sits in a ``MonarchChartFrame`` with loading, empty and error states
/// and "View as table". Tapping a country with a value outlines it and
/// writes its name, value and details under the map; with `onSelect`, the
/// tap also selects it, for drill-in.
///
/// Accessibility: the map is one adjustable element named by the generated
/// summary ("5 countries. Highest: United States 1,200, Germany 300, India
/// 150."). Swiping up and down moves through the countries, largest value
/// first, and reads each one; with `onSelect`, a double tap selects it. It
/// carries an accessibility chart descriptor for Audio Graphs. The legend is
/// read as its steps, and the table is always available. The map never
/// mirrors in right-to-left languages.
public struct MonarchWorldMap: View {
    private let title: String
    private let data: [MonarchWorldMapDatum]
    private let description: String?
    private let status: MonarchChartFrameStatus
    private let variant: MonarchWorldMapVariant
    private let color: MonarchChartColor
    private let thresholds: [Double]?
    private let extras: [MonarchWorldMapExtra]
    private let showsLegend: Bool
    private let summary: String?
    private let emptyMessage: String?
    private let footer: String?
    private let labels: MonarchWorldMapLabels
    private let countryName: ((String) -> String)?
    private let formatValue: ((Double) -> String)?
    private let onRetry: (@MainActor () -> Void)?
    private let onSelect: (@MainActor (MonarchWorldMapDatum) -> Void)?

    @State private var isTableShown = false
    @State private var active: String?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A world map.
    ///
    /// - Parameters:
    ///   - title: The name of the chart, already localised.
    ///   - data: One value per country. A later entry for the same code wins; values that are not finite are left out.
    ///   - description: One line under the title, such as the period.
    ///   - status: Whether the map, a placeholder, an empty state or an error shows.
    ///     A ready map without any value shows the empty state.
    ///   - variant: Filled countries, or bubbles on a neutral map.
    ///   - color: The data colour of the steps or bubbles.
    ///   - thresholds: Three ascending values where levels 2, 3 and 4 start. Quarters of the largest value when `nil`.
    ///   - extras: Values that belong to no country, shown as tiles under the map and in the table.
    ///   - showsLegend: Whether the "Less … More" legend shows under a choropleth.
    ///   - summary: Replaces the generated summary that VoiceOver reads for the map.
    ///   - emptyMessage: The empty-state message.
    ///   - footer: Notes under the chart, such as the source.
    ///   - labels: Replacements for the built-in strings.
    ///   - countryName: Names a country from its code. The default is the region name in the environment's locale.
    ///   - formatValue: Writes a value. The default is a number in the environment's locale.
    ///   - onRetry: Shows Try again in the error status and runs when it is pressed.
    ///   - onSelect: Runs with a country's datum when it is tapped or selected with VoiceOver.
    public init(
        _ title: String,
        data: [MonarchWorldMapDatum],
        description: String? = nil,
        status: MonarchChartFrameStatus = .ready,
        variant: MonarchWorldMapVariant = .choropleth,
        color: MonarchChartColor = .data1,
        thresholds: [Double]? = nil,
        extras: [MonarchWorldMapExtra] = [],
        showsLegend: Bool = true,
        summary: String? = nil,
        emptyMessage: String? = nil,
        footer: String? = nil,
        labels: MonarchWorldMapLabels = MonarchWorldMapLabels(),
        countryName: ((String) -> String)? = nil,
        formatValue: ((Double) -> String)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onSelect: (@MainActor (MonarchWorldMapDatum) -> Void)? = nil
    ) {
        self.title = title
        self.data = data
        self.description = description
        self.status = status
        self.variant = variant
        self.color = color
        self.thresholds = thresholds
        self.extras = extras
        self.showsLegend = showsLegend
        self.summary = summary
        self.emptyMessage = emptyMessage
        self.footer = footer
        self.labels = labels
        self.countryName = countryName
        self.formatValue = formatValue
        self.onRetry = onRetry
        self.onSelect = onSelect
    }

    /// The content of the view.
    public var body: some View {
        let rows = MonarchWorldMapTokens.rows(data)
        let resolved = MonarchWorldMapTokens.status(status, rows: rows, extras: extras)
        let showsTable = isTableShown && resolved == .ready
        MonarchChartFrame(
            title,
            description: description,
            status: resolved,
            emptyMessage: emptyMessage,
            footer: footer,
            height: MonarchWorldMapTokens.minimumHeight,
            labels: labels.frame,
            onRetry: onRetry,
            content: {
                if showsTable {
                    MonarchChartFrameTableView(
                        table: MonarchWorldMapTokens.table(
                            rows: rows,
                            extras: extras,
                            name: name,
                            format: format,
                            labels: labels
                        ),
                        noValue: noValue
                    )
                } else {
                    map(rows)
                }
            },
            actions: {
                if resolved == .ready {
                    MonarchButton(
                        MonarchChartFrameTokens.toggleTitle(showsTable: showsTable, labels: labels.frame),
                        variant: .ghost,
                        size: .sm
                    ) {
                        isTableShown.toggle()
                    }
                }
            }
        )
    }

    private var noValue: String {
        labels.frame.noValue ?? MonarchChartFrameStrings.noValue
    }

    private func format(_ value: Double) -> String {
        formatValue?(value) ?? value.formatted(.number.locale(locale))
    }

    private func name(_ code: String) -> String {
        MonarchWorldMapTokens.countryName(code, locale: locale, custom: countryName)
    }

    private func describe(_ datum: MonarchWorldMapDatum) -> String {
        MonarchWorldMapTokens.describe(name: name(datum.country), value: format(datum.value), details: datum.details)
    }

    private func map(_ rows: [MonarchWorldMapDatum]) -> some View {
        let placed = MonarchWorldMapTokens.placed(rows)
        let unplaced = MonarchWorldMapTokens.unplaced(rows)
        let scale = MonarchWorldMapTokens.scale(rows, thresholds: thresholds)
        let byCode = Dictionary(uniqueKeysWithValues: rows.map { ($0.country, $0) })
        let current = active.flatMap { code in placed.first { $0.country == code } }
        let index = current.flatMap { datum in placed.firstIndex { $0.country == datum.country } }
        let spoken =
            summary
            ?? MonarchWorldMapTokens.summary(
                count: rows.count,
                countText: rows.count.formatted(.number.locale(locale)),
                top: MonarchWorldMapTokens.top(rows, name: name, format: format),
                noValue: noValue,
                labels: labels
            )
        let note = MonarchWorldMapTokens.unplacedNote(unplaced, name: name, format: format, labels: labels)
        return VStack(alignment: .leading, spacing: MonarchWorldMapTokens.sectionSpacing) {
            if !rows.isEmpty {
                MonarchWorldMapPlot(
                    rows: byCode,
                    placed: placed,
                    variant: variant,
                    color: color,
                    scale: scale,
                    maximum: MonarchWorldMapTokens.maximum(rows),
                    active: current?.country,
                    onTap: { code in
                        active = code
                        if let code, let datum = byCode[code] { onSelect?(datum) }
                    }
                )
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(spoken)
                .accessibilityValue(current.map(describe) ?? "")
                .accessibilityAddTraits(onSelect == nil ? [] : .isButton)
                .accessibilityAdjustableAction { direction in
                    let next = MonarchWorldMapTokens.adjusted(
                        index,
                        forward: direction == .increment,
                        count: placed.count
                    )
                    active = next.map { placed[$0].country }
                }
                .accessibilityAction {
                    if let current { onSelect?(current) }
                }
                .accessibilityChartDescriptor(
                    MonarchChartFrameDescriptor(
                        title: title,
                        summary: spoken,
                        table: MonarchWorldMapTokens.table(
                            rows: rows,
                            extras: [],
                            name: name,
                            format: format,
                            labels: labels,
                            includesDetails: false
                        )
                    )
                )
                if let current {
                    Text(describe(current))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textDefault)
                        .accessibilityHidden(true)
                }
            }
            if !extras.isEmpty {
                MonarchWorldMapTiles(
                    extras: extras,
                    format: format,
                    label: labels.extras ?? MonarchWorldMapStrings.extras
                )
            }
            if !rows.isEmpty && showsLegend && variant == .choropleth {
                MonarchHeatmapLegend(
                    color: color,
                    showsNoData: false,
                    steps: MonarchHeatmapTokens.legendSteps(
                        scale: scale,
                        format: format,
                        showsNoData: false,
                        labels: MonarchWorldMapTokens.legendLabels(labels)
                    ),
                    labels: MonarchWorldMapTokens.legendLabels(labels)
                )
            }
            if let note {
                Text(note)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// The tiles under the map for values that belong to no country.
private struct MonarchWorldMapTiles: View {
    let extras: [MonarchWorldMapExtra]
    let format: (Double) -> String
    let label: String

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        MonarchFlowLayout(spacing: MonarchWorldMapTokens.tileSpacing) {
            ForEach(extras) { extra in
                HStack(spacing: MonarchWorldMapTokens.tileContentSpacing) {
                    Text(extra.label)
                        .foregroundStyle(theme.colors.textMuted)
                    Text(format(extra.value))
                        .monarchTextStyle(.caption, weight: .semibold)
                        .monospacedDigit()
                        .foregroundStyle(theme.colors.textDefault)
                }
                .monarchTextStyle(.caption)
                .padding(.horizontal, MonarchWorldMapTokens.tileHorizontalPadding)
                .padding(.vertical, MonarchWorldMapTokens.tileVerticalPadding)
                .background(MonarchShapes.control.fill(theme.colors.bgSubtle))
                .overlay(
                    MonarchShapes.control.strokeBorder(theme.colors.borderSubtle, lineWidth: MonarchShapes.borderWidth)
                )
                .accessibilityElement(children: .combine)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(label)
    }
}

#if DEBUG
    #Preview("MonarchWorldMap") {
        let data = [
            MonarchWorldMapDatum("US", value: 1_200, details: [MonarchWorldMapDetail("Users", value: "340")]),
            MonarchWorldMapDatum("DE", value: 300),
            MonarchWorldMapDatum("IN", value: 150),
            MonarchWorldMapDatum("BR", value: 90),
            MonarchWorldMapDatum("SG", value: 40),
            MonarchWorldMapDatum("XX", value: 5),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchWorldMap("Workspaces", data: data, extras: [MonarchWorldMapExtra("Global", value: 75)])
                MonarchWorldMap("Workspaces", data: data, variant: .bubble, color: .data3)
                MonarchWorldMap("Workspaces", data: [], status: .loading)
            }
        }
    }
#endif
