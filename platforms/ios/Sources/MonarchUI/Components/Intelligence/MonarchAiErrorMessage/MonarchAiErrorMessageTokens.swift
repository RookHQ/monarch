import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiErrorMessage``. The card takes its
/// colours, padding and shape from the banner, so it looks like one.
enum MonarchAiErrorMessageTokens {
    /// The gap between the text, the actions and the details.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between actions.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding before and after the technical details.
    static let detailsHorizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below the technical details.
    static let detailsVerticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The corner radius of the technical details.
    static let detailsCornerRadius: CGFloat = MonarchTokens.Radius.md
    /// How often the countdown is checked.
    static let tick: Duration = .milliseconds(500)

    /// An action the card can offer.
    enum Action: Hashable, CaseIterable {
        /// Ask for the reply again.
        case retry
        /// Put the failed prompt back in the input.
        case editPrompt
        /// Change the model.
        case switchModel
        /// Start a new conversation.
        case newConversation
        /// Summarize the conversation and continue from the summary.
        case summarize
    }

    /// The tone of a kind. Failures a person can wait out or rephrase are
    /// warnings; the rest are errors.
    static func tone(_ kind: MonarchAiErrorKind) -> MonarchTone {
        switch kind {
        case .rateLimit, .contextOverflow, .safetyBlock: .warning
        case .network, .toolFailure, .unknown: .danger
        }
    }

    /// The SF Symbol of a kind: its own where it has one, otherwise the symbol of its tone.
    static func symbol(_ kind: MonarchAiErrorKind) -> String {
        switch kind {
        case .rateLimit: "clock"
        case .network: "wifi.slash"
        case .toolFailure: "wrench.and.screwdriver"
        case .contextOverflow, .safetyBlock, .unknown: MonarchBannerTokens.symbol(tone: tone(kind))
        }
    }

    /// The SF Symbol of an action, or `nil` for none.
    static func symbol(_ action: Action) -> String? {
        switch action {
        case .retry: "arrow.clockwise"
        case .editPrompt: "pencil"
        case .newConversation: "plus"
        case .switchModel, .summarize: nil
        }
    }

    /// The order of the actions for a kind. The first one offered is the primary button.
    static func order(_ kind: MonarchAiErrorKind) -> [Action] {
        switch kind {
        case .rateLimit: [.retry, .switchModel, .editPrompt, .newConversation]
        case .contextOverflow: [.newConversation, .summarize, .editPrompt, .switchModel, .retry]
        case .safetyBlock: [.editPrompt, .retry, .newConversation, .switchModel]
        case .network, .toolFailure, .unknown: [.retry, .editPrompt, .switchModel, .newConversation]
        }
    }

    /// The actions to show, in order: those of the kind that have a handler.
    /// Summarize is only offered for a conversation that is too long.
    static func actions(for kind: MonarchAiErrorKind, handled: Set<Action>) -> [Action] {
        order(kind).filter { handled.contains($0) }
    }

    /// The variant of the action at a position.
    static func variant(at index: Int) -> MonarchButtonVariant {
        index == 0 ? .primary : .secondary
    }

    /// The whole seconds of a retry delay, rounded up. Zero when there is none.
    static func initialSeconds(_ retryAfter: TimeInterval?) -> Int {
        guard let retryAfter, retryAfter > 0, retryAfter.isFinite else { return 0 }
        return Int(retryAfter.rounded(.up))
    }

    /// The whole seconds left until a deadline, rounded up and never negative.
    static func secondsLeft(until deadline: Date, now: Date) -> Int {
        max(0, Int(deadline.timeIntervalSince(now).rounded(.up)))
    }

    /// What is announced when the card appears: the title, then the message.
    static func announcement(title: String, message: String) -> String {
        [title, message].filter { !$0.isEmpty }.joined(separator: " ")
    }
}
