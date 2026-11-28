/// Built-in strings of the Rook bar, from the `MonarchRookBar` strings table.
enum MonarchRookBarStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.rookBar.\(key)", table: "MonarchRookBar")
    }

    /// The spoken name of the bar.
    static var navigation: String { string("navigation") }
    /// The menu button.
    static var openMenu: String { string("openMenu") }
    /// The close button of the menu.
    static var closeMenu: String { string("closeMenu") }
    /// The avatar button.
    static var accountMenu: String { string("accountMenu") }
    /// The dashboard link.
    static var dashboard: String { string("dashboard") }
    /// The account settings link.
    static var account: String { string("account") }
    /// The sign-out button.
    static var signOut: String { string("signOut") }
    /// The sign-in link.
    static var signIn: String { string("signIn") }
    /// The sign-up link.
    static var signUp: String { string("signUp") }
}
