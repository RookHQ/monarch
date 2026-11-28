import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiMessageActions``.
enum MonarchAiMessageActionsTokens {
    /// The gap between the buttons.
    static let spacing: CGFloat = MonarchTokens.Space.space0_5
    /// The size of every button.
    static let buttonSize: MonarchButtonSize = .sm
    /// The variant of every button.
    static let buttonVariant: MonarchButtonVariant = .ghost

    /// One action of the row, in display order.
    enum Action: CaseIterable, Hashable {
        case copy, regenerate, edit, good, bad, share, readAloud
    }

    /// The actions that are offered: each only when it is enabled by its option or callback.
    static func actions(
        isCopyable: Bool,
        hasRegenerate: Bool,
        hasEdit: Bool,
        hasFeedback: Bool,
        isShareable: Bool,
        hasReadAloud: Bool
    ) -> [Action] {
        Action.allCases.filter { action in
            switch action {
            case .copy: isCopyable
            case .regenerate: hasRegenerate
            case .edit: hasEdit
            case .good, .bad: hasFeedback
            case .share: isShareable
            case .readAloud: hasReadAloud
            }
        }
    }

    /// The actions that stay in the row when the full row does not fit: copy
    /// and the thumbs. The rest move into the More menu.
    static func compactRow(_ actions: [Action]) -> [Action] {
        actions.filter { [.copy, .good, .bad].contains($0) }
    }

    /// The actions that move into the More menu when the full row does not fit.
    static func compactMenu(_ actions: [Action]) -> [Action] {
        actions.filter { ![.copy, .good, .bad].contains($0) }
    }

    /// Whether a More menu is shown: when it holds actions or extra content.
    static func showsMenu(menuActions: [Action], hasMore: Bool) -> Bool {
        hasMore || !menuActions.isEmpty
    }

    /// The SF Symbol of an action. A pressed thumb is filled, so the state is a shape as well as a colour.
    static func symbol(_ action: Action, feedback: MonarchAiMessageFeedback?, isReading: Bool) -> String {
        switch action {
        case .copy: "doc.on.doc"
        case .regenerate: "arrow.clockwise"
        case .edit: "pencil"
        case .good: MonarchAiFeedbackTokens.symbol(.up, isPressed: feedback == .up)
        case .bad: MonarchAiFeedbackTokens.symbol(.down, isPressed: feedback == .down)
        case .share: "square.and.arrow.up"
        case .readAloud: isReading ? "stop.circle" : "speaker.wave.2"
        }
    }

    /// The SF Symbol of the More menu.
    static let moreSymbol = "ellipsis"

    /// The title of an action, which is also its spoken name.
    static func title(_ action: Action, isReading: Bool, labels: MonarchAiMessageActionsLabels) -> String {
        switch action {
        case .copy: labels.copy ?? MonarchAiMessageActionsStrings.copy
        case .regenerate: labels.regenerate ?? MonarchAiMessageActionsStrings.regenerate
        case .edit: labels.edit ?? MonarchAiMessageActionsStrings.edit
        case .good: labels.good ?? MonarchAiMessageActionsStrings.good
        case .bad: labels.bad ?? MonarchAiMessageActionsStrings.bad
        case .share: labels.share ?? MonarchAiMessageActionsStrings.share
        case .readAloud:
            isReading
                ? labels.stopReading ?? MonarchAiMessageActionsStrings.stopReading
                : labels.readAloud ?? MonarchAiMessageActionsStrings.readAloud
        }
    }

    /// The rating after pressing a thumb: pressing the pressed thumb clears it.
    static func rating(after action: Action, current: MonarchAiMessageFeedback?) -> MonarchAiMessageFeedback? {
        switch action {
        case .good: MonarchAiFeedbackTokens.rating(after: .up, current: current)
        case .bad: MonarchAiFeedbackTokens.rating(after: .down, current: current)
        default: current
        }
    }
}
