import MonarchTokens
import SwiftUI

/// What pressing Done does.
enum MonarchKeyCreatedDialogDoneOutcome: Equatable {
    /// Close the sheet and call `onDone`.
    case close
    /// Stay open, show why and move VoiceOver to the checkbox.
    case nudge
}

/// The notice above the value.
struct MonarchKeyCreatedDialogNotice: Equatable {
    let title: String
    let message: String
    let tone: MonarchTone
}

/// Component tokens and pure logic for ``MonarchKeyCreatedDialog``.
enum MonarchKeyCreatedDialogTokens {
    /// The padding around the content of the sheet.
    static let padding: CGFloat = MonarchTokens.Space.space6
    /// The gap between the parts of the sheet.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space5
    /// The gap between the title and the description, and the field label and the field.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the error icon and the error text.
    static let messageIconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The SF Symbol before the "confirm first" message.
    static let errorSymbol = "exclamationmark.circle.fill"
    /// The colour of the "confirm first" message.
    static var errorRole: MonarchColorRole { \.dangerStrong }

    /// Whether the person must tick the acknowledgement: by default for a
    /// secret and not for a public value.
    static func needsAcknowledgement(kind: MonarchKeyCreatedDialogKind, requires: Bool?) -> Bool {
        requires ?? (kind == .secret)
    }

    /// Whether the sheet may close.
    static func isConfirmed(needsAcknowledgement: Bool, isAcknowledged: Bool) -> Bool {
        !needsAcknowledgement || isAcknowledged
    }

    /// What Done does. It stays enabled, so pressing it early explains what
    /// is missing instead of doing nothing.
    static func doneOutcome(isConfirmed: Bool) -> MonarchKeyCreatedDialogDoneOutcome {
        isConfirmed ? .close : .nudge
    }

    /// Whether the "confirm first" message is shown.
    static func showsNudge(isNudged: Bool, isAcknowledged: Bool) -> Bool {
        isNudged && !isAcknowledged
    }

    /// The notice for a kind: a warning for a secret, information for a public value.
    static func notice(kind: MonarchKeyCreatedDialogKind, labels: MonarchKeyCreatedDialogLabels)
        -> MonarchKeyCreatedDialogNotice
    {
        switch kind {
        case .secret:
            MonarchKeyCreatedDialogNotice(
                title: labels.resolvedSecretNoticeTitle,
                message: labels.resolvedSecretNotice,
                tone: .warning
            )
        case .public:
            MonarchKeyCreatedDialogNotice(
                title: labels.resolvedPublicNoticeTitle,
                message: labels.resolvedPublicNotice,
                tone: .info
            )
        }
    }

    /// The label shown above the value: the name the person gave it, else what it is.
    static func fieldLabel(name: String?, label: String) -> String {
        guard let name, !name.isEmpty else { return label }
        return name
    }
}
