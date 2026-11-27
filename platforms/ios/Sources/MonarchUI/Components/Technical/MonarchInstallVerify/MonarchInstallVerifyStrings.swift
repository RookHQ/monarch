/// Built-in strings of ``MonarchInstallVerify``, from the `MonarchInstallVerify` strings table.
enum MonarchInstallVerifyStrings {
    private static let table = "MonarchInstallVerify"

    /// The string for a key without placeholders, such as `installHeading`.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.installVerify.\(key)", table: table)
    }

    /// The string for a key with a text placeholder, such as "Connected on example.com".
    static func string(_ key: String, _ text: String) -> String {
        MonarchStrings.string("monarch.installVerify.\(key)", table: table, text)
    }
}
