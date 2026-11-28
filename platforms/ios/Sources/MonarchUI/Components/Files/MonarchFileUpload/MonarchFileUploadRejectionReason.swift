/// Why a ``MonarchFileUpload`` refused a file.
public enum MonarchFileUploadRejectionReason: Sendable, Hashable, CaseIterable {
    /// The type is not one of the allowed content types.
    case invalidType
    /// The file is larger than the maximum size.
    case tooLarge
    /// Adding it would go over the maximum number of files.
    case tooMany
    /// The app's own check refused it.
    case custom
}
