import SwiftUI

/// Chooses a role for a person, with what each role can do in plain words.
///
/// ```swift
/// MonarchRolePicker(
///     selection: $role,
///     roles: [
///         MonarchRoleOption("viewer", title: "Viewer", description: "Can see everything, change nothing."),
///         MonarchRoleOption("editor", title: "Editor", description: "Can edit content.", capabilities: ["Invite guests"]),
///         MonarchRoleOption(
///             "owner",
///             title: "Owner",
///             isDisabled: true,
///             disabledReason: "Only owners can make someone an owner."
///         ),
///     ]
/// )
/// ```
///
/// The `cards` layout shows every role as a ``MonarchRadioCardGroup`` card;
/// the `compact` layout is a standard menu `Picker` with the chosen role's
/// description under it. Roles that cannot be chosen stay visible with the
/// reason. Give roles in order from least to most access.
///
/// Accessibility: in the cards layout each role is a button named by its
/// title, with the description, capabilities and reason as its value and the
/// selected trait when chosen. The compact layout keeps the standard picker.
/// An error is shown with an icon and text, not colour alone. Every target is
/// at least 44 points.
public struct MonarchRolePicker<Value: Hashable>: View {
    private let label: String?
    @Binding private var selection: Value?
    private let roles: [MonarchRoleOption<Value>]
    private let helper: String?
    private let error: String?
    private let layout: MonarchRolePickerLayout
    private let placeholder: String?
    private let isRequired: Bool
    private let showsDescription: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    /// A role picker.
    ///
    /// - Parameters:
    ///   - label: Replaces the built-in label "Role".
    ///   - selection: The value of the chosen role, or `nil` for no choice yet.
    ///   - roles: The roles, from least to most access.
    ///   - helper: Help text under the control, already localised.
    ///   - error: An error under the control, which replaces the helper text.
    ///   - layout: Cards for every role, or a compact menu picker.
    ///   - placeholder: Replaces the compact picker's built-in "Choose a role".
    public init(
        _ label: String? = nil,
        selection: Binding<Value?>,
        roles: [MonarchRoleOption<Value>],
        helper: String? = nil,
        error: String? = nil,
        layout: MonarchRolePickerLayout = .cards,
        placeholder: String? = nil
    ) {
        self.init(
            label,
            selection: selection,
            roles: roles,
            isRequired: false,
            showsDescription: true,
            helper: helper,
            error: error,
            layout: layout,
            placeholder: placeholder
        )
    }

    /// A role picker with the required mark, and the choice of showing the chosen role's description.
    ///
    /// - Parameters:
    ///   - label: Replaces the built-in label "Role".
    ///   - selection: The value of the chosen role, or `nil` for no choice yet.
    ///   - roles: The roles, from least to most access.
    ///   - isRequired: Shows a mark after the label of the compact layout and adds "Required" to its hint.
    ///   - showsDescription: Whether the compact layout shows the chosen role's description under the field.
    ///   - helper: Help text under the control, already localised.
    ///   - error: An error under the control, which replaces the helper text.
    ///   - layout: Cards for every role, or a compact menu picker.
    ///   - placeholder: Replaces the compact picker's built-in "Choose a role".
    public init(
        _ label: String? = nil,
        selection: Binding<Value?>,
        roles: [MonarchRoleOption<Value>],
        isRequired: Bool,
        showsDescription: Bool = true,
        helper: String? = nil,
        error: String? = nil,
        layout: MonarchRolePickerLayout = .cards,
        placeholder: String? = nil
    ) {
        self.label = label
        _selection = selection
        self.roles = roles
        self.helper = helper
        self.error = error
        self.layout = layout
        self.placeholder = placeholder
        self.isRequired = isRequired
        self.showsDescription = showsDescription
    }

    private var shownLabel: String { label ?? MonarchRolePickerStrings.label }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchRolePickerTokens.spacing) {
            switch layout {
            case .cards:
                MonarchRadioCardGroup(
                    shownLabel,
                    selection: $selection,
                    options: MonarchRolePickerTokens.cardOptions(roles)
                )
            case .compact:
                compact
            }
            if let message = MonarchRolePickerTokens.message(helper: helper, error: error) {
                messageView(message.text, isError: message.isError)
            }
        }
    }

    private var compact: some View {
        let selected = MonarchRolePickerTokens.selectedRole(roles, selection: selection)
        return VStack(alignment: .leading, spacing: MonarchRolePickerTokens.spacing) {
            MonarchSelectField(
                shownLabel,
                selection: $selection,
                valueText: selected?.title,
                prompt: placeholder ?? MonarchRolePickerStrings.placeholder,
                isRequired: isRequired
            ) {
                if selection == nil {
                    Text(placeholder ?? MonarchRolePickerStrings.placeholder)
                        .tag(Value?.none)
                        .selectionDisabled(true)
                }
                ForEach(roles) { role in
                    roleRow(role)
                        .tag(Value?.some(role.value))
                        .selectionDisabled(role.isDisabled)
                }
            }
            if showsDescription, let description = MonarchRolePickerTokens.compactDescription(selected),
                !description.isEmpty
            {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
    }

    /// A menu row: the title, with the description or the reason as its subtitle.
    private func roleRow(_ role: MonarchRoleOption<Value>) -> some View {
        let subtitle = role.isDisabled ? (role.disabledReason ?? role.description) : role.description
        return VStack(alignment: .leading) {
            Text(role.title)
            if let subtitle, !subtitle.isEmpty {
                Text(subtitle)
            }
        }
    }

    private func messageView(_ text: String, isError: Bool) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchRolePickerTokens.iconSpacing) {
            if isError {
                MonarchIcon(systemName: MonarchRolePickerTokens.errorSymbol, size: .sm)
            }
            Text(text).monarchTextStyle(.caption)
        }
        .foregroundStyle(isError ? theme.colors.dangerStrong : theme.colors.textMuted)
        .accessibilityElement(children: .combine)
    }
}

#if DEBUG
    #Preview("MonarchRolePicker") {
        let roles = [
            MonarchRoleOption("viewer", title: "Viewer", description: "Can see everything, change nothing."),
            MonarchRoleOption(
                "editor",
                title: "Editor",
                description: "Can edit content.",
                capabilities: ["Edit pages", "Invite guests"]
            ),
            MonarchRoleOption(
                "owner",
                title: "Owner",
                description: "Full control.",
                isDisabled: true,
                disabledReason: "Only owners can make someone an owner."
            ),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchRolePicker(selection: .constant("editor"), roles: roles, helper: "You can change this later.")
                MonarchRolePicker(selection: .constant(nil), roles: roles, error: "Choose a role.", layout: .compact)
            }
        }
    }
#endif
