/// Replacements for the built-in strings of ``MonarchTermsAcceptance``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchTermsAcceptanceLabels: Sendable {
    /// Builds the checkbox label from the joined document titles. Built in: "I agree to the Terms of
    /// Service and Privacy Policy".
    public var label: (@Sendable (String) -> String)?
    /// The error shown while the box is required and not ticked. Built in: "Accept the terms to continue".
    public var requiredMessage: String?
    /// The hint VoiceOver reads on a required box. Built in: "Required".
    public var requiredHint: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        label: (@Sendable (String) -> String)? = nil,
        requiredMessage: String? = nil,
        requiredHint: String? = nil
    ) {
        self.label = label
        self.requiredMessage = requiredMessage
        self.requiredHint = requiredHint
    }
}
