/// The editor a ``MonarchEditableCell`` opens.
public enum MonarchEditableCellKind: String, Sendable, Hashable, CaseIterable {
    /// A single-line text field.
    case text
    /// A text field with a numbers keyboard. The draft is parsed in the locale.
    case number
    /// A system menu of choices. Choosing saves.
    case select
    /// The system compact date picker. Choosing a day saves.
    case date
}
