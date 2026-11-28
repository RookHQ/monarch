import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchMemberRow``.
enum MonarchMemberRowTokens {
    /// The least width of the role picker, so the pickers of a list line up as on the web.
    static let roleMinimumWidth: CGFloat = MonarchTokens.Space.space16 + MonarchTokens.Space.space12
    /// The gap between the avatar and the text.
    static let avatarSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of text.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the name, "(You)" and the badges.
    static let nameSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the controls.
    static let controlSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the identity and the controls when they stack.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The padding above and below the row.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between a notice's icon and its text.
    static let noticeIconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The size of the avatar.
    static let avatarSize: MonarchAvatarSize = .md
    /// The symbol of the remove button.
    static let removeSymbol = "trash"
    /// The symbol of the resend swipe action.
    static let resendSymbol = "paperplane"
    /// The symbol before an error.
    static let errorSymbol = "exclamationmark.circle.fill"
    /// The symbol before a confirmation.
    static let doneSymbol = "checkmark.circle.fill"

    /// A message under the identity: a failure or a confirmation.
    enum Notice: Equatable {
        /// An action failed; shown in the danger colour with an icon.
        case error(String)
        /// An action worked; shown in the success colour with an icon.
        case done(String)

        /// Whether the notice is a failure.
        var isError: Bool {
            if case .error = self { return true }
            return false
        }

        /// The text of the notice.
        var text: String {
            switch self {
            case .error(let text), .done(let text): text
            }
        }
    }

    /// Whether the role is a picker: allowed (by default when there is a
    /// handler and it is not the signed-in person's row) and there are roles.
    static func isRoleEditable(canChangeRole: Bool?, hasHandler: Bool, isSelf: Bool, roleCount: Int) -> Bool {
        hasHandler && (canChangeRole ?? !isSelf) && roleCount > 0
    }

    /// Whether Remove is offered: allowed (by default when it is not the
    /// signed-in person's row) and there is a handler.
    static func isRemovable(canRemove: Bool?, hasHandler: Bool, isSelf: Bool) -> Bool {
        hasHandler && (canRemove ?? !isSelf)
    }

    /// Whether Resend invite is offered.
    static func isResendable(status: MonarchMemberRowStatus, hasHandler: Bool) -> Bool {
        status == .invited && hasHandler
    }

    /// The title of the role shown: the role's title, else its raw value.
    static func roleTitle(_ role: String?, roles: [MonarchRoleOption<String>]) -> String? {
        guard let role, !role.isEmpty else { return nil }
        return roles.first { $0.value == role }?.title ?? role
    }

    /// The badge of a status: none for active, info for invited, warning for suspended.
    static func badge(
        _ status: MonarchMemberRowStatus,
        tone: MonarchTone? = nil,
        labels: MonarchMemberRowLabels
    ) -> (text: String, tone: MonarchTone)? {
        switch status {
        case .active: nil
        case .invited: (labels.invited ?? MonarchMemberRowStrings.invited, tone ?? .info)
        case .suspended: (labels.suspended ?? MonarchMemberRowStrings.suspended, tone ?? .warning)
        }
    }

    /// The colour of the name: muted while suspended.
    static func nameRole(_ status: MonarchMemberRowStatus) -> MonarchColorRole {
        status == .suspended ? \.textMuted : \.textDefault
    }

    /// The colour of a notice.
    static func noticeRole(_ notice: Notice) -> MonarchColorRole {
        switch notice {
        case .error: \.dangerStrong
        case .done: \.successStrong
        }
    }

    /// The text after the name of the signed-in person: "(You)".
    static func selfMarker(_ labels: MonarchMemberRowLabels) -> String {
        "(\(labels.you ?? MonarchMemberRowStrings.you))"
    }

    /// The spoken name of the identity: the name, then "You" on the signed-in person's row.
    static func accessibilityLabel(name: String, isSelf: Bool, labels: MonarchMemberRowLabels) -> String {
        isSelf ? "\(name), \(labels.you ?? MonarchMemberRowStrings.you)" : name
    }

    /// The spoken details of the identity: status, presence, extra badges,
    /// the role when it is not a picker, email and meta, in that order.
    static func accessibilityValue(
        status: MonarchMemberRowStatus,
        presence: MonarchPresenceStatus?,
        badges: [String],
        readOnlyRole: String?,
        email: String?,
        meta: String?,
        labels: MonarchMemberRowLabels
    ) -> String {
        var parts: [String] = []
        if let badge = badge(status, labels: labels) { parts.append(badge.text) }
        if let presence {
            parts.append(labels.presence?(presence) ?? MonarchPresenceDot.label(for: presence))
        }
        parts += badges
        for text in [readOnlyRole, email, meta] {
            if let text, !text.isEmpty { parts.append(text) }
        }
        return parts.joined(separator: ", ")
    }

    /// The confirmation of a removal: title, message and confirm button, for a member or an invitation.
    static func confirmation(
        name: String,
        status: MonarchMemberRowStatus,
        consequences: [String],
        labels: MonarchMemberRowLabels
    ) -> (title: String, message: String, confirm: String) {
        let isInvite = status == .invited
        let title =
            isInvite
            ? labels.revokeTitle?(name) ?? MonarchMemberRowStrings.revokeTitle(name)
            : labels.removeTitle?(name) ?? MonarchMemberRowStrings.removeTitle(name)
        let description =
            isInvite
            ? labels.revokeDescription ?? MonarchMemberRowStrings.revokeDescription
            : labels.removeDescription ?? MonarchMemberRowStrings.removeDescription
        let confirm =
            isInvite
            ? labels.revokeConfirm ?? MonarchMemberRowStrings.revokeConfirm
            : labels.removeConfirm ?? MonarchMemberRowStrings.removeConfirm
        let message = ([description] + consequences.filter { !$0.isEmpty }).joined(separator: "\n")
        return (title, message, confirm)
    }

    /// The spoken name of the remove button.
    static func removeLabel(name: String, status: MonarchMemberRowStatus, labels: MonarchMemberRowLabels) -> String {
        status == .invited
            ? labels.revoke?(name) ?? MonarchMemberRowStrings.revoke(name)
            : labels.remove?(name) ?? MonarchMemberRowStrings.remove(name)
    }

    /// The message of a failed action: the error's own description, else the built-in one.
    static func failureMessage(_ error: any Error, labels: MonarchMemberRowLabels) -> String {
        MonarchFailureMessage.message(for: error, fallback: labels.failed ?? MonarchMemberRowStrings.failed)
    }
}
