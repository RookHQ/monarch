import SwiftUI

/// One number with its label, an optional unit and an optional change:
/// "Open tickets 128 ↑ 12%". It has no surface; put it in a card, a header
/// or a table footer.
///
/// ```swift
/// MonarchMetric("Open tickets", value: 128, change: MonarchMetricChange(0.12, comparison: "vs last week"))
/// MonarchMetric("Response time", value: 840, unit: "ms", size: .sm)
/// MonarchMetric("Revenue", value: 1_284_000, format: .currency(code: "EUR"), isCompact: true, size: .lg)
/// ```
///
/// Numbers are written with Foundation's format styles in the current
/// locale and use tabular figures, so stacked metrics line up. `nil` shows a
/// muted dash.
///
/// Accessibility: the metric is one element. Its label is the metric's
/// label; its value is the full number with its unit and the change as a
/// sentence, for example "12,345 ms, up 12% vs last week, better". A compact
/// figure ("12K") is read in full, and the dash is read as "No data".
public struct MonarchMetric: View {
    private let label: String
    private let number: Double?
    private let text: String?
    private let usesText: Bool
    private let spokenText: String?
    private let unit: String?
    private let change: MonarchMetricChange?
    private let format: MonarchMetricFormat
    private let size: MonarchMetricSize
    private let isCompact: Bool
    private let emptyLabel: String?
    private let layout: MonarchMetricLayout
    private let alignment: HorizontalAlignment

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A metric with a number that Monarch formats.
    ///
    /// - Parameters:
    ///   - label: What the number measures, such as "Open tickets". Always visible.
    ///   - value: The number. `nil` shows a dash that is read as "No data".
    ///   - unit: A unit written smaller after the value, such as "ms" or "/ month".
    ///   - change: The change versus an earlier period.
    ///   - format: A plain number, a percentage of a ratio, or a currency.
    ///   - size: `sm` for dense rows, `md`, or `lg` for a headline figure.
    ///   - isCompact: Writes 1.2K, or €3.4M from iOS 18. VoiceOver reads the full value.
    ///   - emptyLabel: Replaces the built-in "No data" read for a missing value.
    ///   - layout: The label above the value, or everything on one line.
    ///   - alignment: `.leading`, or `.trailing` for numeric table columns.
    public init(
        _ label: String,
        value: Double?,
        unit: String? = nil,
        change: MonarchMetricChange? = nil,
        format: MonarchMetricFormat = .number,
        size: MonarchMetricSize = .md,
        isCompact: Bool = false,
        emptyLabel: String? = nil,
        layout: MonarchMetricLayout = .stacked,
        alignment: HorizontalAlignment = .leading
    ) {
        self.label = label
        self.number = value
        self.text = nil
        self.usesText = false
        self.spokenText = nil
        self.unit = unit
        self.change = change
        self.format = format
        self.size = size
        self.isCompact = isCompact
        self.emptyLabel = emptyLabel
        self.layout = layout
        self.alignment = alignment
    }

    /// A metric whose value the app has already written, such as "4.8 / 5".
    ///
    /// - Parameters:
    ///   - label: What the value measures. Always visible.
    ///   - text: The value as shown. `nil` or empty shows a dash read as "No data".
    ///   - spokenText: What VoiceOver reads for the value, when it differs from `text`.
    ///   - unit: A unit written smaller after the value.
    ///   - change: The change versus an earlier period.
    ///   - size: `sm` for dense rows, `md`, or `lg` for a headline figure.
    ///   - emptyLabel: Replaces the built-in "No data" read for a missing value.
    ///   - layout: The label above the value, or everything on one line.
    ///   - alignment: `.leading`, or `.trailing` for numeric table columns.
    public init(
        _ label: String,
        text: String?,
        spokenText: String? = nil,
        unit: String? = nil,
        change: MonarchMetricChange? = nil,
        size: MonarchMetricSize = .md,
        emptyLabel: String? = nil,
        layout: MonarchMetricLayout = .stacked,
        alignment: HorizontalAlignment = .leading
    ) {
        self.label = label
        self.number = nil
        self.text = text
        self.usesText = true
        self.spokenText = spokenText
        self.unit = unit
        self.change = change
        self.format = .number
        self.size = size
        self.isCompact = false
        self.emptyLabel = emptyLabel
        self.layout = layout
        self.alignment = alignment
    }

