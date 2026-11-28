/// Built-in strings of the JSON viewer, from the `MonarchJsonViewer` strings table.
enum MonarchJsonViewerStrings {
    private static let table = "MonarchJsonViewer"

    /// A string without arguments.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.jsonViewer.\(key)", table: table)
    }

    /// A string with arguments.
    static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }
}
