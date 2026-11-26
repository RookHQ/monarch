/// How a ``MonarchFileUpload`` is drawn.
public enum MonarchFileUploadVariant: Sendable, Hashable, CaseIterable {
    /// A bordered area with the icon, the text and the buttons stacked. The default.
    case zone
    /// One row, for tight spaces such as a form. It stacks at the accessibility text sizes.
    case compact
}
