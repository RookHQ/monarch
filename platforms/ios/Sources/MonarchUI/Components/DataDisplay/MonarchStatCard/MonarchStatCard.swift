import SwiftUI

/// One KPI for a dashboard: a ``MonarchMetric`` (label, value, unit and
/// change) on a card, with an optional hint, an explanation behind an info
/// button, a trend sparkline and a drill-down.
///
/// ```swift
/// MonarchStatCard(
///     "Open tickets",
///     value: 128,
///     change: MonarchMetricChange(0.12, comparison: "vs last week", goodDirection: .down),
///     hint: "38 waiting on a customer",
///     help: "Tickets that are not solved or closed.",
///     sparkline: [96, 104, 110, 121, 128]
/// ) {
///     path.append(Route.openTickets)
/// }
/// ```
///
/// The number is written by ``MonarchMetric`` (Foundation format styles,
/// tabular digits, compact notation read in full). While loading the value
/// is a redacted placeholder; an error replaces it with the message and,
/// with `onRetry`, Try again. A tone other than neutral colours the border
/// and adds the tone's icon, read as the tone's name. With `action` the
/// card is a button with a chevron. Group cards with ``MonarchDashboardGrid``
/// or a `LazyVGrid`.
///
/// Accessibility: the metric is one element read as its label, then the
/// value, unit and change as a sentence. With `action` it is a button with
/// the hint "Shows the details". The info button is separate, named "About
/// <label>". The sparkline is hidden: the value and change carry the data.
/// While loading the value reads "Loading".
public struct MonarchStatCard: View {
    private let label: String
    private let value: MonarchStatCardValue
    private let unit: String?
    private let change: MonarchMetricChange?
    private let hint: String?
    private let help: String?
    private let systemImage: String?
    private let sparkline: [Double?]?
    private let tone: MonarchTone
    private let variant: MonarchStatCardVariant
    private let size: MonarchMetricSize
    private let isLoading: Bool
    private let errorMessage: String?
    private let isRetrying: Bool
    private let emptyLabel: String?
    private let helpLabel: String?
    private let loadingLabel: String?
    private let onRetry: (@MainActor () -> Void)?
    private let action: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchStatCardTokens

    /// A stat card with a number that Monarch formats.
    ///
    /// - Parameters:
    ///   - label: What the number measures, such as "Open tickets". Always visible.
    ///   - value: The number. `nil` shows a dash that is read as "No data".
    ///   - unit: A unit written smaller after the value, such as "ms".
    ///   - change: The change versus an earlier period, drawn by ``MonarchDeltaIndicator``.
    ///   - format: A plain number, a percentage of a ratio, or a currency.
    ///   - isCompact: Writes 1.2K or €3.4M. VoiceOver reads the full value.
    ///   - hint: A line under the value, such as "38 of 52 people".
    ///   - help: How the metric is defined, shown from an info button.
    ///   - systemImage: A decorative SF Symbol before the metric.
    ///   - sparkline: A trend under the value. Decorative.
    ///   - tone: A threshold or emphasis: the border takes the tone, with its icon. Say why in `hint`.
    ///   - variant: A card, or no surface inside another surface.
    ///   - size: `sm` for dense rows, `md`, or `lg` for a headline figure.
    ///   - isLoading: Shows a placeholder for the value.
    ///   - errorMessage: Replaces the value with this message.
    ///   - isRetrying: Whether a retry is in progress.
    ///   - emptyLabel: Replaces the built-in "No data" read for a missing value.
    ///   - helpLabel: Replaces the built-in spoken name of the info button, "About <label>".
    ///   - loadingLabel: Replaces the built-in "Loading" read while loading.
    ///   - action: Opens the details behind the number. Makes the card a button.
    ///   - onRetry: Shows Try again with an error.
    public init(
        _ label: String,
        value: Double?,
        unit: String? = nil,
        change: MonarchMetricChange? = nil,
        format: MonarchMetricFormat = .number,
        isCompact: Bool = false,
        hint: String? = nil,
        help: String? = nil,
        systemImage: String? = nil,
        sparkline: [Double?]? = nil,
        tone: MonarchTone = .neutral,
        variant: MonarchStatCardVariant = .card,
        size: MonarchMetricSize = .md,
        isLoading: Bool = false,
        errorMessage: String? = nil,
        isRetrying: Bool = false,
        emptyLabel: String? = nil,
        helpLabel: String? = nil,
        loadingLabel: String? = nil,
        action: (@MainActor () -> Void)? = nil,
        onRetry: (@MainActor () -> Void)? = nil
    ) {
        self.label = label
        self.value = .number(value, format: format, isCompact: isCompact)
        self.unit = unit
        self.change = change
        self.hint = hint
        self.help = help
        self.systemImage = systemImage
        self.sparkline = sparkline
        self.tone = tone
        self.variant = variant
        self.size = size
        self.isLoading = isLoading
        self.errorMessage = errorMessage
        self.isRetrying = isRetrying
        self.emptyLabel = emptyLabel
        self.helpLabel = helpLabel
        self.loadingLabel = loadingLabel
        self.onRetry = onRetry
        self.action = action
    }

