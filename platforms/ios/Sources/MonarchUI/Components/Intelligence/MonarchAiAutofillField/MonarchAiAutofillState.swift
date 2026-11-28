/// Where a field that AI filled stands.
public enum MonarchAiAutofillState: String, Sendable, Hashable, CaseIterable {
    /// AI filled the field and nobody has checked it yet.
    case suggested
    /// The person accepted the AI value.
    case accepted
    /// The person changed the AI value.
    case edited
    /// The person removed the AI value. The status line is gone.
    case cleared
}
