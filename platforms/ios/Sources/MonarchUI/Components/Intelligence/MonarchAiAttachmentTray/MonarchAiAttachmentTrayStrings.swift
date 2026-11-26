/// Built-in strings of the attachment tray, from the `MonarchAiAttachmentTray` strings table.
enum MonarchAiAttachmentTrayStrings {
    private static let table = "MonarchAiAttachmentTray"

    /// The spoken name of the tray.
    static var label: String { MonarchStrings.string("monarch.aiAttachmentTray.label", table: table) }

    /// Beside the percentage of a file that is uploading.
    static var uploading: String { MonarchStrings.string("monarch.aiAttachmentTray.uploading", table: table) }

    /// The visible word of the button that retries a failed upload.
    static var retry: String { MonarchStrings.string("monarch.aiAttachmentTray.retry", table: table) }

    /// Announced when an upload finishes.
    static func ready(_ name: String) -> String {
        MonarchStrings.string("monarch.aiAttachmentTray.ready", table: table, name)
    }

    /// Announced after a file is removed.
    static func removed(_ name: String) -> String {
        MonarchStrings.string("monarch.aiAttachmentTray.removed", table: table, name)
    }
}