    /// A stat card whose value the app has already written, such as "4.8 / 5".
    ///
    /// - Parameters:
    ///   - label: What the value measures. Always visible.
    ///   - text: The value as shown. `nil` or empty shows a dash read as "No data".
    ///   - spokenText: What VoiceOver reads for the value, when it differs from `text`.
    ///   - unit: A unit written smaller after the value.
    ///   - change: The change versus an earlier period.
    ///   - hint: A line under the value.
    ///   - help: How the metric is defined, shown from an info button.
    ///   - systemImage: A decorative SF Symbol before the metric.
    ///   - sparkline: A trend under the value. Decorative.
    ///   - tone: A threshold or emphasis: the border takes the tone, with its icon.
    ///   - variant: A card, or no surface inside another surface.
    ///   - size: `sm` for dense rows, `md`, or `lg` for a headline figure.
    ///   - isLoading: Shows a placeholder for the value.
    ///   - errorMessage: Replaces the value with this message.
    ///   - isRetrying: Whether a retry is in progress.
    ///   - emptyLabel: Replaces the built-in "No data" read for a missing value.
    ///   - helpLabel: Replaces the built-in spoken name of the info button.
    ///   - loadingLabel: Replaces the built-in "Loading" read while loading.
    ///   - action: Opens the details behind the number. Makes the card a button.
    ///   - onRetry: Shows Try again with an error.
    public init(
        _ label: String,
        text: String?,
        spokenText: String? = nil,
        unit: String? = nil,
        change: MonarchMetricChange? = nil,
        hint: String? = nil,
        help: String? = nil,
        systemImage: String? = nil,
        sparkline: [Double?]? = nil,
        tone: MonarchTone = .neutral,
        variant: MonarchStatCardVariant = .card,
        size: MonarchMetricSize = .md,
        isLoading: Bool = false,
        errorMessage: String? = nil,
        isRetrying: Bool = false,
        emptyLabel: String? = nil,
        helpLabel: String? = nil,
        loadingLabel: String? = nil,
        action: (@MainActor () -> Void)? = nil,
        onRetry: (@MainActor () -> Void)? = nil
    ) {
        self.label = label
        self.value = .text(text, spoken: spokenText)
        self.unit = unit
        self.change = change
        self.hint = hint
        self.help = help
        self.systemImage = systemImage
        self.sparkline = sparkline
        self.tone = tone
        self.variant = variant
        self.size = size
        self.isLoading = isLoading
        self.errorMessage = errorMessage
        self.isRetrying = isRetrying
        self.emptyLabel = emptyLabel
        self.helpLabel = helpLabel
        self.loadingLabel = loadingLabel
        self.onRetry = onRetry
        self.action = action
    }

