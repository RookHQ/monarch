import Foundation
import MonarchTokens

/// Component tokens and logic for ``MonarchAiToolResultCard``.
enum MonarchAiToolResultCardTokens {
    /// The gap between the icon tile and the text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of text.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the text, the facts and the actions.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between actions.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The side of the icon tile before text scaling.
    static let tileSide: CGFloat = MonarchTokens.Control.sm
    /// The corner radius of the icon tile.
    static let tileCornerRadius: CGFloat = MonarchTokens.Radius.md

    /// Where the undo is.
    enum UndoState: Hashable {
        /// Undo is offered.
        case idle
        /// The undo is running.
        case pending
        /// The undo worked.
        case done
        /// The undo failed; it can be tried again.
        case failed
    }

    /// The SF Symbol of a kind.
    static func symbol(_ kind: MonarchAiToolResultKind) -> String {
        switch kind {
        case .recordCreated: "plus"
        case .emailSent: "envelope"
        case .eventBooked: "calendar"
        case .ticketFiled: "ticket"
        case .generic: "checkmark"
        }
    }

    /// The fill and the icon colour of the tile: success for a result that
    /// stands, muted once it was undone.
    static func tile(isUndone: Bool) -> (background: MonarchColorRole, foreground: MonarchColorRole) {
        let muted: MonarchColorRole = \.bgMuted
        let mutedText: MonarchColorRole = \.textMuted
        let success: MonarchColorRole = \.successSubtle
        let successText: MonarchColorRole = \.successStrong
        return isUndone ? (muted, mutedText) : (success, successText)
    }

    /// The colour of the status line.
    static func statusColor(_ state: UndoState) -> MonarchColorRole {
        state == .failed ? \.dangerStrong : \.textMuted
    }

    /// Whether Undo is shown: there is a handler and the undo has not worked yet.
    static func showsUndo(hasHandler: Bool, state: UndoState) -> Bool {
        hasHandler && state != .done
    }

    /// Whether Open is shown: there is a safe address and the result was not undone.
    static func showsOpen(url: URL?, state: UndoState) -> Bool {
        url != nil && state != .done
    }

    /// Whether activating Undo starts an undo: not while one runs and not after one worked.
    static func canStartUndo(_ state: UndoState) -> Bool {
        state == .idle || state == .failed
    }

    /// The address of the result when it is safe to open: `http` or `https`
    /// with a host, or `mailto`. Anything else, such as `javascript:`, is dropped.
    static func safeURL(_ url: URL?) -> URL? {
        url.flatMap(MonarchSafeURL.url)
    }

    /// What VoiceOver reads for the text of the card: the kind, the title and the description.
    static func accessibilityLabel(kind: String, title: String, description: String?) -> String {
        [kind, title, description ?? ""].filter { !$0.isEmpty }.joined(separator: ", ")
    }
}
