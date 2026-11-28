import SwiftUI

/// How full the model's context window is: a segmented bar of what fills it
/// (instructions, files, earlier messages) with a legend, a notice as it
/// nears the limit, and an optional "Summarize earlier messages" button.
///
/// ```swift
/// MonarchAiContextWindowMeter(
///     used: 110_000,
///     limit: 128_000,
///     breakdown: [
///         MonarchAiContextWindowPart("Instructions", tokens: 3_000),
///         MonarchAiContextWindowPart("Attachments", tokens: 22_000),
///         MonarchAiContextWindowPart("Messages", tokens: 85_000),
///     ],
///     isSummarizing: isSummarizing
/// ) {
///     summarize()
/// }
/// ```
///
/// Tokens that no part covers are shown as "Other", so the parts add up to
/// the total. Without a breakdown the bar is one fill: accent while there is
/// room, warning at `warningAt` and danger when full. The summarize button
/// is a ghost button while there is room and a secondary one once the meter
/// warns; it is offered, never run for you. `isCompact` shows a small bar
/// and the percentage, for a toolbar; activating it opens the full meter in
/// a popover.
///
/// Accessibility: the bar is one element named "Context window" whose value
/// is the summary ("45,000 of 128,000 tokens (35%)", or "About …" for an
/// estimate). The legend is a container named "What fills the context
/// window" whose entries are read as label and amount; the swatches are
/// decorative. The notice has an icon and text, and VoiceOver announces it
/// when the state changes while the meter is on screen. The compact meter
/// is a button with the same name and value and a hint that it shows the
/// details.
public struct MonarchAiContextWindowMeter: View {
    private let used: Int
    private let limit: Int
    private let breakdown: [MonarchAiContextWindowPart]
    private let warningAt: Double
    private let isApproximate: Bool
    private let isSummarizing: Bool
    private let labels: MonarchAiContextWindowMeterLabels
    private let isCompact: Bool
    private let onSummarize: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @State private var showsDetails = false

    /// A context window meter.
    ///
    /// - Parameters:
    ///   - used: The tokens in the context window now.
    ///   - limit: The model's context window size, in tokens.
    ///   - breakdown: What the tokens are made of, drawn as coloured segments with a legend.
    ///   - warningAt: The share of the limit, 0 to 1, at which the meter warns.
    ///   - isApproximate: Whether the count is an estimate, which reads "About …".
    ///   - isSummarizing: Whether a summary is being made: the button shows progress and does nothing.
    ///   - labels: Replaces the built-in strings.
    ///   - isCompact: Shows a small bar and the percentage that opens the full meter in a popover.
    ///   - onSummarize: Adds "Summarize earlier messages".
    public init(
        used: Int,
        limit: Int,
        breakdown: [MonarchAiContextWindowPart] = [],
        warningAt: Double = 0.8,
        isApproximate: Bool = false,
        isSummarizing: Bool = false,
        labels: MonarchAiContextWindowMeterLabels = MonarchAiContextWindowMeterLabels(),
        isCompact: Bool = false,
        onSummarize: (@MainActor () -> Void)? = nil
    ) {
        self.used = used
        self.limit = limit
        self.breakdown = breakdown
        self.warningAt = warningAt
        self.isApproximate = isApproximate
        self.isSummarizing = isSummarizing
        self.labels = labels
        self.isCompact = isCompact
        self.onSummarize = onSummarize
    }

    /// A context window meter from the usage the latest reply reported in
    /// the conversation model: input plus output tokens.
    ///
    /// - Parameters:
    ///   - usage: The usage of the latest assistant message.
    ///   - limit: The model's context window size, in tokens.
    ///   - breakdown: What the tokens are made of.
    ///   - warningAt: The share of the limit, 0 to 1, at which the meter warns.
    ///   - isApproximate: Whether the count is an estimate, which reads "About …".
    ///   - isSummarizing: Whether a summary is being made.
    ///   - labels: Replaces the built-in strings.
    ///   - isCompact: Shows a small bar that opens the full meter in a popover.
    ///   - onSummarize: Adds "Summarize earlier messages".
    public init(
        usage: MonarchAiUsage,
        limit: Int,
        breakdown: [MonarchAiContextWindowPart] = [],
        warningAt: Double = 0.8,
        isApproximate: Bool = false,
        isSummarizing: Bool = false,
        labels: MonarchAiContextWindowMeterLabels = MonarchAiContextWindowMeterLabels(),
        isCompact: Bool = false,
        onSummarize: (@MainActor () -> Void)? = nil
    ) {
        self.init(
            used: usage.inputTokens + usage.outputTokens,
            limit: limit,
            breakdown: breakdown,
            warningAt: warningAt,
            isApproximate: isApproximate,
            isSummarizing: isSummarizing,
            labels: labels,
            isCompact: isCompact,
            onSummarize: onSummarize
        )
    }

