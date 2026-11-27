import Charts
import SwiftUI

/// Parts of one whole as a donut inside a ``MonarchChartFrame``: the total
/// in the middle, a legend of values and shares, and the data as a table.
///
/// ```swift
/// MonarchDonutChart(
///     "Assets by type",
///     segments: [
///         MonarchDonutChartSegment("laptop", label: "Laptops", value: 412),
///         MonarchDonutChartSegment("phone", label: "Phones", value: 268),
///         MonarchDonutChartSegment("monitor", label: "Monitors", value: 131),
///     ],
///     centerLabel: "assets"
/// )
/// ```
///
/// Drawn with Swift Charts (`SectorMark`). Slices take the theme's data
/// slots in order, largest first, separated by a small gap. With more than
/// `maxSegments` segments the smallest fold into a neutral "Other" slice;
/// the table still lists every segment. A share under 1% reads "<1%".
/// The legend sits beside the donut and moves under it when there is no
/// room. A total of 0 shows the empty state.
///
/// Accessibility: the donut is one element named by the title. Its value is
/// a generated summary ("Total 864. Laptops 412 (48%), …"), or `summary`,
/// and it carries an accessibility chart descriptor with one value per
/// segment (Audio Graphs). The centre text is part of that element. Each
/// legend entry is read as its label, value and share. "View as table"
/// shows every segment, the total, and the shares as text.
public struct MonarchDonutChart: View {
    private typealias Tokens = MonarchDonutChartTokens

    private let title: String
    private let segments: [MonarchDonutChartSegment]
    private let description: String?
    private let centerValue: String?
    private let centerLabel: String?
    private let summary: String?
    private let sort: MonarchDonutChartSort
    private let maxSegments: Int
    private let legendContent: MonarchDonutChartLegendContent
    private let status: MonarchChartFrameStatus
    private let size: CGFloat
    private let thickness: CGFloat?
    private let percentDigits: Int
    private let showsCenterValue: Bool
    private let showsLegend: Bool
    private let isTableToggleShown: Bool
    private let emptyMessage: String?
    private let footer: String?
    private let labels: MonarchDonutChartLabels
    private let formatValue: ((Double) -> String)?
    private let onRetry: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme

    /// A donut chart.
    ///
    /// - Parameters:
    ///   - title: The name of the chart, already localised.
    ///   - segments: The parts of the whole.
    ///   - description: One line under the title, such as what the whole is.
    ///   - centerValue: The large text in the middle. The formatted total when `nil`.
    ///   - centerLabel: Small text under the centre value, such as "assets".
    ///   - summary: Replaces the generated spoken summary.
    ///   - sort: Largest slice first, or the given order.
    ///   - maxSegments: The most slices drawn; beyond it the smallest fold
    ///     into Other. At least 2.
    ///   - legendContent: What each legend entry shows after its label.
    ///   - status: Whether the chart, a placeholder, an empty state or an
    ///     error shows. With a total of 0, ready shows empty.
    ///   - size: The outer diameter. 160 points by default.
    ///   - thickness: The ring's thickness. 22% of the diameter by default.
    ///   - percentDigits: Decimal places in shares.
    ///   - showsCenterValue: Whether the middle shows the centre value.
    ///   - showsLegend: Whether the legend shows beside the donut.
    ///   - isTableToggleShown: Whether "View as table" is offered.
    ///   - emptyMessage: The empty-state message.
    ///   - footer: Notes under the chart: the source, the last update.
    ///   - labels: Replacements for the built-in strings.
    ///   - formatValue: Writes values in the centre, legend, table and
    ///     summary. The default is a number in the current locale.
    ///   - onRetry: Shows Try again in the error status and runs when it is pressed.
    public init(
        _ title: String,
        segments: [MonarchDonutChartSegment],
        description: String? = nil,
        centerValue: String? = nil,
        centerLabel: String? = nil,
        summary: String? = nil,
        sort: MonarchDonutChartSort = .descending,
        maxSegments: Int = 6,
        legendContent: MonarchDonutChartLegendContent = .both,
        status: MonarchChartFrameStatus = .ready,
        size: CGFloat? = nil,
        thickness: CGFloat? = nil,
        percentDigits: Int = 0,
        showsCenterValue: Bool = true,
        showsLegend: Bool = true,
        isTableToggleShown: Bool = true,
        emptyMessage: String? = nil,
        footer: String? = nil,
        labels: MonarchDonutChartLabels = MonarchDonutChartLabels(),
        formatValue: ((Double) -> String)? = nil,
        onRetry: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.segments = segments
        self.description = description
        self.centerValue = centerValue
        self.centerLabel = centerLabel
        self.summary = summary
        self.sort = sort
        self.maxSegments = maxSegments
        self.legendContent = legendContent
        self.status = status
        self.size = size ?? Tokens.defaultSize
        self.thickness = thickness
        self.percentDigits = percentDigits
        self.showsCenterValue = showsCenterValue
        self.showsLegend = showsLegend
        self.isTableToggleShown = isTableToggleShown
        self.emptyMessage = emptyMessage
        self.footer = footer
        self.labels = labels
        self.formatValue = formatValue
        self.onRetry = onRetry
    }