    /// Writes a number exactly as a metric shows it, for table cells and
    /// exports that must match.
    ///
    /// - Parameters:
    ///   - value: The number.
    ///   - format: A plain number, a percentage of a ratio, or a currency.
    ///   - isCompact: Writes 1.2K or €3.4M.
    ///   - locale: The locale to write it in.
    /// - Returns: The formatted text.
    public nonisolated static func formatted(
        _ value: Double,
        format: MonarchMetricFormat = .number,
        isCompact: Bool = false,
        locale: Locale = .autoupdatingCurrent
    ) -> String {
        MonarchMetricTokens.format(value, format: format, isCompact: isCompact, locale: locale)
    }

    private var figure: MonarchMetricTokens.Figure {
        if usesText {
            return MonarchMetricTokens.figure(text: text, spoken: spokenText, emptyLabel: emptyLabel)
        }
        return MonarchMetricTokens.figure(
            number,
            format: format,
            isCompact: isCompact,
            emptyLabel: emptyLabel,
            locale: locale
        )
    }

    /// The content of the view.
    public var body: some View {
        let figure = figure
        Group {
            switch layout {
            case .stacked:
                VStack(alignment: alignment, spacing: MonarchMetricTokens.stackedSpacing) {
                    labelText
                    figureView(figure)
                    changeView
                }
                .multilineTextAlignment(MonarchMetricTokens.textAlignment(alignment))
            case .inline:
                MonarchFlowLayout(spacing: MonarchMetricTokens.inlineSpacing, alignment: .center) {
                    labelText
                    figureView(figure)
                    changeView
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityValue(
            MonarchMetricTokens.accessibilityValue(
                figure: figure,
                unit: unit,
                changeSentence: change.map { MonarchMetricTokens.changeSentence($0, locale: locale) }
            )
        )
    }

    private var labelText: some View {
        Text(label)
            .monarchTextStyle(
                MonarchMetricTokens.labelStyle(size, layout: layout),
                weight: MonarchMetricTokens.labelWeight
            )
            .foregroundStyle(theme.colors[role: MonarchMetricTokens.muted])
    }

    private func figureView(_ figure: MonarchMetricTokens.Figure) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchMetricTokens.unitSpacing) {
            Text(figure.text)
                .monarchTextStyle(
                    MonarchMetricTokens.valueStyle(size, layout: layout),
                    weight: MonarchMetricTokens.valueWeight
                )
                .monospacedDigit()
                .foregroundStyle(theme.colors[role: MonarchMetricTokens.valueColor(isEmpty: figure.isEmpty)])
            if !figure.isEmpty, let unit, !unit.isEmpty {
                Text(unit)
                    .monarchTextStyle(
                        MonarchMetricTokens.unitStyle(size, layout: layout),
                        weight: MonarchMetricTokens.labelWeight
                    )
                    .foregroundStyle(theme.colors[role: MonarchMetricTokens.muted])
                    .fixedSize()
            }
        }
    }

    @ViewBuilder private var changeView: some View {
        if let change {
            MonarchDeltaIndicator(
                change.value,
                comparison: change.comparison,
                goodDirection: change.goodDirection,
                variant: change.variant,
                size: MonarchMetricTokens.changeSize(size),
                flatThreshold: change.flatThreshold,
                formatValue: change.formatValue
            )
        }
    }
}

#if DEBUG
    #Preview("MonarchMetric") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchMetricSize.allCases, id: \.self) { size in
                    MonarchMetric(
                        "Open tickets",
                        value: 128,
                        change: MonarchMetricChange(0.12, comparison: "vs last week"),
                        size: size
                    )
                }
                MonarchMetric("Response time", value: 840, unit: "ms", layout: .inline)
                MonarchMetric("Revenue", value: 1_284_000, format: .currency(code: "EUR"), isCompact: true)
                MonarchMetric("Churn", value: nil)
            }
        }
    }
#endif
