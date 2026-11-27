/// Replacements for the built-in strings of ``MonarchAiArtifactCanvas``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiArtifactCanvasLabels: Sendable {
    /// The spoken name of the canvas, from the title. Built in: "Artifact: Q3 summary".
    public var label: (@Sendable (String) -> String)?
    /// The visible name of a kind. Built in: "Document", "Web page"…
    public var kind: (@Sendable (MonarchAiArtifactKind) -> String)?
    /// The visible version. Built in: "v3".
    public var version: (@Sendable (Int) -> String)?
    /// The Preview segment. Built in: "Preview".
    public var preview: String?
    /// The Code segment. Built in: "Code".
    public var code: String?
    /// The spoken name of the segmented control. Built in: "View".
    public var views: String?
    /// The close button. Built in: "Close artifact".
    public var close: String?
    /// The copy button. Built in: "Copy".
    public var copy: String?
    /// The copy button after copying. Built in: "Copied".
    public var copied: String?
    /// The share button. Built in: "Share".
    public var share: String?
    /// The save button, when the app gives no `saveTitle`. Built in: "Save".
    public var save: String?
    /// The spoken name of the web preview, from the title. Built in: "Preview of Q3 summary".
    public var frameTitle: (@Sendable (String) -> String)?
    /// Shown under the web preview. Built in: "Sandboxed preview: scripts, forms and network requests are blocked."
    public var sandboxNote: String?
    /// Shown when an image address is not allowed. Built in: "This image address is not allowed, so it is not shown."
    public var unsafeImage: String?
    /// Shown when the kind has no preview. Built in: "No preview for this kind of artifact. See the Code tab."
    public var noPreview: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: (@Sendable (String) -> String)? = nil,
        kind: (@Sendable (MonarchAiArtifactKind) -> String)? = nil,
        version: (@Sendable (Int) -> String)? = nil,
        preview: String? = nil,
        code: String? = nil,
        views: String? = nil,
        close: String? = nil,
        copy: String? = nil,
        copied: String? = nil,
        share: String? = nil,
        save: String? = nil,
        frameTitle: (@Sendable (String) -> String)? = nil,
        sandboxNote: String? = nil,
        unsafeImage: String? = nil,
        noPreview: String? = nil
    ) {
        self.label = label
        self.kind = kind
        self.version = version
        self.preview = preview
        self.code = code
        self.views = views
        self.close = close
        self.copy = copy
        self.copied = copied
        self.share = share
        self.save = save
        self.frameTitle = frameTitle
        self.sandboxNote = sandboxNote
        self.unsafeImage = unsafeImage
        self.noPreview = noPreview
    }
}
