import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiEditProposal``.
enum MonarchAiEditProposalTokens {
    /// How a run of diff text is drawn.
    struct SegmentStyle: Equatable {
        /// The fill behind the text, or none.
        var background: MonarchColorRole?
        /// The colour of the text, or `nil` to keep the paragraph's.
        var foreground: MonarchColorRole?
        /// Struck through: removed text.
        var isStruck: Bool
        /// Underlined: added text.
        var isUnderlined: Bool
    }

    /// The gap between the header, the changes and the actions.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the parts of the header.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the title and its badges.
    static let badgeSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap inside a change.
    static let hunkSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between buttons.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding inside the card.
    static let padding: CGFloat = MonarchTokens.Space.space4
    /// Padding inside a change.
    static let hunkPadding: CGFloat = MonarchTokens.Space.space3
    /// The corner radius of the card.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.lg
    /// The corner radius of a change.
    static let hunkCornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The width of the AI bar on the leading edge of the card.
    static let barWidth: CGFloat = MonarchTokens.Space.space0_5 + MonarchTokens.Space.px
    /// The gap between the decision icon and its text.
    static let statusSpacing: CGFloat = MonarchTokens.Space.space1
    /// The weight of the title.
    static let titleWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The weight of a change's name and decision.
    static let labelWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)
    /// The SF Symbol of accept and accepted.
    static let acceptSymbol = "checkmark"
    /// The SF Symbol of reject and rejected.
    static let rejectSymbol = "xmark"
    /// The SF Symbol of revert.
    static let revertSymbol = "arrow.uturn.backward"
    /// The card's border.
    static var border: MonarchColorRole { \.borderDefault }
    /// The AI bar.
    static var bar: MonarchColorRole { \.aiBorder }
    /// The fill of a change.
    static var hunkBackground: MonarchColorRole { \.bgSubtle }
    /// The border of a change.
    static var hunkBorder: MonarchColorRole { \.borderSubtle }

    /// The tone of the state badge.
    static func tone(_ state: MonarchAiEditProposalState) -> MonarchTone {
        switch state {
        case .proposed: .info
        case .applied: .success
        case .rejected: .neutral
        case .reverted: .warning
        }
    }

    /// The colour of a decision.
    static func statusRole(_ status: MonarchAiEditHunkStatus) -> MonarchColorRole {
        status == .accepted ? \.successStrong : \.textMuted
    }

    /// The decision shown for a change: the app's, else the remembered one,
    /// overridden by the proposal's state. Once applied, only rejected
    /// changes stay rejected; a rejected proposal rejects every change; a
    /// reverted proposal shows every change undecided, because nothing of
    /// it is in the document any more.
    static func status(
        of hunk: MonarchAiEditHunk,
        decided: [String: MonarchAiEditHunkStatus],
        state: MonarchAiEditProposalState
    ) -> MonarchAiEditHunkStatus {
        let own = hunk.status ?? decided[hunk.id] ?? .proposed
        switch state {
        case .proposed: return own
        case .applied: return own == .rejected ? .rejected : .accepted
        case .rejected: return .rejected
        case .reverted: return .proposed
        }
    }

    /// The decisions after deciding every undecided change at once.
    static func decideAll(
        _ hunks: [MonarchAiEditHunk],
        statuses: [MonarchAiEditHunkStatus],
        as status: MonarchAiEditHunkStatus,
        decided: [String: MonarchAiEditHunkStatus]
    ) -> [String: MonarchAiEditHunkStatus] {
        var next = decided
        for (hunk, current) in zip(hunks, statuses) where current == .proposed {
            next[hunk.id] = status
        }
        return next
    }

    /// Whether Accept all and Reject all are shown.
    static func showsAll(state: MonarchAiEditProposalState, pending: Int, hasAllActions: Bool) -> Bool {
        state == .proposed && pending > 0 && hasAllActions
    }

    /// Whether a change shows its own buttons.
    static func showsHunkActions(
        state: MonarchAiEditProposalState,
        status: MonarchAiEditHunkStatus,
        hasHunkActions: Bool
    ) -> Bool {
        state == .proposed && status == .proposed && hasHunkActions
    }

    /// The change that takes focus after change `index` is decided: the next
    /// undecided one, else the previous one, else `nil` for the title.
    static func nextFocus(after index: Int, statuses: [MonarchAiEditHunkStatus]) -> Int? {
        let pending = statuses.indices.filter { statuses[$0] == .proposed && $0 != index }
        return pending.first { $0 > index } ?? pending.last { $0 < index }
    }

    /// How a run of diff text is drawn for a decision. Undecided changes
    /// show both sides tinted; a decided change mutes the losing side and
    /// shows the winning side as plain text.
    static func style(_ kind: MonarchAiEditProposalSegment.Kind, status: MonarchAiEditHunkStatus) -> SegmentStyle {
        switch (kind, status) {
        case (.equal, _):
            SegmentStyle(background: nil, foreground: nil, isStruck: false, isUnderlined: false)
        case (.delete, .proposed):
            SegmentStyle(
                background: \.aiDiffRemove,
                foreground: \.aiDiffRemoveStrong,
                isStruck: true,
                isUnderlined: false
            )
        case (.insert, .proposed):
            SegmentStyle(background: \.aiDiffAdd, foreground: \.aiDiffAddStrong, isStruck: false, isUnderlined: true)
        case (.delete, .accepted):
            SegmentStyle(background: nil, foreground: \.textSubtle, isStruck: true, isUnderlined: false)
        case (.insert, .accepted):
            SegmentStyle(background: nil, foreground: nil, isStruck: false, isUnderlined: true)
        case (.delete, .rejected):
            SegmentStyle(background: nil, foreground: nil, isStruck: false, isUnderlined: false)
        case (.insert, .rejected):
            SegmentStyle(background: nil, foreground: \.textSubtle, isStruck: false, isUnderlined: true)
        }
    }

    /// Whether the diff changes anything.
    static func isChanged(_ segments: [MonarchAiEditProposalSegment]) -> Bool {
        segments.contains { $0.kind != .equal }
    }

    /// What VoiceOver reads for a diff: removed and added runs after their prefixes.
    static func spokenDiff(_ segments: [MonarchAiEditProposalSegment], deleted: String, inserted: String) -> String {
        segments
            .map { segment in
                switch segment.kind {
                case .equal: segment.text
                case .delete: " \(deleted) \(segment.text) "
                case .insert: " \(inserted) \(segment.text) "
                }
            }
            .joined()
            .split(whereSeparator: \.isWhitespace)
            .joined(separator: " ")
    }

    /// The count line: "3 changes", "1 of 3 changes left".
    static func countText(total: Int, pending: Int, labels: MonarchAiEditProposalLabels) -> String {
        if let count = labels.count { return count(total, pending) }
        return pending == total
            ? MonarchAiEditProposalStrings.count(total)
            : MonarchAiEditProposalStrings.left(pending, of: total)
    }

    /// The name of a change: its label, else "Change 2 of 3".
    static func hunkName(_ hunk: MonarchAiEditHunk, index: Int, total: Int, labels: MonarchAiEditProposalLabels)
        -> String
    {
        if let label = hunk.label, !label.isEmpty { return label }
        return labels.hunkName?(index + 1, total) ?? MonarchAiEditProposalStrings.hunkName(index + 1, total)
    }

    /// What is announced after a decision.
    static func announcement(_ event: MonarchAiEditProposalEvent, left: Int, labels: MonarchAiEditProposalLabels)
        -> String
    {
        if let announcement = labels.announcement { return announcement(event, left) }
        switch event {
        case .acceptedAll: return MonarchAiEditProposalStrings.acceptedAll
        case .rejectedAll: return MonarchAiEditProposalStrings.rejectedAll
        case .accepted, .rejected:
            let done =
                event == .accepted ? MonarchAiEditProposalStrings.acceptedOne : MonarchAiEditProposalStrings.rejectedOne
            let rest = left > 0 ? MonarchAiEditProposalStrings.remaining(left) : MonarchAiEditProposalStrings.noneLeft
            return "\(done) \(rest)"
        }
    }

    /// The name of a proposal state.
    static func stateName(_ state: MonarchAiEditProposalState, labels: MonarchAiEditProposalLabels) -> String {
        labels.state?(state) ?? MonarchAiEditProposalStrings.state(state)
    }

    /// The name of a decision.
    static func hunkStateName(_ status: MonarchAiEditHunkStatus, labels: MonarchAiEditProposalLabels) -> String {
        labels.hunkState?(status) ?? MonarchAiEditProposalStrings.hunkState(status)
    }

    /// The line under the title: the count while proposed, else the read-only note.
    static func summary(
        state: MonarchAiEditProposalState,
        total: Int,
        pending: Int,
        labels: MonarchAiEditProposalLabels
    ) -> String {
        guard state != .proposed else { return countText(total: total, pending: pending, labels: labels) }
        return labels.readOnly?(state) ?? MonarchAiEditProposalStrings.readOnly(state)
    }

    /// What VoiceOver reads for a change: its name, its decision, then the spoken diff.
    static func hunkLabel(name: String, status: String?, diff: String) -> String {
        [name, status, diff].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ". ")
    }
}
