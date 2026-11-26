/// The answer of the check of a ``MonarchUsernameAvailabilityInput``.
public struct MonarchUsernameAvailabilityResult: Sendable, Hashable {
    /// Whether the value can be used, for example the handle is free.
    public let isValid: Bool
    /// A message, already localised, shown under the field and announced.
    /// Without it the component's own text for the status is used.
    public let message: String?

    /// An answer.
    ///
    /// - Parameters:
    ///   - isValid: Whether the value can be used.
    ///   - message: A message, already localised, shown under the field and announced.
    public init(isValid: Bool, message: String? = nil) {
        self.isValid = isValid
        self.message = message
    }
}
