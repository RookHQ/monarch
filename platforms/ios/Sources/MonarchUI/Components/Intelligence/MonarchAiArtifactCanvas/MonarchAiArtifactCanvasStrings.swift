/// Built-in strings of the artifact canvas, from the `MonarchAiArtifactCanvas` strings table.
enum MonarchAiArtifactCanvasStrings {
    private static let table = "MonarchAiArtifactCanvas"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiArtifactCanvas.\(key)", table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.aiArtifactCanvas.\(key)", table: table, argument)
    }

    /// The spoken name of the canvas.
    static func label(_ title: String) -> String { string("label", title) }
    /// The Preview segment.
    static var preview: String { string("preview") }
    /// The Code segment.
    static var code: String { string("code") }
    /// The segmented control.
    static var views: String { string("views") }
    /// The close button.
    static var close: String { string("close") }
    /// The copy button.
    static var copy: String { string("copy") }
    /// The copy button after copying.
    static var copied: String { string("copied") }
    /// The share button.
    static var share: String { string("share") }
    /// The save button.
    static var save: String { string("save") }
    /// The web preview.
    static func frameTitle(_ title: String) -> String { string("frameTitle", title) }
    /// The note under the web preview.
    static var sandboxNote: String { string("sandboxNote") }
    /// A refused image address.
    static var unsafeImage: String { string("unsafeImage") }
    /// No preview for the kind.
    static var noPreview: String { string("noPreview") }
}
