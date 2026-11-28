import SwiftUI

/// A word-sized trend line, area or bar chart for tables, stat cards and lists.
///
/// ```swift
/// MonarchSparkline([3, 5, 4, 8, 7, 9], label: "Sign-ups, last 6 days")
/// MonarchSparkline(latency, tone: .warning, variant: .area)
/// ```
///
/// A `nil` or a value that is not finite is a gap: the line breaks there.
/// Without a tone the sparkline takes the foreground style around it, like
/// text. A tone is never the only signal: show the value or a
/// ``MonarchDeltaIndicator`` beside it.
///
/// The chart runs from the leading to the trailing edge, so it is mirrored
/// in right-to-left layouts and the latest value is at the trailing end.
///
/// Accessibility: with a `label` the sparkline is an image whose value gives
/// the latest, lowest and highest values, then the name of the tone. Without
/// a label it is decorative and hidden, so put the numbers in text nearby.
/// It scales with Dynamic Type.
public struct MonarchSparkline: View {
    private let values: [Double?]
    private let label: String?
    private let summary: String?
    private let tone: MonarchTone?
    private let color: MonarchChartColor?
    private let variant: MonarchSparklineVariant
    private let range: ClosedRange<Double>?
    private let isInverted: Bool
    private let showsLastPoint: Bool
    private let notEnoughDataLabel: String?
    private let fullWidth: Bool
    private let formatValue: ((Double) -> String)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .body) private var scale: CGFloat = 1

    /// A sparkline.
    ///
    /// - Parameters:
    ///   - values: The values in order. `nil` is a gap.
    ///   - label: The spoken name, such as "p95 latency, last 14 days". Without
    ///     it the sparkline is decorative.
    ///   - summary: Replaces the spoken value "latest 9, low 3, high 9".
    ///   - tone: A colour with meaning. `nil` takes the surrounding foreground style.
    ///   - color: A chart series colour (one of the theme's data slots), for a sparkline that has no status meaning. Ignored when `tone` is set.
    ///   - variant: A line, a line with a soft fill, or bars.
    ///   - range: Fixes the value range, so sparklines in a table share one
    ///     scale. Each fits its own values when `nil`.
    ///   - isInverted: Whether smaller values are drawn higher, as for a rank.
    ///   - showsLastPoint: Whether the latest point has a dot. Lines only.
    ///   - notEnoughDataLabel: Replaces the built-in text "Not enough data".
    ///   - fullWidth: Whether the sparkline fills the available width.
    ///   - formatValue: Writes the numbers of the spoken value. The default is
    ///     a number in the current locale.
    public init(
        _ values: [Double?],
        label: String? = nil,
        summary: String? = nil,
        tone: MonarchTone? = nil,
        color: MonarchChartColor? = nil,
        variant: MonarchSparklineVariant = .line,
        range: ClosedRange<Double>? = nil,
        isInverted: Bool = false,
        showsLastPoint: Bool = true,
        notEnoughDataLabel: String? = nil,
        fullWidth: Bool = false,
        formatValue: ((Double) -> String)? = nil
    ) {
        self.values = values
        self.label = label
        self.summary = summary
        self.tone = tone
        self.color = color
        self.variant = variant
        self.range = range
        self.isInverted = isInverted
        self.showsLastPoint = showsLastPoint
        self.notEnoughDataLabel = notEnoughDataLabel
        self.fullWidth = fullWidth
        self.formatValue = formatValue
    }

    /// The content of the view.
    public var body: some View {
        let plot = MonarchSparklineTokens.Plot(values: values, variant: variant, range: range, isInverted: isInverted)
        if let stats = plot.stats, plot.hasEnoughData {
            chart(plot)
                .frame(
                    width: fullWidth ? nil : MonarchSparklineTokens.width * scale,
                    height: MonarchSparklineTokens.height * scale
                )
                .frame(maxWidth: fullWidth ? .infinity : nil)
                .modifier(
                    MonarchSparklineAccessibility(
                        label: label,
                        value: summary
                            ?? MonarchSparklineTokens.summary(
                                stats: stats,
                                tone: tone,
                                format: formatValue ?? { MonarchSparklineTokens.number($0, locale: locale) }
                            )
                    )
                )
        } else {
            Text(notEnoughDataLabel ?? MonarchSparklineStrings.notEnoughData)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
                .modifier(
                    MonarchSparklineAccessibility(
                        label: label,
                        value: notEnoughDataLabel ?? MonarchSparklineStrings.notEnoughData,
                        isHiddenWithoutLabel: false
                    )
                )
        }
    }

    private func chart(_ plot: MonarchSparklineTokens.Plot) -> some View {
        let styles = MonarchSparklineTokens.styles(tone: tone, color: color)
        let line: AnyShapeStyle =
            styles.line.map { AnyShapeStyle(theme.colors[role: $0]) } ?? AnyShapeStyle(.foreground)
        let area: AnyShapeStyle =
            styles.area.map { AnyShapeStyle(theme.colors[role: $0].opacity(tone == nil && color != nil ? 0.2 : 1)) }
            ?? AnyShapeStyle(.quaternary)
        return ZStack {
            switch variant {
            case .bar:
                MonarchSparklineMark(plot: plot, part: .bars).fill(line)
            case .line, .area:
                if variant == .area {
                    MonarchSparklineMark(plot: plot, part: .area).fill(area)
                }
                MonarchSparklineMark(plot: plot, part: .line)
                    .stroke(
                        line,
                        style: StrokeStyle(
                            lineWidth: MonarchSparklineTokens.lineWidth * scale,
                            lineCap: .round,
                            lineJoin: .round
                        )
                    )
                MonarchSparklineMark(
                    plot: plot,
                    part: .dots(includesLast: showsLastPoint, radius: MonarchSparklineTokens.dotRadius * scale)
                )
                .fill(line)
            }
        }
    }
}

/// With a label the sparkline is an image with a value; without one it is hidden.
private struct MonarchSparklineAccessibility: ViewModifier {
    let label: String?
    let value: String
    var isHiddenWithoutLabel = true

    func body(content: Content) -> some View {
        if let label, !label.isEmpty {
            content
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
                .accessibilityValue(value)
                .accessibilityAddTraits(.isImage)
        } else {
            content.accessibilityHidden(isHiddenWithoutLabel)
        }
    }
}

#if DEBUG
    #Preview("MonarchSparkline") {
        let values: [Double?] = [3, 5, 4, nil, 8, 7, 9, 6, 11]
        return MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchSparklineVariant.allCases, id: \.self) { variant in
                    HStack {
                        MonarchSparkline(values, label: "Sign-ups", variant: variant)
                        MonarchSparkline(values, tone: .success, variant: variant)
                        MonarchSparkline(values, tone: .danger, variant: variant, isInverted: true)
                    }
                }
                MonarchSparkline(values, tone: .info, variant: .area, fullWidth: true)
                MonarchSparkline([1], label: "Sign-ups")
            }
        }
    }
#endif
