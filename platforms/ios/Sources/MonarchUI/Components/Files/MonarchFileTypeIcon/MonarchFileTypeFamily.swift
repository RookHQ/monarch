import UniformTypeIdentifiers

/// The family of a file, which decides the glyph of ``MonarchFileTypeIcon``.
///
/// The same eleven families exist on the web and on Android.
public enum MonarchFileTypeFamily: String, Sendable, Hashable, CaseIterable {
    /// A picture: PNG, JPEG, HEIC, SVG and so on.
    case image
    /// A film or clip.
    case video
    /// A sound file.
    case audio
    /// A PDF document.
    case pdf
    /// A word-processing document.
    case document
    /// A spreadsheet, including CSV.
    case spreadsheet
    /// A slide deck.
    case presentation
    /// A compressed archive.
    case archive
    /// Source code and structured data such as JSON.
    case code
    /// Plain text.
    case text
    /// Anything else.
    case other

    /// The family of a file from its name and MIME type.
    ///
    /// A specific MIME type wins. An empty type or `application/octet-stream`
    /// falls back to the extension of the name. A type or extension that
    /// Monarch's own tables do not list is looked up as a `UTType`.
    ///
    /// - Parameters:
    ///   - fileName: The file name or path, for example `report.pdf`.
    ///   - contentType: The MIME type, for example `application/pdf`.
    public init(fileName: String?, contentType: String? = nil) {
        self = MonarchFileTypeRules.family(fileName: fileName, contentType: contentType)
    }

    /// The family a uniform type belongs to.
    public init(_ type: UTType) {
        self = MonarchFileTypeRules.family(of: type)
    }
}
