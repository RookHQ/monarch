import Foundation

/// What the upload closure of ``MonarchMediaUpload`` returns: where the
/// stored file is, and warnings to show.
public struct MonarchMediaUploadResult: Hashable, Sendable {
    /// The address of the stored file. It becomes the field's value.
    public var url: URL
    /// Warnings to show under the preview, such as "The video was shortened
    /// to 90 seconds.", already localised.
    public var warnings: [String]

    /// An upload result.
    ///
    /// - Parameters:
    ///   - url: The address of the stored file.
    ///   - warnings: Warnings to show, already localised.
    public init(url: URL, warnings: [String] = []) {
        self.url = url
        self.warnings = warnings
    }
}
