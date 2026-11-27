import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAiAgentRunTimeline``.
enum MonarchAiAgentRunTimelineTokens {
    /// The gap between the header, the totals and the events.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between events.
    static let eventSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap inside an event's header and between facts.
    static let innerSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between a fact's name and its value.
    static let factSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The height of a bar's track. It holds no text.
    static let trackHeight: CGFloat = MonarchTokens.Space.space3
    /// The narrowest a bar is drawn, so an instant event stays visible.
    static let minimumBarWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The corner radius of tracks, bars and the kind label.
    static let radius: CGFloat = MonarchTokens.Radius.xs
    /// The horizontal padding of the kind label.
    static let kindPadding: CGFloat = MonarchTokens.Space.space1
    /// The horizontal padding of a table cell.
    static let cellPadding: CGFloat = MonarchTokens.Space.space3
    /// The vertical padding of a table cell.
    static let cellVerticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The track's background.
    static var track: MonarchColorRole { \.bgSubtle }
    /// The colour mixed into a running event's bar.
    static var running: MonarchColorRole { \.aiThinking }

    // MARK: Totals and rows

    /// Totals over a run: its start and end, wall-clock time, tokens and cost.
    struct Totals: Equatable {
        /// The earliest start.
        let start: Date
        /// The latest known time, or `now` when an event is running.
        let end: Date
        /// The total of input tokens.
        let inputTokens: Int
        /// The total of output tokens.
        let outputTokens: Int
        /// The total cost in micros.
        let costMicros: Int
        /// Whether any event has a cost.
        let hasCost: Bool

        /// The wall-clock time of the run, in seconds.
        var duration: TimeInterval { end.timeIntervalSince(start) }
    }

    /// The totals of a run. A running event ends at `now` when given, else at
    /// the latest known time, so the result never depends on the clock.
    static func totals(_ events: [MonarchAiRunEvent], now: Date?) -> Totals {
        let starts = events.map(\.startedAt)
        let ends = events.compactMap(\.endedAt)
        let start = starts.min() ?? now ?? Date(timeIntervalSinceReferenceDate: 0)
        let latest = (starts + ends + [start]).max() ?? start
        var end = latest
        if let now, events.contains(where: { $0.endedAt == nil }) {
            end = max(latest, now)
        }
        return Totals(
            start: start,
            end: end,
            inputTokens: events.reduce(0) { $0 + ($1.inputTokens ?? 0) },
            outputTokens: events.reduce(0) { $0 + ($1.outputTokens ?? 0) },
            costMicros: events.reduce(0) { $0 + ($1.costMicros ?? 0) },
            hasCost: events.contains { $0.costMicros != nil }
        )
    }

    /// One event placed on the run's time axis.
    struct Row: Identifiable, Equatable {
        /// The event.
        let event: MonarchAiRunEvent
        /// When it started.
        let start: Date
        /// When it ended, or the end of the run while running. Never before `start`.
        let end: Date
        /// Whether it has not ended.
        let isRunning: Bool

        /// The event's id.
        var id: String { event.id }
        /// The time it took, in seconds.
        var duration: TimeInterval { end.timeIntervalSince(start) }
    }

    /// The events sorted by start time, each with its end.
    static func rows(_ events: [MonarchAiRunEvent], totals: Totals) -> [Row] {
        events
            .map { event in
                let end = event.endedAt ?? totals.end
                return Row(
                    event: event,
                    start: event.startedAt,
                    end: max(event.startedAt, end),
                    isRunning: event.endedAt == nil
                )
            }
            .enumerated()
            .sorted { lhs, rhs in
                lhs.element.start == rhs.element.start ? lhs.offset < rhs.offset : lhs.element.start < rhs.element.start
            }
            .map(\.element)
    }

    /// Where a bar starts on the track, 0 through 1, from the leading edge.
    static func offsetFraction(_ row: Row, totals: Totals) -> CGFloat {
        let span = max(totals.duration, 0.001)
        return CGFloat(min(1, max(0, row.start.timeIntervalSince(totals.start) / span)))
    }

