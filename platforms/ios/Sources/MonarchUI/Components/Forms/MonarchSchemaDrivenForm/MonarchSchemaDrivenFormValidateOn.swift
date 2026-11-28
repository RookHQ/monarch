/// When a ``MonarchSchemaDrivenForm`` first shows a question's own error.
public enum MonarchSchemaDrivenFormValidateOn: Hashable, Sendable, CaseIterable {
    /// A typed question's error shows once the person leaves it (focus moves away or another question is
    /// answered); a choice's error shows once it is answered. Every error shows after a submit attempt.
    case blur
    /// No error shows until the person tries to submit.
    case submit
}
