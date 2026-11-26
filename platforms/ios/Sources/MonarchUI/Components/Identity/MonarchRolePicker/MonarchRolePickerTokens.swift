import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchRolePicker``.
enum MonarchRolePickerTokens {
    /// The gap between the label, the control and the messages.
    static let spacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the error icon and the error text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The SF Symbol before an error.
    static let errorSymbol = "exclamationmark.circle"

    /// The text under the title of a role card: the description, then each
    /// capability on its own line after a bullet, as the web lists them, then the reason a disabled role cannot be chosen.
    static func cardDescription(
        description: String?,
        capabilities: [String],
        isDisabled: Bool,
        disabledReason: String?
    ) -> String? {
        var lines: [String] = []
        if let description, !description.isEmpty { lines.append(description) }
        lines.append(contentsOf: capabilities.filter { !$0.isEmpty }.map { "\u{2022}  " + $0 })
        if isDisabled, let disabledReason, !disabledReason.isEmpty { lines.append(disabledReason) }
        return lines.isEmpty ? nil : lines.joined(separator: "\n")
    }

    /// The radio cards of the roles.
    static func cardOptions<Value: Hashable>(_ roles: [MonarchRoleOption<Value>]) -> [MonarchRadioCardOption<Value>] {
        roles.map { role in
            MonarchRadioCardOption(
                role.value,
                title: role.title,
                description: cardDescription(
                    description: role.description,
                    capabilities: role.capabilities,
                    isDisabled: role.isDisabled,
                    disabledReason: role.disabledReason
                ),
                isDisabled: role.isDisabled
            )
        }
    }

    /// The text under a compact picker: why the chosen role is disabled, else its description.
    static func compactDescription<Value: Hashable>(_ role: MonarchRoleOption<Value>?) -> String? {
        guard let role else { return nil }
        if role.isDisabled, let reason = role.disabledReason, !reason.isEmpty { return reason }
        return role.description
    }

    /// The role with the selected value.
    static func selectedRole<Value: Hashable>(
        _ roles: [MonarchRoleOption<Value>],
        selection: Value?
    ) -> MonarchRoleOption<Value>? {
        guard let selection else { return nil }
        return roles.first { $0.value == selection }
    }

    /// The message under the control: an error replaces the helper text.
    static func message(helper: String?, error: String?) -> (text: String, isError: Bool)? {
        if let error, !error.isEmpty { return (error, true) }
        if let helper, !helper.isEmpty { return (helper, false) }
        return nil
    }
}