    private var totalLabel: String { labels.total ?? MonarchDonutChartStrings.total }
    private var segmentTitle: String { labels.segment ?? MonarchDonutChartStrings.segment }
    private var valueTitle: String { labels.value ?? MonarchDonutChartStrings.value }

    /// The content of the view.
    public var body: some View {
        let ordered = Tokens.ordered(segments, sort: sort)
        let total = Tokens.total(ordered)
        let slices = Tokens.slices(
            ordered,
            maxSegments: maxSegments,
            other: labels.other ?? MonarchDonutChartStrings.other
        )
        let table = Tokens.table(
            ordered,
            segmentTitle: segmentTitle,
            valueTitle: valueTitle,
            shareTitle: labels.share ?? MonarchDonutChartStrings.share,
            totalLabel: totalLabel,
            format: formatValue,
            digits: percentDigits
        )
        MonarchChartDataFrame(
            title: title,
            description: description,
            status: Tokens.resolvedStatus(status, total: total),
            table: table,
            legend: [],
            hiddenSeries: nil,
            legendMarker: .square,
            emptyMessage: emptyMessage,
            footer: footer,
            height: size,
            isTableToggleShown: isTableToggleShown,
            labels: labels.frame,
            onRetry: onRetry
        ) {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .center, spacing: Tokens.rowSpacing) {
                    donut(ordered: ordered, slices: slices, total: total)
                    legend(slices, total: total)
                }
                VStack(alignment: .leading, spacing: Tokens.columnSpacing) {
                    donut(ordered: ordered, slices: slices, total: total)
                    legend(slices, total: total)
                }
            }
        }
    }

    private func donut(ordered: [Tokens.Slice], slices: [Tokens.Slice], total: Double) -> some View {
        let ring = Tokens.thickness(thickness, size: size)
        let inset = Tokens.angularInset(sliceCount: slices.count)
        let spoken =
            summary
            ?? Tokens.summary(ordered, totalLabel: totalLabel, format: formatValue, digits: percentDigits)
        return ZStack {
            Circle()
                .strokeBorder(theme.colors[role: Tokens.trackRole], lineWidth: ring)
            Chart(slices) { slice in
                SectorMark(
                    angle: .value(valueTitle, slice.value),
                    innerRadius: .ratio(Tokens.innerRatio(thickness: ring, size: size)),
                    angularInset: inset
                )
                .foregroundStyle(theme.colors[role: MonarchChartLegendTokens.role(slice.color)])
            }
            .chartLegend(.hidden)
            center(total: total)
                .padding(ring + Tokens.centerInset)
        }
        .frame(width: size, height: size)
        .modifier(
            MonarchChartAccessibility(
                title: title,
                summary: spoken,
                table: Tokens.descriptorTable(
                    ordered,
                    segmentTitle: segmentTitle,
                    valueTitle: valueTitle,
                    format: formatValue
                )
            )
        )
    }

    @ViewBuilder
    private func center(total: Double) -> some View {
        let value = showsCenterValue ? centerValue ?? Tokens.text(total, format: formatValue) : nil
        if value != nil || centerLabel != nil {
            VStack(spacing: 0) {
                if let value {
                    Text(value)
                        .monarchTextStyle(Tokens.centerValueStyle)
                        .monospacedDigit()
                        .foregroundStyle(theme.colors.textDefault)
                }
                if let centerLabel, !centerLabel.isEmpty {
                    Text(centerLabel)
                        .monarchTextStyle(Tokens.centerLabelStyle)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .multilineTextAlignment(.center)
            .minimumScaleFactor(Tokens.centerMinimumScale)
        }
    }

    @ViewBuilder
    private func legend(_ slices: [Tokens.Slice], total: Double) -> some View {
        if showsLegend {
            MonarchChartLegend(
                slices.map { slice in
                    MonarchChartLegendItem(
                        slice.key,
                        label: slice.label,
                        color: slice.color,
                        value: Tokens.legendValue(
                            value: Tokens.text(slice.value, format: formatValue),
                            share: Tokens.share(slice.value, of: total, digits: percentDigits),
                            content: legendContent
                        )
                    )
                },
                marker: .square,
                axis: .vertical,
                legendLabel: labels.frame.legend
            )
        }
    }
}

#if DEBUG
    #Preview("MonarchDonutChart") {
        let devices = [
            MonarchDonutChartSegment("laptop", label: "Laptops", value: 412),
            MonarchDonutChartSegment("phone", label: "Phones", value: 268),
            MonarchDonutChartSegment("monitor", label: "Monitors", value: 131),
            MonarchDonutChartSegment("tablet", label: "Tablets", value: 54),
            MonarchDonutChartSegment("other", label: "Accessories", value: 9),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchDonutChart("Assets by type", segments: devices, centerLabel: "assets")
                MonarchDonutChart("Assets by type", segments: devices, maxSegments: 3, legendContent: .percent)
                MonarchDonutChart("Assets by type", segments: [])
            }
        }
    }
#endif
