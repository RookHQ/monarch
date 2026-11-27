import MonarchTokens
import SwiftUI

/// The technical trace of one agent run: every model call, tool call,
/// retrieval and sub-agent as a row in a tree, with its duration, tokens,
/// cost and a bar on a shared time scale. Selecting a row opens its details
/// in a sheet.
///
/// ```swift
/// MonarchAiTraceView(run.spans, currency: "USD", now: run.elapsed) { span in
///     conversation.scroll(to: span.partId)
/// }
/// ```
///
/// The header has the title and a segmented filter (All, Errors only,
/// Slowest first); under it are the totals: wall-clock time, tokens, cost
/// per currency and the number of errors. Spans nest by `parentId`, with
/// siblings in start order. At first every top-level span is open, and so is
/// every failed span and every span a failure ran inside. The details show
/// the status, the times, tokens, cost, model, error, attributes and the
/// plain-text input and output with Copy, and Show in conversation when
/// `onOpenPart` is given and the span has a part or message. Rows are in a
/// `LazyVStack`, so a long trace builds only the rows on screen when the
/// view is in a scroll view. Data comes in through the parameters; the
/// view never fetches.
///
/// Accessibility: the trace is a container named "Run trace" with a heading.
/// Each row is a button read as the name, the status unless OK, the kind,
/// the duration, tokens, cost and "Starts at +1.2 s, takes 850 ms"; the
/// selected row has the selected trait and a bar beside it. Spans with
/// children have a separate chevron button, "Show spans inside fetch". A
/// failed span has an error icon and the word Error, not colour alone. The
/// bars are hidden from VoiceOver; their timing is in the row's label.
@_spi(Experimental) public struct MonarchAiTraceView: View {
    private let spans: [MonarchAiTraceSpan]
    private let currency: String
    private let now: TimeInterval?
    private let selectionBinding: Binding<String?>?
    private let filterBinding: Binding<MonarchAiTraceFilter>?
    private let expandedBinding: Binding<Set<String>>?
    private let labels: MonarchAiTraceViewLabels
    private let onOpenPart: (@MainActor (MonarchAiTraceSpan) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @State private var ownSelection: String?
    @State private var ownFilter = MonarchAiTraceFilter.all
    @State private var ownExpanded: Set<String>?
    @State private var availableWidth: CGFloat = .infinity

    /// A trace view.
    ///
    /// - Parameters:
    ///   - spans: The spans of one run, in any order. Children are found through `parentId`.
    ///   - currency: The ISO 4217 currency of spans that do not name one.
    ///   - now: The current time in seconds from the start of the run, where
    ///     the bars of running spans end. Without it they end at the latest known time.
    ///   - selection: The id of the span whose details are open. Kept inside when `nil`.
    ///   - filter: Which spans are shown. Kept inside, starting at All, when `nil`.
    ///   - expandedIDs: The ids of open spans. Kept inside when `nil`: top-level
    ///     spans, failed spans and their parents open at first.
    ///   - labels: Replacements for the built-in strings.
    ///   - onOpenPart: Adds Show in conversation to the details of a span with a
    ///     `partId` or `messageId`.
    public init(
        _ spans: [MonarchAiTraceSpan],
        currency: String = "USD",
        now: TimeInterval? = nil,
        selection: Binding<String?>? = nil,
        filter: Binding<MonarchAiTraceFilter>? = nil,
        expandedIDs: Binding<Set<String>>? = nil,
        labels: MonarchAiTraceViewLabels = MonarchAiTraceViewLabels(),
        onOpenPart: (@MainActor (MonarchAiTraceSpan) -> Void)? = nil
    ) {
        self.spans = spans
        self.currency = currency
        self.now = now
        self.selectionBinding = selection
        self.filterBinding = filter
        self.expandedBinding = expandedIDs
        self.labels = labels
        self.onOpenPart = onOpenPart
    }

    private var selection: Binding<String?> { selectionBinding ?? $ownSelection }
    private var filter: Binding<MonarchAiTraceFilter> { filterBinding ?? $ownFilter }

    private var expanded: Binding<Set<String>> {
        if let expandedBinding { return expandedBinding }
        return Binding(
            get: { ownExpanded ?? MonarchAiTraceViewTokens.defaultExpanded(spans) },
            set: { ownExpanded = $0 }
        )
    }

    private var selectedSpan: Binding<MonarchAiTraceSpan?> {
        Binding(
            get: { selection.wrappedValue.flatMap { id in spans.first { $0.id == id } } },
            set: { selection.wrappedValue = $0?.id }
        )
    }

    /// The content of the view.
    public var body: some View {
        let totals = MonarchAiTraceViewTokens.totals(spans, now: now, currency: currency)
        let rows = MonarchAiTraceViewTokens.rows(
            spans,
            filter: filter.wrappedValue,
            expanded: expanded.wrappedValue,
            totals: totals
        )
        let title = labels.label ?? MonarchAiTraceViewStrings.label
        VStack(alignment: .leading, spacing: MonarchAiTraceViewTokens.spacing) {
            // The title and the filter share a row, as on the web.
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .center) {
                    titleText(title)
                    Spacer(minLength: MonarchAiTraceViewTokens.spacing)
                    filterControl
                }
                VStack(alignment: .leading, spacing: MonarchAiTraceViewTokens.itemSpacing) {
                    titleText(title)
                    filterControl
                }
            }
            totalsView(totals)
            if rows.isEmpty {
                Text(
                    MonarchAiTraceViewTokens.emptyText(
                        filter: filter.wrappedValue,
                        spanCount: spans.count,
                        labels: labels
                    )
                )
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
            } else {
                table(rows, totals: totals)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title)
        .sheet(item: selectedSpan) { span in
            MonarchAiTraceViewDetail(
                span: span,
                currency: currency,
                labels: labels,
                onOpenPart: onOpenPart,
                onClose: { selection.wrappedValue = nil }
            )
        }
    }

