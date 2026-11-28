import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchAiReasoning``.
enum MonarchAiReasoningTokens {
    /// The gap between the glyph and the summary of the still row.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space2
    /// The inset of the reasoning text from its leading rule.
    static let bodyInset: CGFloat = MonarchTokens.Space.space3
    /// The width of the leading rule beside the reasoning text.
    static let ruleWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The minimum height of the still row, matching the disclosure header.
    static let rowMinHeight: CGFloat = MonarchAccessibility.minimumHitTarget
    /// The colour role of the glyph of the still row.
    static var glyph: MonarchColorRole { \.aiStrong }
    /// The colour role of the summary of the still row.
    static var summaryText: MonarchColorRole { \.textMuted }
    /// The colour role of the reasoning text.
    static var bodyText: MonarchColorRole { \.textMuted }
    /// The colour role of the leading rule.
    static var rule: MonarchColorRole { \.aiBorder }

    /// Whether the part has text to disclose: not redacted and not empty.
    static func hasBody(_ part: MonarchAiReasoningPart) -> Bool {
        !part.isRedacted && !part.text.isEmpty
    }

    /// Whole seconds for a duration in milliseconds, rounded, at least 1.
    static func seconds(ms: Int) -> Int {
        max(1, Int((Double(ms) / 1000).rounded()))
    }

    /// The formatted duration: "12s", "1m 5s".
    static func durationText(ms: Int, locale: Locale, labels: MonarchAiReasoningLabels) -> String {
        let whole = seconds(ms: ms)
        return labels.duration?(whole) ?? MonarchAiThinkingIndicatorTokens.elapsedText(seconds: whole, locale: locale)
    }

    /// The summary in the header: "Thinking…" while streaming, "Reasoning
    /// hidden" when redacted, "Thought for 12s" with a duration from the part
    /// or measured by the view, "Thought" without one.
    static func summary(
        part: MonarchAiReasoningPart,
        measuredMs: Int?,
        locale: Locale,
        labels: MonarchAiReasoningLabels
    ) -> String {
        if part.state == .streaming { return labels.thinking ?? MonarchAiReasoningStrings.thinking }
        if part.isRedacted { return labels.reasoningHidden ?? MonarchAiReasoningStrings.reasoningHidden }
        guard let ms = part.durationMs ?? measuredMs else { return labels.thought ?? MonarchAiReasoningStrings.thought }
        let duration = durationText(ms: ms, locale: locale, labels: labels)
        return labels.thoughtFor?(duration) ?? MonarchAiReasoningStrings.thoughtFor(duration)
    }

    /// What VoiceOver reads for the header: "Assistant reasoning: Thought for 12s".
    static func accessibilityLabel(summary: String, labels: MonarchAiReasoningLabels) -> String {
        "\(labels.prefix ?? MonarchAiReasoningStrings.prefix): \(summary)"
    }

    /// Whether the disclosure is open after a transition: it opens when
    /// reasoning starts again and closes when it finishes, unless the person
    /// opened or closed it while it streamed.
    static func expanded(after transition: MonarchAiReasoningTransition, isExpanded: Bool, isTouched: Bool) -> Bool {
        switch transition {
        case .none: isExpanded
        case .started: true
        case .finished: isTouched ? isExpanded : false
        }
    }
}

/// What changed in a reasoning part's streaming state.
enum MonarchAiReasoningTransition: Equatable {
    /// Nothing that matters.
    case none
    /// The part started streaming again, for example after a regeneration.
    case started
    /// The part stopped streaming.
    case finished
}

/// Measures how long a reasoning part streamed. Every time is passed in, so
/// the logic is tested without a clock.
struct MonarchAiReasoningTimeline: Equatable {
    /// When the current stream started, while it streams.
    private(set) var start: Date?
    /// The measured length of the last stream, in milliseconds.
    private(set) var measuredMs: Int?
    /// Whether the part streamed at the last update.
    private(set) var isStreaming: Bool

    /// A timeline for a part in its first state.
    init(isStreaming: Bool, start: Date?) {
        self.isStreaming = isStreaming
        self.start = isStreaming ? start : nil
    }

    /// Records the part's streaming state at `now` and says what changed.
    mutating func update(isStreaming streaming: Bool, now: Date) -> MonarchAiReasoningTransition {
        defer { isStreaming = streaming }
        if streaming {
            if start == nil { start = now }
            return isStreaming ? .none : .started
        }
        guard isStreaming else { return .none }
        if let start {
            measuredMs = max(0, Int((now.timeIntervalSince(start) * 1000).rounded()))
        }
        start = nil
        return .finished
    }
}
