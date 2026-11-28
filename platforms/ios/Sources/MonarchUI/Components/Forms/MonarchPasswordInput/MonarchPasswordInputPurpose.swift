/// What a password field is for, which tells the system what to offer.
public enum MonarchPasswordInputPurpose: Sendable, Hashable, CaseIterable {
    /// An existing password: sign-in, or confirming an action. Saved passwords are offered.
    case current
    /// A new password: sign-up or reset. The system offers to create and save a strong one.
    case new
}
