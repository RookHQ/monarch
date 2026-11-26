/// Built-in strings of the impersonation banner, from the
/// `MonarchImpersonationBanner` strings table.
enum MonarchImpersonationBannerStrings {
    private static let table = "MonarchImpersonationBanner"

    /// The format of the main line, with `%@` for the name.
    static var viewingAsFormat: String {
        MonarchStrings.string("monarch.impersonationBanner.viewingAs", table: table)
    }

    /// The main line.
    static func viewingAs(_ name: String) -> String {
        MonarchStrings.string("monarch.impersonationBanner.viewingAs", table: table, name)
    }

    /// Before the start time.
    static var started: String { MonarchStrings.string("monarch.impersonationBanner.started", table: table) }

    /// Before the reason.
    static var reason: String { MonarchStrings.string("monarch.impersonationBanner.reason", table: table) }

    /// The stop button.
    static var stop: String { MonarchStrings.string("monarch.impersonationBanner.stop", table: table) }

    /// The spoken name of the stop button.
    static func stopLabel(_ name: String) -> String {
        MonarchStrings.string("monarch.impersonationBanner.stopLabel", table: table, name)
    }

    /// The fallback error.
    static var stopError: String { MonarchStrings.string("monarch.impersonationBanner.stopError", table: table) }
}