    /// How much of the track a bar covers, 0 through 1.
    static func widthFraction(_ row: Row, totals: Totals) -> CGFloat {
        let span = max(totals.duration, 0.001)
        let remaining = 1 - offsetFraction(row, totals: totals)
        return min(remaining, CGFloat(max(0, row.duration / span)))
    }

    /// The times written on the axis: the start, the middle and the end of the run.
    static func ticks(_ totals: Totals) -> [TimeInterval] {
        [0, totals.duration / 2, totals.duration]
    }

    /// Whether an open event offers "Show in conversation".
    static func canOpenPart(_ event: MonarchAiRunEvent, hasHandler: Bool) -> Bool {
        hasHandler && (event.partId != nil || event.messageId != nil)
    }

    // MARK: Colours

    /// The fill of a bar by kind.
    static func barFill(_ kind: MonarchAiRunEventKind) -> MonarchColorRole {
        switch kind {
        case .model: \.aiSolid
        case .tool: \.aiTool
        case .handoff: \.infoSolid
        case .error: \.dangerSolid
        }
    }

    /// The background of the kind label.
    static func kindFill(_ kind: MonarchAiRunEventKind) -> MonarchColorRole {
        kind == .error ? \.dangerSubtle : \.bgMuted
    }

    /// The text colour of the kind label.
    static func kindText(_ kind: MonarchAiRunEventKind) -> MonarchColorRole {
        kind == .error ? \.dangerStrong : \.textMuted
    }

    // MARK: Text

    /// The name of a kind.
    static func kindName(_ kind: MonarchAiRunEventKind, labels: MonarchAiAgentRunTimelineLabels) -> String {
        labels.kind?(kind) ?? MonarchAiAgentRunTimelineStrings.kind(kind)
    }

    /// The name of a view.
    static func modeName(_ mode: MonarchAiAgentRunTimelineMode, labels: MonarchAiAgentRunTimelineLabels) -> String {
        labels.mode?(mode) ?? MonarchAiAgentRunTimelineStrings.mode(mode)
    }

    /// A duration: milliseconds under a second, seconds with one decimal under ten
    /// seconds, whole seconds under a minute, then minutes and seconds.
    static func durationText(
        _ seconds: TimeInterval,
        locale: Locale,
        labels: MonarchAiAgentRunTimelineLabels
    ) -> String {
        if let format = labels.formatDuration { return format(seconds) }
        return MonarchFormats.elapsed(seconds, locale: locale)
    }

    /// An offset from the start of the run, such as "+1.2 s".
    static func offsetText(_ seconds: TimeInterval, locale: Locale, labels: MonarchAiAgentRunTimelineLabels) -> String {
        if let format = labels.formatOffset { return format(seconds) }
        return MonarchAiAgentRunTimelineStrings.offset(durationText(seconds, locale: locale, labels: labels))
    }

    /// A row's duration, or "Running".
    static func rowDuration(_ row: Row, locale: Locale, labels: MonarchAiAgentRunTimelineLabels) -> String {
        row.isRunning
            ? labels.running ?? MonarchAiAgentRunTimelineStrings.running
            : durationText(row.duration, locale: locale, labels: labels)
    }

    /// A token count, or the not-available mark.
    static func tokensText(_ count: Int?, locale: Locale, labels: MonarchAiAgentRunTimelineLabels) -> String {
        guard let count else { return notAvailable(labels) }
        return count.formatted(.number.locale(locale))
    }

    /// A cost in micros, in the currency: two decimals, four below one unit.
    static func costText(
        _ micros: Int?,
        currency: String,
        locale: Locale,
        labels: MonarchAiAgentRunTimelineLabels
    ) -> String {
        guard let micros else { return notAvailable(labels) }
        return MonarchFormats.cost(micros: micros, currency: currency, locale: locale)
    }

    /// The mark for a value that is not known.
    static func notAvailable(_ labels: MonarchAiAgentRunTimelineLabels) -> String {
        labels.notAvailable ?? MonarchAiAgentRunTimelineStrings.notAvailable
    }

