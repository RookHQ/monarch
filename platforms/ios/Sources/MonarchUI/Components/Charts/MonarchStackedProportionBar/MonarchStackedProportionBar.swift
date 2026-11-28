import SwiftUI

/// One horizontal bar split into proportions, with a legend of values and
/// shares: pipeline stages, storage by type, monitors by state.
///
/// ```swift
/// MonarchStackedProportionBar(
///     "Lead pipeline",
///     segments: [
///         MonarchStackedProportionBarSegment("new", label: "New", value: 1_240),
///         MonarchStackedProportionBarSegment("replied", label: "Replied", value: 182),
///     ]
/// )
///
/// MonarchStackedProportionBar(
///     "Storage",
///     segments: storage,
///     total: 100,
///     remainderLabel: "Free",
///     legendContent: .value,
///     size: .lg
/// )
/// ```
///
/// The bar is drawn; the legend is the data. Segments use the chart palette
/// in order, or status tones when every segment has one. A `total` larger
/// than the sum leaves empty track, listed as `remainderLabel`.
///
/// Accessibility: the bar is hidden and the legend is read: a container
/// named by `label`, each entry read as its label, value and share. With
/// `showsLegend` off, the legend is not drawn and the bar is one element
/// that reads `label` and every entry. With no data, it reads the label and
/// "No data yet".
public struct MonarchStackedProportionBar: View {
    private let label: String
    private let segments: [MonarchStackedProportionBarSegment]
    private let total: Double?
    private let remainderLabel: String?
    private let legendContent: MonarchStackedProportionBarLegendContent
    private let size: MonarchStackedProportionBarSize
    private let showsLegend: Bool
    private let hidesZero: Bool
    private let percentDigits: Int
    private let emptyLabel: String?
    private let formatValue: ((Double) -> String)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A stacked proportion bar.
    ///
    /// - Parameters:
    ///   - label: Names the legend for VoiceOver, such as "Lead pipeline". Already localised.
    ///   - segments: The parts, in display order.
    ///   - total: The size of the whole, for a capacity. The sum of the parts by default.
    ///   - remainderLabel: The legend name of the empty remainder, such as "Free". Without it the remainder is not listed.
    ///   - legendContent: What each legend entry shows after its label.
    ///   - size: The thickness of the bar.
    ///   - showsLegend: Whether the legend is drawn. Off, VoiceOver still reads every entry.
    ///   - hidesZero: Whether parts with a value of 0 are left out.
    ///   - percentDigits: The decimal places of shares.
    ///   - emptyLabel: Replaces the built-in text "No data yet".
    ///   - formatValue: Writes a value. The default is a number in the current locale.
    public init(
        _ label: String,
        segments: [MonarchStackedProportionBarSegment],
        total: Double? = nil,
        remainderLabel: String? = nil,
        legendContent: MonarchStackedProportionBarLegendContent = .both,
        size: MonarchStackedProportionBarSize = .md,
        showsLegend: Bool = true,
        hidesZero: Bool = true,
        percentDigits: Int = 0,
        emptyLabel: String? = nil,
        formatValue: ((Double) -> String)? = nil
    ) {
        self.label = label
        self.segments = segments
        self.total = total
        self.remainderLabel = remainderLabel
        self.legendContent = legendContent
        self.size = size
        self.showsLegend = showsLegend
        self.hidesZero = hidesZero
        self.percentDigits = percentDigits
        self.emptyLabel = emptyLabel
        self.formatValue = formatValue
    }

