/// What kind of value a ``MonarchKeyCreatedDialog`` shows.
public enum MonarchKeyCreatedDialogKind: Sendable, Hashable, CaseIterable {
    /// A secret shown once: a warning, and the sheet cannot be dismissed
    /// until the person confirms they saved it.
    case secret
    /// A value meant to be public that stays viewable later, such as a
    /// publishable key: an information notice and nothing to confirm.
    case `public`
}
