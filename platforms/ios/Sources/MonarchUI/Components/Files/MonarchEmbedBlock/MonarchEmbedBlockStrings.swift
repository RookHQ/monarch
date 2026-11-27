import Foundation

/// Built-in strings of ``MonarchEmbedBlock``, from the `MonarchEmbedBlock` strings table.
enum MonarchEmbedBlockStrings {
    private static let table = "MonarchEmbedBlock"

    /// "YouTube content".
    static func frameTitle(_ provider: String) -> String {
        MonarchStrings.string("monarch.embedBlock.frameTitle", table: table, provider)
    }
    /// "Load YouTube content".
    static func load(_ provider: String) -> String {
        MonarchStrings.string("monarch.embedBlock.load", table: table, provider)
    }
    /// Shown for a URL that cannot be shown.
    static var invalid: String { MonarchStrings.string("monarch.embedBlock.invalid", table: table) }
}