    /// The content of the view.
    public var body: some View {
        let parts = MonarchStackedProportionBarTokens.parts(segments, hidesZero: hidesZero)
        let sum = parts.reduce(0) { $0 + $1.value }
        let whole = MonarchStackedProportionBarTokens.total(sum: sum, given: total)
        let remainder = max(0, whole - sum)
        let entries = legendEntries(parts, whole: whole, remainder: remainder)
        VStack(alignment: .leading, spacing: MonarchStackedProportionBarTokens.spacing) {
            if whole > 0, !entries.isEmpty {
                track(parts.filter { $0.value > 0 }, remainder: remainder)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(label)
                    .accessibilityValue(
                        MonarchStackedProportionBarTokens.accessibilityValue(entries.map { ($0.label, $0.value) })
                    )
                    .accessibilityHidden(showsLegend)
                if showsLegend {
                    legend(entries)
                }
            } else {
                let empty = emptyLabel ?? MonarchStackedProportionBarStrings.empty
                VStack(alignment: .leading, spacing: MonarchStackedProportionBarTokens.spacing) {
                    track([], remainder: 0)
                    Text(empty)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
                .accessibilityValue(empty)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func legendEntries(
        _ parts: [MonarchStackedProportionBarTokens.Part],
        whole: Double,
        remainder: Double
    ) -> [MonarchStackedProportionBarEntry] {
        func value(_ amount: Double) -> String? {
            MonarchStackedProportionBarTokens.legendValue(
                amount,
                total: whole,
                content: legendContent,
                digits: percentDigits,
                locale: locale,
                formatValue: formatValue
            )
        }
        var entries = parts.map {
            MonarchStackedProportionBarEntry(
                key: $0.key,
                label: $0.label,
                value: value($0.value),
                color: $0.color,
                role: $0.role,
                isRemainder: false
            )
        }
        if remainder > 0, let remainderLabel {
            entries.append(
                MonarchStackedProportionBarEntry(
                    key: MonarchStackedProportionBarTokens.remainderKey,
                    label: remainderLabel,
                    value: value(remainder),
                    color: .neutral,
                    role: MonarchStackedProportionBarTokens.remainderRole,
                    isRemainder: true
                )
            )
        }
        return entries
    }

    /// The bar. Segments are laid out from the leading edge, so the bar
    /// mirrors in right-to-left layouts.
    private func track(_ parts: [MonarchStackedProportionBarTokens.Part], remainder: Double) -> some View {
        GeometryReader { proxy in
            let widths = MonarchStackedProportionBarTokens.widths(
                parts.map(\.value),
                remainder: remainder,
                available: proxy.size.width,
                gap: MonarchStackedProportionBarTokens.gap,
                minimum: MonarchStackedProportionBarTokens.minimumSegmentWidth
            )
            HStack(spacing: MonarchStackedProportionBarTokens.gap) {
                ForEach(Array(parts.enumerated()), id: \.element.key) { index, part in
                    Rectangle()
                        .fill(theme.colors.soft(part.role, by: MonarchBarChartTokens.softening))
                        .frame(width: widths.segments[index])
                }
                if remainder > 0 {
                    Color.clear.frame(width: widths.remainder)
                }
            }
            .frame(width: proxy.size.width, height: proxy.size.height, alignment: .leading)
        }
        .frame(height: MonarchStackedProportionBarTokens.thickness(size))
        .background(Capsule().fill(theme.colors[role: MonarchStackedProportionBarTokens.trackRole]))
        .clipShape(Capsule())
    }

    @ViewBuilder private func legend(_ entries: [MonarchStackedProportionBarEntry]) -> some View {
        if MonarchStackedProportionBarTokens.isStatusMode(segments) || entries.contains(where: \.isRemainder) {
            MonarchStackedProportionBarLegend(label: label, entries: entries)
        } else {
            MonarchChartLegend(
                entries.map { MonarchChartLegendItem($0.key, label: $0.label, color: $0.color, value: $0.value) },
                marker: .square,
                legendLabel: label
            )
        }
    }
}

#if DEBUG
    #Preview("MonarchStackedProportionBar") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchStackedProportionBar(
                    "Lead pipeline",
                    segments: [
                        MonarchStackedProportionBarSegment("new", label: "New", value: 1_240),
                        MonarchStackedProportionBarSegment("campaign", label: "In a campaign", value: 610),
                        MonarchStackedProportionBarSegment("replied", label: "Replied", value: 182),
                        MonarchStackedProportionBarSegment("converted", label: "Converted", value: 41),
                        MonarchStackedProportionBarSegment("unsubscribed", label: "Unsubscribed", value: 6),
                    ]
                )
                MonarchStackedProportionBar(
                    "Storage",
                    segments: [
                        MonarchStackedProportionBarSegment("media", label: "Media", value: 42),
                        MonarchStackedProportionBarSegment("docs", label: "Documents", value: 18),
                    ],
                    total: 100,
                    remainderLabel: "Free",
                    legendContent: .value,
                    size: .lg
                )
                MonarchStackedProportionBar(
                    "Monitors",
                    segments: [
                        MonarchStackedProportionBarSegment("up", label: "Operational", value: 41, tone: .success),
                        MonarchStackedProportionBarSegment("slow", label: "Degraded", value: 3, tone: .warning),
                        MonarchStackedProportionBarSegment("down", label: "Down", value: 1, tone: .danger),
                    ],
                    size: .sm
                )
                MonarchStackedProportionBar("Empty", segments: [])
            }
        }
    }
#endif