    private typealias Tokens = MonarchAiContextWindowMeterTokens

    private var state: MonarchAiContextWindowState {
        .of(used: used, limit: limit, warningAt: warningAt)
    }

    private var amount: String {
        Tokens.amount(used: used, limit: limit, isApproximate: isApproximate, locale: locale, labels: labels)
    }

    /// The content of the view.
    public var body: some View {
        Group {
            if isCompact {
                compact
            } else {
                details
            }
        }
        .onChange(of: state) { _, newState in
            if let notice = Tokens.notice(newState, labels: labels) {
                AccessibilityNotification.Announcement(notice).post()
            }
        }
    }

    private var details: some View {
        let segments = Tokens.segments(breakdown: breakdown, used: used, other: labels.other)
        return VStack(alignment: .leading, spacing: Tokens.stackSpacing) {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline, spacing: Tokens.headerSpacing) {
                    heading
                    Spacer(minLength: 0)
                    summary
                }
                VStack(alignment: .leading, spacing: Tokens.headerSpacing) {
                    heading
                    summary
                }
            }
            .accessibilityHidden(true)
            bar(segments)
            if !segments.isEmpty {
                MonarchChartLegend(
                    Tokens.legendItems(segments, locale: locale, labels: labels),
                    marker: .square,
                    axis: .vertical,
                    legendLabel: labels.legend
                )
            }
            if let notice = Tokens.notice(state, labels: labels), let tone = Tokens.noticeTone(state) {
                MonarchInlineNotice(notice, systemImage: Tokens.noticeSymbol(state), tone: tone)
            }
            if let onSummarize {
                MonarchButton(
                    labels.summarize,
                    variant: Tokens.buttonVariant(state),
                    size: .sm,
                    isLoading: isSummarizing,
                    action: onSummarize
                )
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
    }

    private var heading: some View {
        Text(labels.label)
            .monarchTextStyle(.label)
            .foregroundStyle(theme.colors.textDefault)
    }

    private var summary: some View {
        Text(amount)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
            .monospacedDigit()
    }

    private func bar(_ segments: [Tokens.Segment]) -> some View {
        MonarchAiContextWindowMeterBar(segments: segments, used: used, limit: limit, state: state)
            .accessibilityElement()
            .accessibilityLabel(labels.label)
            .accessibilityValue(amount)
            .accessibilityAddTraits(.updatesFrequently)
    }

    private var compact: some View {
        Button {
            showsDetails = true
        } label: {
            HStack(spacing: Tokens.compactSpacing) {
                MonarchAiContextWindowMeterBar(
                    segments: Tokens.segments(breakdown: breakdown, used: used, other: labels.other),
                    used: used,
                    limit: limit,
                    state: state
                )
                .frame(width: Tokens.compactBarWidth)
                Text(Tokens.percent(Tokens.fraction(used: used, limit: limit), locale: locale))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .monospacedDigit()
                if let tone = Tokens.noticeTone(state) {
                    MonarchIcon(systemName: Tokens.noticeSymbol(state), size: .sm)
                        .foregroundStyle(theme.colors[role: MonarchTonePalette.palette(for: tone).strong])
                }
            }
            .monarchMinimumHitTarget()
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(labels.label)
        .accessibilityValue(amount)
        .accessibilityHint(labels.showDetails)
        .popover(isPresented: $showsDetails) {
            details
                .padding(Tokens.popoverPadding)
                .frame(minWidth: Tokens.popoverMinWidth)
                .presentationCompactAdaptation(.popover)
        }
    }
}

#if DEBUG
    #Preview("MonarchAiContextWindowMeter") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiContextWindowMeter(
                    used: 110_000,
                    limit: 128_000,
                    breakdown: [
                        MonarchAiContextWindowPart("Instructions", tokens: 3_000),
                        MonarchAiContextWindowPart("Attachments", tokens: 22_000),
                        MonarchAiContextWindowPart("Messages", tokens: 80_000),
                    ]
                ) {}
                MonarchAiContextWindowMeter(used: 45_000, limit: 128_000, isApproximate: true)
                MonarchAiContextWindowMeter(used: 130_000, limit: 128_000, isCompact: true)
            }
        }
    }
#endif
