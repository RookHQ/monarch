import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAiDraftQueue``.
enum MonarchAiDraftQueueTokens {
    /// The gap between the header and the drafts, and between drafts.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between the parts of a draft.
    static let cardSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between buttons and between small items.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between a heading and its text.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding inside the context block.
    static let contextPadding: CGFloat = MonarchTokens.Space.space3
    /// The corner radius of the context block.
    static let contextRadius: CGFloat = MonarchTokens.Radius.md
    /// The least number of lines of the editor.
    static let editorMinLines = 4
    /// The most lines the editor grows to before it scrolls.
    static let editorMaxLines = 12

    /// A decision offered on a draft.
    enum Action: Equatable, CaseIterable {
        case approve, send, edit, regenerate, skip
    }

    /// The tone of a status badge.
    static func tone(_ status: MonarchAiDraftStatus) -> MonarchTone {
        switch status {
        case .skipped: .neutral
        case .draft, .regenerating: .ai
        case .approved, .sent: .success
        }
    }

    /// The glyph before an action's label, as the web draws them: a check, a pencil, a retry arrow.
    static func actionSymbol(_ action: Action) -> String? {
        switch action {
        case .approve: "checkmark"
        case .edit: "pencil"
        case .regenerate: "arrow.clockwise"
        case .send, .skip: nil
        }
    }

    /// The SF Symbol of a status, so the status does not depend on colour.
    static func symbol(_ status: MonarchAiDraftStatus) -> String {
        switch status {
        case .draft: "pencil"
        case .regenerating: "arrow.clockwise"
        case .approved: "checkmark"
        case .sent: "paperplane.fill"
        case .skipped: "minus"
        }
    }

    /// Whether a decision has been taken on the draft.
    static func isReviewed(_ status: MonarchAiDraftStatus) -> Bool {
        status == .approved || status == .sent || status == .skipped
    }

    /// The number of drafts with a decision.
    static func reviewedCount(_ drafts: [MonarchAiDraft]) -> Int {
        drafts.filter { isReviewed($0.status) }.count
    }

    /// The share of drafts reviewed, 0 to 1.
    static func fraction(_ drafts: [MonarchAiDraft]) -> Double {
        drafts.isEmpty ? 0 : Double(reviewedCount(drafts)) / Double(drafts.count)
    }

    /// The decisions offered for a status. Nothing is offered while a
    /// draft is being written, or once it is sent or skipped.
    static func actions(_ status: MonarchAiDraftStatus, canSend: Bool) -> [Action] {
        switch status {
        case .draft: [.approve, .edit, .regenerate, .skip]
        case .approved: canSend ? [.send, .edit] : [.edit]
        case .regenerating, .sent, .skipped: []
        }
    }

    /// The button variant of a decision: approve and send are primary.
    static func variant(_ action: Action) -> MonarchButtonVariant {
        switch action {
        case .approve, .send: .primary
        case .edit, .regenerate: .secondary
        case .skip: .ghost
        }
    }

    /// The title of a decision.
    static func title(_ action: Action, labels: MonarchAiDraftQueueLabels) -> String {
        switch action {
        case .approve: labels.approve
        case .send: labels.send
        case .edit: labels.edit
        case .regenerate: labels.regenerate
        case .skip: labels.skip
        }
    }

    /// Whether a decision moves VoiceOver to the next draft.
    static func advances(_ action: Action) -> Bool {
        action == .approve || action == .send || action == .skip
    }

    /// The text colour of a body: muted once skipped.
    static func bodyRole(_ status: MonarchAiDraftStatus) -> MonarchColorRole {
        status == .skipped ? \.textMuted : \.textDefault
    }

    /// The next draft that needs review after `id`, wrapping to the top,
    /// or `nil` when no other draft needs review.
    static func next(after id: String, in drafts: [MonarchAiDraft]) -> String? {
        guard let index = drafts.firstIndex(where: { $0.id == id }) else {
            return drafts.first { $0.status == .draft }?.id
        }
        let ordered = drafts[(index + 1)...] + drafts[..<index]
        return ordered.first { $0.status == .draft }?.id
    }

    /// The edited text to save: trimmed, or `nil` when it is empty or unchanged.
    static func edited(_ draft: String, original: String) -> String? {
        let trimmed = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, trimmed != original.trimmingCharacters(in: .whitespacesAndNewlines) else {
            return nil
        }
        return trimmed
    }

    /// The announcements for status changes since the previous statuses.
    /// Drafts that were not there before are not announced.
    static func announcements(
        previous: [String: MonarchAiDraftStatus],
        drafts: [MonarchAiDraft],
        labels: MonarchAiDraftQueueLabels
    ) -> [String] {
        drafts.compactMap { draft in
            guard let old = previous[draft.id], old != draft.status else { return nil }
            return labels.announce(draft.status, draft.title)
        }
    }

    /// The statuses by id.
    static func statuses(_ drafts: [MonarchAiDraft]) -> [String: MonarchAiDraftStatus] {
        Dictionary(drafts.map { ($0.id, $0.status) }, uniquingKeysWith: { first, _ in first })
    }

    /// What VoiceOver reads for a draft's body: "AI draft" and the text.
    static func bodyLabel(_ body: String, labels: MonarchAiDraftQueueLabels) -> String {
        labels.aiDraft + ", " + body
    }
}
