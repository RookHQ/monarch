/// Built-in strings of the lightbox, from the `MonarchLightbox` strings
/// table.
enum MonarchLightboxStrings {
    private static let table = "MonarchLightbox"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.lightbox." + key, table: table)
    }

    /// The spoken name of the viewer.
    static var title: String { string("title") }
    /// The spoken name of the button that shows the item before.
    static var previous: String { string("previous") }
    /// The spoken name of the button that shows the item after.
    static var next: String { string("next") }
    /// The spoken name of the zoom-in button.
    static var zoomIn: String { string("zoomIn") }
    /// The spoken name of the zoom-out button.
    static var zoomOut: String { string("zoomOut") }
    /// The spoken name of the button that resets the zoom.
    static var resetZoom: String { string("resetZoom") }
    /// The spoken name of the close button.
    static var close: String { string("close") }
    /// The spoken name of the share button.
    static var share: String { string("share") }
    /// Shown instead of media that cannot be shown.
    static var unavailable: String { string("unavailable") }
    /// The spoken name of the progress indicator while an image loads.
    static var loading: String { string("loading") }

    /// Where the item sits: its position, starting at 1, and the number of
    /// items.
    static func position(_ position: Int, _ count: Int) -> String {
        MonarchStrings.string("monarch.lightbox.position", table: table, position, count)
    }
}
