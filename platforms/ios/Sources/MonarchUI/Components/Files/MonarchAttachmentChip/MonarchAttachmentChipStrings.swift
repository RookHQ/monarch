/// Built-in strings of the attachment chip, from the `MonarchAttachmentChip` strings table.
enum MonarchAttachmentChipStrings {
    private static let table = "MonarchAttachmentChip"

    /// "Remove report.pdf".
    static func remove(_ name: String) -> String {
        MonarchStrings.string("monarch.attachmentChip.remove", table: table, name)
    }
    /// "Retry report.pdf".
    static func retry(_ name: String) -> String {
        MonarchStrings.string("monarch.attachmentChip.retry", table: table, name)
    }
    /// "Uploading report.pdf".
    static func uploading(_ name: String) -> String {
        MonarchStrings.string("monarch.attachmentChip.uploading", table: table, name)
    }
    /// Shown while waiting to start.
    static var queued: String { MonarchStrings.string("monarch.attachmentChip.queued", table: table) }
    /// Shown when the upload failed.
    static var failed: String { MonarchStrings.string("monarch.attachmentChip.failed", table: table) }
    /// Shown when the upload was cancelled.
    static var cancelled: String { MonarchStrings.string("monarch.attachmentChip.cancelled", table: table) }
}
