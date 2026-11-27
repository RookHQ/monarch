import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiFeedback``.
enum MonarchAiFeedbackTokens {
    /// Where the feedback is.
    enum Phase: Equatable {
        /// No form: the thumbs, maybe one pressed.
        case idle
        /// The sheet with reasons and the comment is open.
        case details
        /// `onSubmit` is running.
        case pending
        /// Sent: the thank-you is shown.
        case thanks
    }

    /// What happens right after a thumb is pressed.
    enum Next: Equatable {
        /// Nothing more: the rating was cleared.
        case clear
        /// Open the sheet for reasons and a comment.
        case askDetails
        /// Send the rating at once, without details.
        case send
    }

    /// The gap between the thumbs.
    static let thumbSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the thumbs and the message below them.
    static let spacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the parts of the sheet.
    static let formSpacing: CGFloat = MonarchTokens.Space.space4
    /// Padding around the sheet's content.
    static let formPadding: CGFloat = MonarchTokens.Space.space4
    /// The gap between an icon and its message.
    static let messageIconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The lines the comment shows before it scrolls.
    static let commentLines = 2...5
    /// The size of the thumbs.
    static let thumbSize: MonarchButtonSize = .sm
    /// The SF Symbol of the error message.
    static let errorSymbol = "exclamationmark.circle.fill"
    /// The SF Symbol of the thank-you.
    static let thanksSymbol = "checkmark.circle"
    /// The ids of the default reasons after a thumbs down, in order.
    static let defaultDownReasonIDs = ["inaccurate", "not-helpful", "incomplete", "hard-to-follow", "unsafe"]

    /// The SF Symbol of a thumb; filled while pressed, so the state is a shape as well as a colour.
    static func symbol(_ value: MonarchAiMessageFeedback, isPressed: Bool) -> String {
        let base = value == .up ? "hand.thumbsup" : "hand.thumbsdown"
        return isPressed ? base + ".fill" : base
    }

    /// The rating after pressing a thumb: pressing the pressed thumb clears it.
    static func rating(after pressed: MonarchAiMessageFeedback, current: MonarchAiMessageFeedback?)
        -> MonarchAiMessageFeedback?
    {
        current == pressed ? nil : pressed
    }

    /// Whether a rating opens the sheet.
    static func wantsDetails(_ value: MonarchAiMessageFeedback, asks: MonarchAiFeedbackDetails) -> Bool {
        switch asks {
        case .both: true
        case .down: value == .down
        case .none: false
        }
    }

    /// What follows a new rating.
    static func next(for rating: MonarchAiMessageFeedback?, asks: MonarchAiFeedbackDetails) -> Next {
        guard let rating else { return .clear }
        return wantsDetails(rating, asks: asks) ? .askDetails : .send
    }

    /// The built-in reasons: five after a thumbs down, none after a thumbs up.
    static func defaultReasons(for value: MonarchAiMessageFeedback) -> [MonarchCheckboxGroupOption] {
        guard value == .down else { return [] }
        return defaultDownReasonIDs.map {
            MonarchCheckboxGroupOption(id: $0, label: MonarchAiFeedbackStrings.reason($0))
        }
    }

    /// The reasons offered for a rating: the app's, else the built-in ones.
    static func options(
        for value: MonarchAiMessageFeedback,
        reasons: [MonarchAiMessageFeedback: [MonarchCheckboxGroupOption]]?
    ) -> [MonarchCheckboxGroupOption] {
        guard let reasons else { return defaultReasons(for: value) }
        return reasons[value] ?? []
    }

    /// The submission: the chosen reasons in listed order and the trimmed comment.
    static func submission(
        value: MonarchAiMessageFeedback,
        chosen: Set<String>,
        options: [MonarchCheckboxGroupOption],
        comment: String
    ) -> MonarchAiFeedbackSubmission {
        MonarchAiFeedbackSubmission(
            value: value,
            reasons: options.map(\.id).filter(chosen.contains),
            comment: comment.trimmingCharacters(in: .whitespacesAndNewlines)
        )
    }

    /// Whether the sheet is shown: while details are asked for, and while
    /// they are being sent from it. A rating sent at once never opens it.
    static func showsSheet(_ phase: Phase, fromForm: Bool) -> Bool {
        phase == .details || (phase == .pending && fromForm)
    }

    /// The phase after sending fails: back to the sheet when sent from it, else to the thumbs.
    static func phaseAfterFailure(fromForm: Bool) -> Phase {
        fromForm ? .details : .idle
    }

    /// The question above the reasons.
    static func question(_ value: MonarchAiMessageFeedback, labels: MonarchAiFeedbackLabels) -> String {
        value == .up
            ? labels.reasonsUp ?? MonarchAiFeedbackStrings.reasonsUp
            : labels.reasonsDown ?? MonarchAiFeedbackStrings.reasonsDown
    }

    /// The spoken name of a thumb.
    static func thumbLabel(_ value: MonarchAiMessageFeedback, labels: MonarchAiFeedbackLabels) -> String {
        value == .up ? labels.good ?? MonarchAiFeedbackStrings.good : labels.bad ?? MonarchAiFeedbackStrings.bad
    }
}
