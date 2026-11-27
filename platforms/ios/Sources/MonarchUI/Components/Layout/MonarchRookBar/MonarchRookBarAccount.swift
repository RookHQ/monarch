import Foundation

/// Who is signed in, for the account controls of a ``MonarchRookBar``.
public enum MonarchRookBarAccount: Sendable, Hashable {
    /// Nobody: Log in and the primary Sign up.
    case signedOut(signInURL: URL?, signUpURL: URL?)
    /// A person: an avatar menu with Open dashboard, Account settings and Sign out.
    case signedIn(name: String, dashboardURL: URL?, accountURL: URL?)
}
