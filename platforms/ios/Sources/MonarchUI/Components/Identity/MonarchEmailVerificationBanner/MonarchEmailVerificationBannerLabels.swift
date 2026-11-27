/// Replacements for the built-in strings of ``MonarchEmailVerificationBanner``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchEmailVerificationBannerLabels: Sendable {
    /// The title. Built in: "Verify your email address".
    public var title: String?
    /// The message. Receives the address. Built in: "We sent a verification
    /// link to <email>. Open it to confirm the address belongs to you."
    public var message: (@Sendable (String) -> String)?
    /// The resend button. Built in: "Resend email".
    public var resend: String?
    /// The resend button during the cooldown. Receives the seconds left.
    /// Built in: "Resend in 42s".
    public var resendIn: (@Sendable (Int) -> String)?
    /// Shown and announced after a send. Built in: "Verification email sent".
    public var sent: String?
    /// The change email button. Built in: "Change email".
    public var changeEmail: String?
    /// The spoken name of the dismiss button. Built in: "Dismiss".
    public var dismiss: String?
    /// Shown when a send fails with an error that has no description.
    /// Built in: "Couldn't send the email. Try again."
    public var error: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        message: (@Sendable (String) -> String)? = nil,
        resend: String? = nil,
        resendIn: (@Sendable (Int) -> String)? = nil,
        sent: String? = nil,
        changeEmail: String? = nil,
        dismiss: String? = nil,
        error: String? = nil
    ) {
        self.title = title
        self.message = message
        self.resend = resend
        self.resendIn = resendIn
        self.sent = sent
        self.changeEmail = changeEmail
        self.dismiss = dismiss
        self.error = error
    }
}
