import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiTraceView``.
enum MonarchAiTraceViewTokens {
    /// The gap between the header, the totals and the rows.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between rows.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the parts of a row.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the lines of a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// How far each level is indented.
    static let childIndent: CGFloat = MonarchTokens.Space.space4
    /// Padding inside a row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space2
    /// The height of the time track.
    static let trackHeight: CGFloat = MonarchAiAgentRunTimelineTokens.trackHeight
    /// The narrowest a bar is drawn, so a very short span stays visible.
    static let minimumBarWidth: CGFloat = MonarchAiAgentRunTimelineTokens.minimumBarWidth
    /// The corner radius of the track and the bars.
    static let barRadius: CGFloat = MonarchAiAgentRunTimelineTokens.radius
    /// The width of the bar that marks the selected row.
    static let selectionBarWidth: CGFloat = MonarchTokens.Space.space1
    /// The chevron of a span with children.
    static let chevronSymbol = "chevron.forward"
    /// The symbol beside a failed span and the error count.
    static let errorSymbol = "exclamationmark.octagon"
    /// The symbol of Show in conversation.
    static let openPartSymbol = "text.bubble"
    /// The turn of an open chevron, a quarter turn.
    static let openRotation: Double = 90
    /// The shortest time scale, so a trace of zero length still divides.
    static let minimumScale: TimeInterval = 0.001
    /// The fill of the track.
    static var track: MonarchColorRole { \.bgSubtle }
    /// The fill of the selected row.
    static var selectedFill: MonarchColorRole { \.bgSubtle }
    /// The bar that marks the selected row.
    static var selectionBar: MonarchColorRole { \.textDefault }
    /// The error text and icon.
    static var errorText: MonarchColorRole { \.dangerStrong }

    /// The fill of a span's bar: by status first (failed, running), then by kind.
    static func barFill(_ span: MonarchAiTraceSpan) -> MonarchColorRole {
        switch span.status {
        case .error: return \.dangerSolid
        case .running: return \.aiThinking
        case .cancelled: return \.borderStrong
        case .ok: break
        }
        switch span.kind {
        case .model: return \.aiSolid
        case .tool: return \.aiTool
        case .retrieval: return \.infoSolid
        case .agent: return \.textDefault
        case .guardrail: return \.warningStrong
        case .other: return \.borderStrong
        }
    }

    /// The chevron's turn, mirrored in right-to-left layouts.
    static func chevronRotation(isExpanded: Bool, layoutDirection: LayoutDirection) -> Angle {
        guard isExpanded else { return .zero }
        return .degrees(layoutDirection == .rightToLeft ? -openRotation : openRotation)
    }

    // MARK: Totals

    /// Totals over a trace.
    struct Totals: Equatable {
        /// The earliest start, or 0 for an empty trace.
        var start: TimeInterval = 0
        /// The latest known time.
        var end: TimeInterval = 0
        var inputTokens = 0
        var outputTokens = 0
        /// Cost per currency, in the order the currencies first appear.
        var costs: [(currency: String, micros: Int)] = []
        var errorCount = 0
        var runningCount = 0
        var spanCount = 0

        var duration: TimeInterval { end - start }
        var totalTokens: Int { inputTokens + outputTokens }

        static func == (lhs: Totals, rhs: Totals) -> Bool {
            lhs.start == rhs.start && lhs.end == rhs.end && lhs.inputTokens == rhs.inputTokens
                && lhs.outputTokens == rhs.outputTokens && lhs.errorCount == rhs.errorCount
                && lhs.runningCount == rhs.runningCount && lhs.spanCount == rhs.spanCount
                && lhs.costs.map(\.currency) == rhs.costs.map(\.currency)
                && lhs.costs.map(\.micros) == rhs.costs.map(\.micros)
        }
    }

