import Foundation
import MonarchTokens

/// Component tokens and logic for ``MonarchAiUndoBanner``.
enum MonarchAiUndoBannerTokens {
    /// Padding before and after the content.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// Padding above and below the content.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the icon and the text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title, the description, the changes and the actions.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the changes.
    static let changeSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between actions.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The corner radius.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.lg
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// More changes than this collapse into a disclosure.
    static let visibleChanges = 3

    /// Where the banner is in its life.
    enum State: Hashable {
        /// Undo is offered.
        case active
        /// The undo is running.
        case undoing
        /// The undo worked.
        case undone
        /// The undo failed; it can be tried again.
        case failed
        /// The time to undo ran out.
        case expired
    }

    /// The progress of the undo itself.
    enum Undo: Hashable {
        /// Not started.
        case idle
        /// Running.
        case undoing
        /// Worked.
        case undone
        /// Failed.
        case failed
    }

    /// The state from the undo's progress and whether the time ran out. A
    /// finished undo wins over the timer.
    static func state(undo: Undo, isExpired: Bool) -> State {
        switch undo {
        case .undone: .undone
        case _ where isExpired: .expired
        case .idle: .active
        case .undoing: .undoing
        case .failed: .failed
        }
    }

    /// Whether Undo is shown.
    static func showsUndo(_ state: State) -> Bool {
        state == .active || state == .undoing || state == .failed
    }

    /// Whether activating Undo starts an undo.
    static func canStartUndo(_ state: State) -> Bool {
        state == .active || state == .failed
    }

    /// Whether the time limit counts down: there is one, Undo can still be
    /// used, the app is in front, the person is not reading the list of
    /// changes, and neither VoiceOver nor Switch Control is running (they
    /// get no time limit).
    static func isCountingDown(
        hasDuration: Bool,
        state: State,
        isSceneActive: Bool,
        isReadingChanges: Bool,
        isAssistiveTechnologyRunning: Bool
    ) -> Bool {
        hasDuration && (state == .active || state == .failed) && isSceneActive && !isReadingChanges
            && !isAssistiveTechnologyRunning
    }

    /// The fill and border: the AI roles while the change can be undone,
    /// neutral once it was undone or the time ran out.
    static func surface(_ state: State) -> (background: MonarchColorRole, border: MonarchColorRole) {
        let neutral: (background: MonarchColorRole, border: MonarchColorRole) = (\.bgSubtle, \.borderSubtle)
        let ai: (background: MonarchColorRole, border: MonarchColorRole) = (\.aiSubtle, \.aiBorder)
        switch state {
        case .undone, .expired: return neutral
        case .active, .undoing, .failed: return ai
        }
    }

    /// The colour of the status line.
    static func statusColor(_ state: State) -> MonarchColorRole {
        state == .failed ? \.dangerStrong : \.textMuted
    }

    /// Whether the changes are behind a disclosure: more than three.
    static func collapsesChanges(_ count: Int) -> Bool {
        count > visibleChanges
    }

    /// The link to the audit log when it is safe to open: `http` or `https`
    /// with a host. Anything else is dropped.
    static func safeURL(_ url: URL?) -> URL? {
        guard let url, let scheme = url.scheme?.lowercased(), scheme == "http" || scheme == "https",
            url.host?.isEmpty == false
        else { return nil }
        return url
    }

    /// What VoiceOver reads for the title: the AI prefix, then the title.
    static func titleLabel(prefix: String, title: String) -> String {
        [prefix, title].filter { !$0.isEmpty }.joined(separator: " ")
    }
}