    private func titleText(_ title: String) -> some View {
        Text(title)
            .monarchTextStyle(.subheading)
            .foregroundStyle(theme.colors.textDefault)
            .accessibilityAddTraits(.isHeader)
    }

    private var filterControl: some View {
        MonarchSegmentedControl(
            labels.filterLabel ?? MonarchAiTraceViewStrings.filterLabel,
            selection: filter,
            items: MonarchAiTraceFilter.allCases.map { option in
                MonarchSegmentedItem(MonarchAiTraceViewTokens.filterName(option, labels: labels), value: option)
            },
            size: .sm,
            fillsWidth: false
        )
    }

    /// The numbers of the run, each a muted label over its value, as on the web.
    private func totalsView(_ totals: MonarchAiTraceViewTokens.Totals) -> some View {
        MonarchFlowLayout(spacing: MonarchAiTraceViewTokens.spacing) {
            stat(
                labels.totalDuration ?? MonarchAiTraceViewStrings.totalDuration,
                MonarchAiTraceViewTokens.durationText(totals.duration, locale: locale, labels: labels)
            )
            stat(
                labels.totalTokens ?? MonarchAiTraceViewStrings.totalTokens,
                totals.totalTokens.formatted(.number.locale(locale))
            )
            if let cost = MonarchAiTraceViewTokens.totalCostText(totals, locale: locale, labels: labels) {
                stat(labels.totalCost ?? MonarchAiTraceViewStrings.totalCost, cost)
            }
            stat(labels.errors ?? MonarchAiTraceViewStrings.errors, totals.errorCount.formatted(.number.locale(locale)))
        }
    }

    private func stat(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: MonarchAiTraceViewTokens.lineSpacing) {
            Text(label)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
            Text(value)
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors.textDefault)
                .monospacedDigit()
        }
        .accessibilityElement(children: .combine)
    }

    /// The spans as a table that scrolls sideways: Span, Kind, Duration, Tokens, Cost and the timeline bar.
    private func table(_ rows: [MonarchAiTraceViewTokens.Row], totals: MonarchAiTraceViewTokens.Totals) -> some View {
        let shape = RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous)
        // On a width that cannot hold the six columns, a row stacks its facts and keeps the bar in view.
        let isCompact = availableWidth < MonarchAiTraceViewTokens.tableWidth
        return ScrollView(.horizontal, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                if !isCompact { MonarchAiTraceViewTableHeader(labels: labels) }
                ForEach(rows) { row in
                    if !isCompact || row.id != rows.first?.id { MonarchDivider() }
                    MonarchAiTraceViewRow(
                        row: row,
                        totals: totals,
                        isSelected: selection.wrappedValue == row.id,
                        currency: currency,
                        labels: labels,
                        isCompact: isCompact,
                        onToggle: toggle,
                        onSelect: { selection.wrappedValue = $0 }
                    )
                }
            }
            .frame(width: isCompact ? availableWidth : MonarchAiTraceViewTokens.tableWidth, alignment: .leading)
        }
        .onGeometryChange(for: CGFloat.self) {
            $0.size.width
        } action: {
            availableWidth = $0
        }
        .clipShape(shape)
        .overlay(shape.stroke(theme.colors.borderDefault, lineWidth: 1))
    }

    private func toggle(_ id: String) {
        var next = expanded.wrappedValue
        if next.contains(id) { next.remove(id) } else { next.insert(id) }
        expanded.wrappedValue = next
    }
}

#if DEBUG
    #Preview("MonarchAiTraceView") {
        MonarchPreviewMatrix {
            MonarchAiTraceView(
                [
                    MonarchAiTraceSpan(
                        id: "run",
                        kind: .agent,
                        name: "Answer question",
                        status: .ok,
                        start: 0,
                        end: 4.2
                    ),
                    MonarchAiTraceSpan(
                        id: "plan",
                        parentId: "run",
                        kind: .model,
                        name: "Plan",
                        status: .ok,
                        start: 0.1,
                        end: 1.3,
                        inputTokens: 1_200,
                        outputTokens: 180,
                        costMicros: 4_200
                    ),
                    MonarchAiTraceSpan(
                        id: "search",
                        parentId: "run",
                        kind: .tool,
                        name: "search_documents",
                        status: .error,
                        start: 1.4,
                        end: 2.0,
                        error: "Timed out"
                    ),
                ]
            )
        }
    }
#endif
