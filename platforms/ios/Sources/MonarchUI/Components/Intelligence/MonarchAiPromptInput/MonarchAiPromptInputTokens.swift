import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiPromptInput``.
enum MonarchAiPromptInputTokens {
    /// The gap between the assistant mark and the text of the field.
    static let fieldGlyphSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the label, the menu, the box and the footer.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the parts inside the box.
    static let boxSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the controls of the bar under the text.
    static let barSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding inside the box.
    static let padding: CGFloat = MonarchTokens.Space.space2
    /// Padding before and after the text inside the box.
    static let textPadding: CGFloat = MonarchTokens.Space.space2
    /// The most lines the text grows to by default.
    static let defaultMaxLines = 8
    /// The SF Symbols of the prompt.
    static let sendSymbol = "arrow.up"
    static let attachSymbol = "paperclip"
    static let dropSymbol = "square.and.arrow.down"

    /// How close the text is to its limit.
    enum Limit: Hashable {
        /// Under the warning share, or no limit.
        case ok
        /// At or over the warning share of the limit.
        case warning
        /// Over the limit: sending is off.
        case over
    }

    /// The limit state of `count` characters, as on the web: over above
    /// `maxLength`, warning from `warnAt` of it (rounded down), ok otherwise.
    static func limit(count: Int, maxLength: Int?, warnAt: Double) -> Limit {
        guard let maxLength else { return .ok }
        if count > maxLength { return .over }
        let threshold = Int((Double(maxLength) * min(max(warnAt, 0), 1)).rounded(.down))
        return count >= threshold ? .warning : .ok
    }

    /// What to announce when the limit state changes: only on the way up,
    /// never when it returns to ok.
    static func announcement(
        from previous: Limit,
        to current: Limit,
        count: Int,
        maxLength: Int?,
        labels: MonarchAiPromptInputLabels
    ) -> String? {
        guard let maxLength, previous != current else { return nil }
        switch (previous, current) {
        case (.ok, .warning): return warningText(maxLength - count, labels: labels)
        case (_, .over): return overText(count - maxLength, labels: labels)
        default: return nil
        }
    }

    /// The characters left near the limit.
    static func warningText(_ remaining: Int, labels: MonarchAiPromptInputLabels) -> String {
        labels.counterWarning?(remaining) ?? MonarchAiPromptInputStrings.counterWarning(remaining)
    }

    /// The characters over the limit.
    static func overText(_ over: Int, labels: MonarchAiPromptInputLabels) -> String {
        labels.counterOver?(over) ?? MonarchAiPromptInputStrings.counterOver(over)
    }

    /// The visible counter, "120 / 4000".
    static func counterText(count: Int, maxLength: Int, labels: MonarchAiPromptInputLabels) -> String {
        labels.counter?(count, maxLength) ?? MonarchAiPromptInputStrings.counter(count, maxLength)
    }

    /// The spoken counter: the count of the limit, or by how much it is over.
    static func counterLabel(count: Int, maxLength: Int, labels: MonarchAiPromptInputLabels) -> String {
        if count > maxLength { return overText(count - maxLength, labels: labels) }
        return labels.counterLabel?(count, maxLength) ?? MonarchAiPromptInputStrings.counterLabel(count, maxLength)
    }

    /// The token estimate, "≈ 120 tokens".
    static func tokensText(_ count: Int, labels: MonarchAiPromptInputLabels) -> String {
        labels.tokens?(count) ?? MonarchAiPromptInputStrings.tokens(count)
    }

    /// The colour of the counter: muted, the strong warning near the limit, the strong danger over it.
    static func counterForeground(_ limit: Limit) -> MonarchColorRole {
        switch limit {
        case .ok: \.textMuted
        case .warning: \.warningStrong
        case .over: \.dangerStrong
        }
    }

    /// The fill of the box: the accent tone while files are dragged over it.
    static func background(isDragging: Bool, isEnabled: Bool) -> MonarchColorRole {
        if !isEnabled { return \.bgSubtle }
        return isDragging ? MonarchTonePalette.palette(for: .accent).subtle : \.bgSurface
    }

    /// The border of the box: danger over the limit, accent while dragging.
    static func border(limit: Limit, isDragging: Bool, isEnabled: Bool) -> MonarchColorRole {
        if !isEnabled { return \.borderSubtle }
        if isDragging { return MonarchTonePalette.palette(for: .accent).border }
        return limit == .over ? \.dangerBorder : \.borderInput
    }

    /// The text colour.
    static func foreground(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// Whether the prompt can be sent: enabled, not busy, within the limit,
    /// and with text that is not only white space or with attachments.
    static func canSubmit(
        text: String,
        hasAttachments: Bool,
        maxLength: Int?,
        isEnabled: Bool,
        isBusy: Bool
    ) -> Bool {
        guard isEnabled, !isBusy else { return false }
        if let maxLength, text.count > maxLength { return false }
        return !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || hasAttachments
    }

    /// The text after a send failed: the sent text comes back unless
    /// something new was typed meanwhile.
    static func restored(current: String, sent: String) -> String {
        current.isEmpty ? sent : current
    }

    /// The visible line range, with at least one line and `maxLines` no smaller than `minLines`.
    static func lines(min minLines: Int, max maxLines: Int) -> ClosedRange<Int> {
        let low = Swift.max(1, minLines)
        return low...Swift.max(low, maxLines)
    }

    /// Whether a Return press from a hardware keyboard sends: plain Return
    /// with `.returnKey`, Command-Return with either.
    static func sends(submitOn: MonarchAiPromptInputSubmitOn, modifiers: EventModifiers) -> Bool {
        if modifiers == .command { return true }
        return submitOn == .returnKey && modifiers.isEmpty
    }
}