    /// The content of the view.
    public var body: some View {
        let shape = RoundedRectangle(cornerRadius: Tokens.cornerRadius, style: .continuous)
        Group {
            if Tokens.hasSurface(variant) {
                content
                    .padding(Tokens.padding)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    // An inline surface: a hairline border and no shadow.
                    .background(shape.fill(theme.colors[role: MonarchCardTokens.background]))
                    .overlay(
                        shape.strokeBorder(theme.colors[role: Tokens.border(tone)], lineWidth: Tokens.borderWidth)
                    )
            } else {
                content.frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .accessibilityElement(children: .contain)
    }

    private var content: some View {
        HStack(alignment: .top, spacing: Tokens.rowSpacing) {
            if let action, Tokens.phase(isLoading: isLoading, errorMessage: errorMessage) == .value {
                Button(action: action) {
                    HStack(alignment: .center, spacing: Tokens.rowSpacing) {
                        main
                        MonarchIcon(systemName: Tokens.disclosureSymbol, size: .sm)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityHint(MonarchStatCardStrings.drillDownHint)
            } else {
                main
            }
            // The icons sit at the trailing end, so the label and the value
            // start on the card's left edge.
            if let systemImage {
                MonarchIcon(systemName: systemImage, size: .sm)
                    .foregroundStyle(theme.colors.textMuted)
            }
            if let symbol = Tokens.toneSymbol(tone), let spoken = MonarchToneStrings.name(tone) {
                MonarchIcon(systemName: symbol, label: spoken, size: .sm)
                    .foregroundStyle(theme.colors[role: Tokens.toneColor(tone)])
            }
            if let help, !help.isEmpty {
                MonarchInfoTooltip(help, label: helpLabel ?? MonarchStatCardStrings.helpLabel(label))
            }
        }
    }

    private var main: some View {
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            valueArea
                .frame(maxWidth: .infinity, alignment: .leading)
            if let hint, !hint.isEmpty {
                Text(hint)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if let sparkline, Tokens.phase(isLoading: isLoading, errorMessage: errorMessage) == .value {
                MonarchSparkline(sparkline, tone: Tokens.sparklineTone(tone), fullWidth: true)
                    .accessibilityHidden(true)
            }
        }
    }

    @ViewBuilder private var valueArea: some View {
        switch Tokens.phase(isLoading: isLoading, errorMessage: errorMessage) {
        case .value:
            metric(value)
        case .loading:
            MonarchMetric(label, text: Tokens.placeholderValue, unit: unit, size: size)
                .redacted(reason: .placeholder)
                .accessibilityValue(loadingLabel ?? MonarchStatCardStrings.loading)
        case .error:
            VStack(alignment: .leading, spacing: Tokens.spacing) {
                Text(label)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textMuted)
                if let onRetry {
                    MonarchInlineErrorRetry(errorMessage ?? "", isRetrying: isRetrying, onRetry: onRetry)
                } else {
                    MonarchInlineNotice(errorMessage ?? "", tone: .danger, variant: .plain)
                }
            }
        }
    }

    @ViewBuilder private func metric(_ value: MonarchStatCardValue) -> some View {
        switch value {
        case .number(let number, let format, let isCompact):
            MonarchMetric(
                label,
                value: number,
                unit: unit,
                change: change,
                format: format,
                size: size,
                isCompact: isCompact,
                emptyLabel: emptyLabel
            )
        case .text(let text, let spoken):
            MonarchMetric(
                label,
                text: text,
                spokenText: spoken,
                unit: unit,
                change: change,
                size: size,
                emptyLabel: emptyLabel
            )
        }
    }
}

/// The value of a ``MonarchStatCard``: a number Monarch formats, or text the app wrote.
private enum MonarchStatCardValue {
    case number(Double?, format: MonarchMetricFormat, isCompact: Bool)
    case text(String?, spoken: String?)
}

#if DEBUG
    #Preview("MonarchStatCard") {
        MonarchPreviewMatrix {
            VStack {
                MonarchStatCard(
                    "Open tickets",
                    value: 128,
                    change: MonarchMetricChange(0.12, comparison: "vs last week", goodDirection: .down),
                    hint: "38 waiting on a customer",
                    help: "Tickets that are not solved or closed.",
                    sparkline: [96, 104, 110, 121, 128],
                    action: {}
                )
                MonarchStatCard(
                    "Revenue",
                    value: 1_284_000,
                    format: .currency(code: "EUR"),
                    isCompact: true,
                    tone: .success
                )
                MonarchStatCard("Churn", value: nil, isLoading: true)
                MonarchStatCard("Uptime", value: nil, errorMessage: "Couldn’t load uptime.", onRetry: {})
            }
        }
    }
#endif