    /// Totals over a trace. Tokens and cost add every span, so a parent must
    /// carry only what it used itself. `now` ends the spans still running.
    static func totals(_ spans: [MonarchAiTraceSpan], now: TimeInterval?, currency: String) -> Totals {
        var totals = Totals()
        var start = TimeInterval.infinity
        var end = -TimeInterval.infinity
        var costs: [String: Int] = [:]
        var order: [String] = []
        for span in spans where span.start.isFinite {
            start = min(start, span.start)
            end = max(end, span.start)
        }
        for span in spans {
            if let spanEnd = span.end {
                if spanEnd.isFinite { end = max(end, spanEnd) }
            } else {
                totals.runningCount += 1
            }
            totals.inputTokens += span.inputTokens ?? 0
            totals.outputTokens += span.outputTokens ?? 0
            if span.status == .error { totals.errorCount += 1 }
            if let micros = span.costMicros {
                let code = span.currency ?? currency
                if costs[code] == nil { order.append(code) }
                costs[code, default: 0] += micros
            }
        }
        if !start.isFinite { start = 0 }
        if end < start { end = start }
        if totals.runningCount > 0, let now, now.isFinite { end = max(end, now) }
        totals.start = start
        totals.end = end
        totals.costs = order.map { ($0, costs[$0] ?? 0) }
        totals.spanCount = spans.count
        return totals
    }

    // MARK: Tree

    /// The spans by id and the children of each parent (`nil` for the top
    /// level), in start order. A span whose parent is missing, itself, or in a
    /// loop with it is shown at the top level.
    struct Tree {
        var byId: [String: MonarchAiTraceSpan]
        var children: [String?: [MonarchAiTraceSpan]]
    }

    static func tree(_ spans: [MonarchAiTraceSpan]) -> Tree {
        var byId: [String: MonarchAiTraceSpan] = [:]
        for span in spans where byId[span.id] == nil { byId[span.id] = span }
        func isInLoop(_ span: MonarchAiTraceSpan) -> Bool {
            var seen: Set<String> = []
            var parent = span.parentId
            while let id = parent, let next = byId[id] {
                if id == span.id { return true }
                if !seen.insert(id).inserted { return false }
                parent = next.parentId
            }
            return false
        }
        var children: [String?: [MonarchAiTraceSpan]] = [:]
        for span in spans where byId[span.id] == span {
            let key: String? =
                if let parent = span.parentId, parent != span.id, byId[parent] != nil, !isInLoop(span) {
                    parent
                } else {
                    nil
                }
            children[key, default: []].append(span)
        }
        for key in children.keys {
            children[key]?.sort { ($0.start.isFinite ? $0.start : 0) < ($1.start.isFinite ? $1.start : 0) }
        }
        return Tree(byId: byId, children: children)
    }

    /// The spans open at first: every top-level span, every failed span and
    /// every span a failed span ran inside, so an error is never hidden.
    static func defaultExpanded(_ spans: [MonarchAiTraceSpan]) -> Set<String> {
        let tree = tree(spans)
        var out = Set(tree.children[nil]?.map(\.id) ?? [])
        for span in spans where span.status == .error {
            var seen: Set<String> = []
            var current: MonarchAiTraceSpan? = span
            while let at = current, seen.insert(at.id).inserted {
                out.insert(at.id)
                current = at.parentId.flatMap { tree.byId[$0] }
            }
        }
        return out
    }

    /// The widths of the table columns, in points: span, kind, duration, tokens, cost and timeline.
    static let columns: [CGFloat] = [190, 92, 76, 64, 76, 200]
    /// The width of the whole table.
    static var tableWidth: CGFloat { columns.reduce(0, +) }

    /// One visible row.
    struct Row: Equatable, Identifiable {
        var span: MonarchAiTraceSpan
        var level: Int
        var hasChildren: Bool
        var isExpanded: Bool
        var id: String { span.id }
    }

    /// The rows that are shown for a filter and the open spans: the children
    /// of open spans only, failed spans and their parents for `errors`, and
    /// siblings slowest first for `slowest`.
    static func rows(
        _ spans: [MonarchAiTraceSpan],
        filter: MonarchAiTraceFilter,
        expanded: Set<String>,
        totals: Totals
    ) -> [Row] {
        let tree = tree(spans)
        var keep: Set<String>?
        if filter == .errors {
            var kept = Set<String>()
            for span in tree.byId.values where span.status == .error {
                var current: MonarchAiTraceSpan? = span
                while let at = current, kept.insert(at.id).inserted {
                    current = at.parentId.flatMap { tree.byId[$0] }
                }
            }
            keep = kept
        }
        func length(_ span: MonarchAiTraceSpan) -> TimeInterval { (span.end ?? totals.end) - span.start }
        func kids(_ id: String?) -> [MonarchAiTraceSpan] {
            var list = tree.children[id] ?? []
            if let keep { list = list.filter { keep.contains($0.id) } }
            if filter == .slowest { list.sort { length($0) > length($1) } }
            return list
        }
        var out: [Row] = []
        var visited: Set<String> = []
        func walk(_ id: String?, level: Int) {
            for span in kids(id) where visited.insert(span.id).inserted {
                let hasChildren = !kids(span.id).isEmpty
                let isOpen = hasChildren && expanded.contains(span.id)
                out.append(Row(span: span, level: level, hasChildren: hasChildren, isExpanded: isOpen))
                if isOpen { walk(span.id, level: level + 1) }
            }
        }
        walk(nil, level: 1)
        return out
    }