    /// A name and its value read as one, such as "Total time, 4.2 s".
    static func fact(_ name: String, _ value: String) -> String {
        MonarchAiAgentRunTimelineStrings.fact(name, value)
    }

    /// What VoiceOver reads for an event's header: its kind, its name and its duration or "Running".
    static func rowLabel(_ row: Row, locale: Locale, labels: MonarchAiAgentRunTimelineLabels) -> String {
        [kindName(row.event.kind, labels: labels), row.event.name, rowDuration(row, locale: locale, labels: labels)]
            .joined(separator: ", ")
    }

    /// The facts of an open event, or of a table row, as names and values. Unknown tokens and
    /// cost are left out of an open event (`includesUnknown` false) and shown as the
    /// not-available mark in the table.
    static func facts(
        _ row: Row,
        totals: Totals,
        currency: String,
        locale: Locale,
        labels: MonarchAiAgentRunTimelineLabels,
        includesUnknown: Bool
    ) -> [(name: String, value: String)] {
        let event = row.event
        var facts: [(name: String, value: String)] = [
            (
                labels.start ?? MonarchAiAgentRunTimelineStrings.start,
                offsetText(row.start.timeIntervalSince(totals.start), locale: locale, labels: labels)
            ),
            (
                labels.duration ?? MonarchAiAgentRunTimelineStrings.duration,
                rowDuration(row, locale: locale, labels: labels)
            ),
        ]
        if includesUnknown || event.inputTokens != nil {
            facts.append(
                (
                    labels.inputTokens ?? MonarchAiAgentRunTimelineStrings.inputTokens,
                    tokensText(event.inputTokens, locale: locale, labels: labels)
                )
            )
        }
        if includesUnknown || event.outputTokens != nil {
            facts.append(
                (
                    labels.outputTokens ?? MonarchAiAgentRunTimelineStrings.outputTokens,
                    tokensText(event.outputTokens, locale: locale, labels: labels)
                )
            )
        }
        if (includesUnknown && totals.hasCost) || event.costMicros != nil {
            facts.append(
                (
                    labels.cost ?? MonarchAiAgentRunTimelineStrings.cost,
                    costText(event.costMicros, currency: currency, locale: locale, labels: labels)
                )
            )
        }
        return facts
    }

    /// The totals as names and values: time, tokens and, when any event has one, cost.
    static func totalFacts(
        _ totals: Totals,
        currency: String,
        locale: Locale,
        labels: MonarchAiAgentRunTimelineLabels
    ) -> [(name: String, value: String)] {
        var facts: [(name: String, value: String)] = [
            (
                labels.totalDuration ?? MonarchAiAgentRunTimelineStrings.totalDuration,
                durationText(totals.duration, locale: locale, labels: labels)
            ),
            (
                labels.inputTokens ?? MonarchAiAgentRunTimelineStrings.inputTokens,
                tokensText(totals.inputTokens, locale: locale, labels: labels)
            ),
            (
                labels.outputTokens ?? MonarchAiAgentRunTimelineStrings.outputTokens,
                tokensText(totals.outputTokens, locale: locale, labels: labels)
            ),
        ]
        if totals.hasCost {
            facts.append(
                (
                    labels.cost ?? MonarchAiAgentRunTimelineStrings.cost,
                    costText(totals.costMicros, currency: currency, locale: locale, labels: labels)
                )
            )
        }
        return facts
    }

    /// What VoiceOver reads for a table row: the name, the kind, then every known fact.
    static func tableRowSummary(
        _ row: Row,
        totals: Totals,
        currency: String,
        locale: Locale,
        labels: MonarchAiAgentRunTimelineLabels
    ) -> String {
        let facts = Self.facts(
            row,
            totals: totals,
            currency: currency,
            locale: locale,
            labels: labels,
            includesUnknown: false
        )
        return ([row.event.name, kindName(row.event.kind, labels: labels)] + facts.map { fact($0.name, $0.value) })
            .joined(separator: ", ")
    }
}
