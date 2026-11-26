/// Built-in strings of the email verification banner, from the
/// `MonarchEmailVerificationBanner` strings table.
enum MonarchEmailVerificationBannerStrings {
    private static let table = "MonarchEmailVerificationBanner"

    /// The title.
    static var title: String { MonarchStrings.string("monarch.emailVerificationBanner.title", table: table) }

    /// The message with the address.
    static func message(_ email: String) -> String {
        MonarchStrings.string("monarch.emailVerificationBanner.message", table: table, email)
    }

    /// The resend button.
    static var resend: String { MonarchStrings.string("monarch.emailVerificationBanner.resend", table: table) }

    /// The resend button during the cooldown.
    static func resendIn(_ seconds: Int) -> String {
        MonarchStrings.string("monarch.emailVerificationBanner.resendIn", table: table, seconds)
    }

    /// Shown after a send.
    static var sent: String { MonarchStrings.string("monarch.emailVerificationBanner.sent", table: table) }

    /// The change email button.
    static var changeEmail: String {
        MonarchStrings.string("monarch.emailVerificationBanner.changeEmail", table: table)
    }

    /// The dismiss button.
    static var dismiss: String { MonarchStrings.string("monarch.emailVerificationBanner.dismiss", table: table) }

    /// The fallback error.
    static var error: String { MonarchStrings.string("monarch.emailVerificationBanner.error", table: table) }
}
