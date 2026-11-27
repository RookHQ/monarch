/// A file a ``MonarchFileUpload`` refused, with the reason and a message
/// ready to show.
public struct MonarchFileUploadRejection: Hashable, Sendable {
    /// The refused file.
    public let file: MonarchFileUploadFile
    /// Why it was refused.
    public let reason: MonarchFileUploadRejectionReason
    /// A sentence that names the file and the limit, already localised.
    public let message: String

    /// A refusal.
    ///
    /// - Parameters:
    ///   - file: The refused file.
    ///   - reason: Why it was refused.
    ///   - message: A sentence that names the file and the limit.
    public init(file: MonarchFileUploadFile, reason: MonarchFileUploadRejectionReason, message: String) {
        self.file = file
        self.reason = reason
        self.message = message
    }
}
