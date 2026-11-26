/// Replacements for the built-in strings of ``MonarchAiSourcePreview``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiSourcePreviewLabels: Sendable {
    /// The caption of the quoted passage when it is not found in the content,
    /// and the spoken value of the paragraph that holds it. Built in: "Quoted passage".
    public var quoted: String?
    /// The button that opens the original. Built in: "Open original".
    public var openOriginal: String?
    /// Shown when there is neither content nor a quote. Built in: "No preview is available for this source."
    public var noPreview: String?
    /// The button that closes the preview. Built in: "Close".
    public var close: String?
    /// The name of a kind of source in the meta line. Built in: "Web page", "Document", "Record", "File".
    public var kind: (@Sendable (MonarchAiSourceKind) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        quoted: String? = nil,
        openOriginal: String? = nil,
        noPreview: String? = nil,
        close: String? = nil,
        kind: (@Sendable (MonarchAiSourceKind) -> String)? = nil
    ) {
        self.quoted = quoted
        self.openOriginal = openOriginal
        self.noPreview = noPreview
        self.close = close
        self.kind = kind
    }
}