    /// The bar of a span on the shared scale, as fractions of the width:
    /// where it starts and how wide it is.
    static func bar(_ span: MonarchAiTraceSpan, totals: Totals) -> (offset: Double, width: Double) {
        let scale = max(totals.duration, minimumScale)
        let start = span.start.isFinite ? span.start : totals.start
        let end = max(start, span.end ?? totals.end)
        let offset = min(1, max(0, (start - totals.start) / scale))
        let width = max(0, min(1 - offset, (end - start) / scale))
        return (offset, width.isFinite ? width : 0)
    }

    // MARK: Text

    /// The duration formatting of ``MonarchAiAgentRunTimeline``, so the two read alike.
    private static func timelineLabels(_ labels: MonarchAiTraceViewLabels) -> MonarchAiAgentRunTimelineLabels {
        MonarchAiAgentRunTimelineLabels(
            notAvailable: labels.notAvailable ?? MonarchAiTraceViewStrings.notAvailable,
            formatDuration: labels.formatDuration
        )
    }

    /// A duration: "850 ms", "4.2 s", "2 min 5 s".
    static func durationText(_ seconds: TimeInterval, locale: Locale, labels: MonarchAiTraceViewLabels) -> String {
        // Under ten seconds two decimals are kept ("1.28 s"), as the web table does.
        if labels.formatDuration == nil, seconds >= 1, seconds < 10 {
            return MonarchStrings.string(
                "monarch.internal.elapsed.seconds",
                table: "InternalFormats",
                (seconds).formatted(.number.precision(.fractionLength(0...2)).locale(locale))
            )
        }
        return MonarchAiAgentRunTimelineTokens.durationText(seconds, locale: locale, labels: timelineLabels(labels))
    }

    /// A cost in micros: two decimals, four below one unit.
    static func costText(
        _ micros: Int?,
        currency: String,
        locale: Locale,
        labels: MonarchAiTraceViewLabels
    ) -> String {
        MonarchAiAgentRunTimelineTokens.costText(
            micros,
            currency: currency,
            locale: locale,
            labels: timelineLabels(labels)
        )
    }

    /// Every cost of the totals, joined with "+", or `nil` when no span has a cost.
    static func totalCostText(_ totals: Totals, locale: Locale, labels: MonarchAiTraceViewLabels) -> String? {
        guard !totals.costs.isEmpty else { return nil }
        return totals.costs
            .map { costText($0.micros, currency: $0.currency, locale: locale, labels: labels) }
            .joined(separator: " + ")
    }

    /// The tokens of one span, or `nil` when it has none.
    static func tokens(_ span: MonarchAiTraceSpan) -> Int? {
        guard span.inputTokens != nil || span.outputTokens != nil else { return nil }
        return (span.inputTokens ?? 0) + (span.outputTokens ?? 0)
    }

    /// A span's duration, or "Running".
    static func spanDuration(_ span: MonarchAiTraceSpan, locale: Locale, labels: MonarchAiTraceViewLabels) -> String {
        guard let end = span.end else { return statusName(.running, labels: labels) }
        return durationText(end - span.start, locale: locale, labels: labels)
    }

    /// Where a span sits on the time scale: "Starts at +1.2 s, takes 850 ms".
    static func timing(_ span: MonarchAiTraceSpan, locale: Locale, labels: MonarchAiTraceViewLabels) -> String {
        let start = MonarchAiTraceViewStrings.offset(durationText(span.start, locale: locale, labels: labels))
        let duration = span.end.map { durationText($0 - span.start, locale: locale, labels: labels) }
        if let timing = labels.timing { return timing(start, duration) }
        guard let duration else { return MonarchAiTraceViewStrings.timingRunning(start) }
        return MonarchAiTraceViewStrings.timing(start, duration)
    }

    static func kindName(_ kind: MonarchAiTraceSpanKind, labels: MonarchAiTraceViewLabels) -> String {
        labels.kind?(kind) ?? MonarchAiTraceViewStrings.kind(kind)
    }

