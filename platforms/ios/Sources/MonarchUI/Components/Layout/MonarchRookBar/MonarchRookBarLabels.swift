/// Replacements for the built-in strings of ``MonarchRookBar``. Each `nil` keeps the built-in text.
public struct MonarchRookBarLabels: Sendable, Hashable {
    /// The spoken name of the bar. Built-in: "Rook".
    public var navigation: String?
    /// The menu button. Built-in: "Open menu".
    public var openMenu: String?
    /// The close button of the menu. Built-in: "Close menu".
    public var closeMenu: String?
    /// The avatar button. Built-in: "Account menu".
    public var accountMenu: String?
    /// Built-in: "Open dashboard".
    public var dashboard: String?
    /// Built-in: "Account settings".
    public var account: String?
    /// Built-in: "Sign out".
    public var signOut: String?
    /// Built-in: "Log in".
    public var signIn: String?
    /// Built-in: "Sign up".
    public var signUp: String?

    /// Replacement strings.
    ///
    /// - Parameters:
    ///   - navigation: The spoken name of the bar.
    ///   - openMenu: The menu button.
    ///   - closeMenu: The close button of the menu.
    ///   - accountMenu: The avatar button.
    ///   - dashboard: The dashboard link.
    ///   - account: The account settings link.
    ///   - signOut: The sign-out button.
    ///   - signIn: The sign-in link.
    ///   - signUp: The sign-up link.
    public init(
        navigation: String? = nil,
        openMenu: String? = nil,
        closeMenu: String? = nil,
        accountMenu: String? = nil,
        dashboard: String? = nil,
        account: String? = nil,
        signOut: String? = nil,
        signIn: String? = nil,
        signUp: String? = nil
    ) {
        self.navigation = navigation
        self.openMenu = openMenu
        self.closeMenu = closeMenu
        self.accountMenu = accountMenu
        self.dashboard = dashboard
        self.account = account
        self.signOut = signOut
        self.signIn = signIn
        self.signUp = signUp
    }
}
