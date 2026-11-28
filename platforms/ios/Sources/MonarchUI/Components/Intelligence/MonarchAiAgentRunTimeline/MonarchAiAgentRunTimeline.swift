import SwiftUI

/// The trace of one agent run: every model call, tool call and hand-off as
/// a bar on a shared time axis, totals for time, tokens and cost, and
/// details that open per event. A table view shows the same data in rows.
///
/// ```swift
/// MonarchAiAgentRunTimeline(run.events, currency: "EUR") { event in
///     conversation.scroll(to: event.partId)
/// }
/// ```
///
/// Events are sorted by start time. An event without `endedAt` is running:
/// it lasts until `now` when given, else until the latest known time, so
/// the trace never depends on the clock. Bars start at the leading edge of
/// the track and follow the layout direction.
///
/// Accessibility: the trace is a container named "Agent run" with a header
/// trait on its heading. Each total is one element ("Total time, 4.2 s").
/// Each event is a disclosure button read as its kind, name and duration
/// ("Tool, search_invoices, 1.2 s") with an expanded or collapsed value;
/// the bar is hidden, so nothing depends on it. Each open fact is one
/// element. In the table each row is one element with every value. The
/// view switch is a standard segmented `Picker`.
public struct MonarchAiAgentRunTimeline: View {
    private let events: [MonarchAiRunEvent]
    private let currency: String
    private let now: Date?
    private let externalMode: Binding<MonarchAiAgentRunTimelineMode>?
    private let labels: MonarchAiAgentRunTimelineLabels
    private let onOpenPart: (@MainActor (MonarchAiRunEvent) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @State private var internalMode: MonarchAiAgentRunTimelineMode
    @State private var expanded: Set<String> = []

    /// A run timeline.
    ///
    /// - Parameters:
    ///   - events: The run's events, in any order.
    ///   - currency: The ISO 4217 currency of `costMicros`.
    ///   - now: The end of events that are still running. Without it they end at the latest known time.
    ///   - mode: The view, timeline or table. Without it the component keeps the state itself.
    ///   - initialMode: The first view when the component keeps the state.
    ///   - labels: Replacements for the built-in strings.
    ///   - onOpenPart: Adds "Show in conversation" to an open event that has a `partId` or a
    ///     `messageId`, and runs with the event when it is pressed.
    public init(
        _ events: [MonarchAiRunEvent],
        currency: String = "USD",
        now: Date? = nil,
        mode: Binding<MonarchAiAgentRunTimelineMode>? = nil,
        initialMode: MonarchAiAgentRunTimelineMode = .timeline,
        labels: MonarchAiAgentRunTimelineLabels = MonarchAiAgentRunTimelineLabels(),
        onOpenPart: (@MainActor (MonarchAiRunEvent) -> Void)? = nil
    ) {
        self.events = events
        self.currency = currency
        self.now = now
        self.externalMode = mode
        self.labels = labels
        self.onOpenPart = onOpenPart
        _internalMode = State(initialValue: initialMode)
    }

    private var mode: Binding<MonarchAiAgentRunTimelineMode> {
        externalMode ?? $internalMode
    }

    /// The content of the view.
    public var body: some View {
        let totals = MonarchAiAgentRunTimelineTokens.totals(events, now: now)
        let rows = MonarchAiAgentRunTimelineTokens.rows(events, totals: totals)
        let label = labels.label ?? MonarchAiAgentRunTimelineStrings.label
        VStack(alignment: .leading, spacing: MonarchAiAgentRunTimelineTokens.spacing) {
            header(label)
            totalsView(totals)
            if rows.isEmpty {
                Text(labels.empty ?? MonarchAiAgentRunTimelineStrings.empty)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            } else if mode.wrappedValue == .table {
                MonarchAiAgentRunTimelineTable(rows: rows, totals: totals, currency: currency, labels: labels)
            } else {
                timeline(rows, totals: totals)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(label)
    }

    private func header(_ label: String) -> some View {
        let title = Text(label)
            .monarchTextStyle(.subheading)
            .foregroundStyle(theme.colors.textDefault)
            .accessibilityAddTraits(.isHeader)
        let picker = Picker(labels.modeLabel ?? MonarchAiAgentRunTimelineStrings.modeLabel, selection: mode) {
            ForEach(MonarchAiAgentRunTimelineMode.allCases, id: \.self) { mode in
                Text(MonarchAiAgentRunTimelineTokens.modeName(mode, labels: labels)).tag(mode)
            }
        }
        .pickerStyle(.segmented)
        .fixedSize()
        return ViewThatFits(in: .horizontal) {
            HStack(spacing: MonarchAiAgentRunTimelineTokens.innerSpacing) {
                title
                Spacer(minLength: MonarchAiAgentRunTimelineTokens.innerSpacing)
                picker
            }
            VStack(alignment: .leading, spacing: MonarchAiAgentRunTimelineTokens.innerSpacing) {
                title
                picker
            }
        }
    }

    private func totalsView(_ totals: MonarchAiAgentRunTimelineTokens.Totals) -> some View {
        let facts = MonarchAiAgentRunTimelineTokens.totalFacts(
            totals,
            currency: currency,
            locale: locale,
            labels: labels
        )
        return MonarchFlowLayout(spacing: MonarchAiAgentRunTimelineTokens.spacing, alignment: .top) {
            ForEach(facts.indices, id: \.self) { index in
                VStack(alignment: .leading, spacing: MonarchAiAgentRunTimelineTokens.factSpacing) {
                    Text(facts[index].name)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                    Text(facts[index].value)
                        .monarchTextStyle(.bodyStrong)
                        .monospacedDigit()
                        .foregroundStyle(theme.colors.textDefault)
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(MonarchAiAgentRunTimelineTokens.fact(facts[index].name, facts[index].value))
            }
        }
    }

    private func timeline(_ rows: [MonarchAiAgentRunTimelineTokens.Row], totals: MonarchAiAgentRunTimelineTokens.Totals)
        -> some View
    {
        VStack(alignment: .leading, spacing: MonarchAiAgentRunTimelineTokens.eventSpacing) {
            HStack {
                ForEach(Array(MonarchAiAgentRunTimelineTokens.ticks(totals).enumerated()), id: \.offset) {
                    index,
                    tick in
                    if index > 0 { Spacer(minLength: MonarchAiAgentRunTimelineTokens.innerSpacing) }
                    Text(MonarchAiAgentRunTimelineTokens.durationText(tick, locale: locale, labels: labels))
                        .monarchTextStyle(.caption)
                        .monospacedDigit()
                        .foregroundStyle(theme.colors.textSubtle)
                }
            }
            .accessibilityHidden(true)
            ForEach(rows) { row in
                MonarchAiAgentRunTimelineEventRow(
                    row: row,
                    totals: totals,
                    currency: currency,
                    labels: labels,
                    isExpanded: expandedBinding(row.id),
                    onOpenPart: onOpenPart
                )
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.events ?? MonarchAiAgentRunTimelineStrings.events)
    }

    private func expandedBinding(_ id: String) -> Binding<Bool> {
        Binding(
            get: { expanded.contains(id) },
            set: { isOpen in
                if isOpen { expanded.insert(id) } else { expanded.remove(id) }
            }
        )
    }
}

#if DEBUG
    #Preview("MonarchAiAgentRunTimeline") {
        let start = Date(timeIntervalSinceReferenceDate: 800_000_000)
        MonarchPreviewMatrix {
            MonarchAiAgentRunTimeline(
                [
                    MonarchAiRunEvent(
                        id: "1",
                        kind: .model,
                        name: "rook-large",
                        startedAt: start,
                        endedAt: start.addingTimeInterval(1.2),
                        inputTokens: 1_200,
                        outputTokens: 310,
                        costMicros: 4_100
                    ),
                    MonarchAiRunEvent(
                        id: "2",
                        kind: .tool,
                        name: "search_invoices",
                        startedAt: start.addingTimeInterval(1.3),
                        endedAt: start.addingTimeInterval(2.9)
                    ),
                    MonarchAiRunEvent(
                        id: "3",
                        kind: .handoff,
                        name: "Billing agent",
                        startedAt: start.addingTimeInterval(3)
                    ),
                ],
                now: start.addingTimeInterval(4)
            )
        }
    }
#endif