    static func statusName(_ status: MonarchAiTraceSpanStatus, labels: MonarchAiTraceViewLabels) -> String {
        labels.status?(status) ?? MonarchAiTraceViewStrings.status(status)
    }

    static func filterName(_ filter: MonarchAiTraceFilter, labels: MonarchAiTraceViewLabels) -> String {
        labels.filter?(filter) ?? MonarchAiTraceViewStrings.filter(filter)
    }

    /// The facts of a row after its name: kind, duration, tokens, cost.
    static func facts(
        _ span: MonarchAiTraceSpan,
        currency: String,
        locale: Locale,
        labels: MonarchAiTraceViewLabels
    ) -> [String] {
        var out = [kindName(span.kind, labels: labels), spanDuration(span, locale: locale, labels: labels)]
        if let tokens = tokens(span) {
            out.append(
                "\(tokens.formatted(.number.locale(locale))) \(labels.totalTokens ?? MonarchAiTraceViewStrings.totalTokens)"
            )
        }
        if let micros = span.costMicros {
            out.append(costText(micros, currency: span.currency ?? currency, locale: locale, labels: labels))
        }
        return out
    }

    /// What VoiceOver reads for a row: name, status unless OK, facts and timing.
    static func rowLabel(
        _ span: MonarchAiTraceSpan,
        currency: String,
        locale: Locale,
        labels: MonarchAiTraceViewLabels
    ) -> String {
        var parts = [span.name]
        if span.status != .ok, span.status != .running { parts.append(statusName(span.status, labels: labels)) }
        parts += facts(span, currency: currency, locale: locale, labels: labels)
        parts.append(timing(span, locale: locale, labels: labels))
        return parts.joined(separator: ", ")
    }

    /// The text shown when there are no rows.
    static func emptyText(filter: MonarchAiTraceFilter, spanCount: Int, labels: MonarchAiTraceViewLabels) -> String {
        filter == .errors && spanCount > 0
            ? labels.noErrors ?? MonarchAiTraceViewStrings.noErrors
            : labels.empty ?? MonarchAiTraceViewStrings.empty
    }

    /// The facts of the details, as name and value, leaving out what is not known.
    static func detailFacts(
        _ span: MonarchAiTraceSpan,
        currency: String,
        locale: Locale,
        labels: MonarchAiTraceViewLabels
    ) -> [(name: String, value: String)] {
        var out: [(name: String, value: String)] = [
            (labels.statusName ?? MonarchAiTraceViewStrings.statusName, statusName(span.status, labels: labels)),
            (labels.kindName ?? MonarchAiTraceViewStrings.kindName, kindName(span.kind, labels: labels)),
            (
                labels.start ?? MonarchAiTraceViewStrings.start,
                MonarchAiTraceViewStrings.offset(durationText(span.start, locale: locale, labels: labels))
            ),
        ]
        if let end = span.end {
            out.append(
                (
                    labels.end ?? MonarchAiTraceViewStrings.end,
                    MonarchAiTraceViewStrings.offset(durationText(end, locale: locale, labels: labels))
                )
            )
        }
        out.append(
            (labels.duration ?? MonarchAiTraceViewStrings.duration, spanDuration(span, locale: locale, labels: labels))
        )
        if let tokens = span.inputTokens {
            out.append(
                (labels.inputTokens ?? MonarchAiTraceViewStrings.inputTokens, tokens.formatted(.number.locale(locale)))
            )
        }
        if let tokens = span.outputTokens {
            out.append(
                (
                    labels.outputTokens ?? MonarchAiTraceViewStrings.outputTokens,
                    tokens.formatted(.number.locale(locale))
                )
            )
        }
        if let micros = span.costMicros {
            out.append(
                (
                    labels.totalCost ?? MonarchAiTraceViewStrings.totalCost,
                    costText(micros, currency: span.currency ?? currency, locale: locale, labels: labels)
                )
            )
        }
        if let model = span.model, !model.isEmpty {
            out.append((labels.model ?? MonarchAiTraceViewStrings.model, model))
        }
        return out
    }

    /// The attributes sorted by name.
    static func attributes(_ span: MonarchAiTraceSpan) -> [(name: String, value: String)] {
        span.attributes.sorted { $0.key < $1.key }.map { ($0.key, $0.value) }
    }

    /// Whether the details offer Show in conversation.
    static func canOpenPart(_ span: MonarchAiTraceSpan, hasHandler: Bool) -> Bool {
        hasHandler && (span.partId != nil || span.messageId != nil)
    }
}
