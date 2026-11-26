/// The rules that applied where a consent decision was made.
public enum MonarchConsentRegime: String, Sendable, Hashable, CaseIterable {
    /// Consent is asked before optional storage is used.
    case optIn
    /// Optional storage runs until the person refuses.
    case optOut
    /// Only a notice is required.
    case notice
}
