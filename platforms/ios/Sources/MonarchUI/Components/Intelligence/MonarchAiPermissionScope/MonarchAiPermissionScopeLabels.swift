/// Replacements for the built-in strings of ``MonarchAiPermissionScope``.
/// Every value is optional; leave one out to keep the built-in English text.
public struct MonarchAiPermissionScopeLabels: Sendable {
    /// The spoken name of the list.
    public var label: String
    /// The full description of a level in the read-only list: "No access",
    /// "Can read", "Can read and edit".
    public var access: @Sendable (MonarchAiAccessLevel) -> String
    /// The short name of a level on the editable control: "None", "Read", "Write".
    public var option: @Sendable (MonarchAiAccessLevel) -> String
    /// The spoken name of a row's control, such as "Access to Calendar".
    public var control: @Sendable (_ app: String) -> String
    /// The title of the manage button.
    public var manage: String
    /// Shown when there are no apps.
    public var empty: String

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - label: Replaces "What the AI can access".
    ///   - access: Replaces the descriptions of the levels.
    ///   - option: Replaces the short names on the control.
    ///   - control: Replaces "Access to <app>".
    ///   - manage: Replaces "Manage permissions".
    ///   - empty: Replaces "The AI has no access to any app."
    public init(
        label: String? = nil,
        access: (@Sendable (MonarchAiAccessLevel) -> String)? = nil,
        option: (@Sendable (MonarchAiAccessLevel) -> String)? = nil,
        control: (@Sendable (_ app: String) -> String)? = nil,
        manage: String? = nil,
        empty: String? = nil
    ) {
        self.label = label ?? MonarchAiPermissionScopeStrings.label
        self.access = access ?? { MonarchAiPermissionScopeStrings.access($0) }
        self.option = option ?? { MonarchAiPermissionScopeStrings.option($0) }
        self.control = control ?? { MonarchAiPermissionScopeStrings.control(app: $0) }
        self.manage = manage ?? MonarchAiPermissionScopeStrings.manage
        self.empty = empty ?? MonarchAiPermissionScopeStrings.empty
    }
}
