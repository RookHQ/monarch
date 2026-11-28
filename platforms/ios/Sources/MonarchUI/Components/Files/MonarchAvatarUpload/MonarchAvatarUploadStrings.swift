import Foundation

/// Built-in strings of ``MonarchAvatarUpload``, from the `MonarchAvatarUpload` strings table.
enum MonarchAvatarUploadStrings {
    private static let table = "MonarchAvatarUpload"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.avatarUpload." + key, table: table)
    }

    /// The picker button with no picture.
    static var upload: String { string("upload") }
    /// The picker button with a picture.
    static var change: String { string("change") }
    /// The remove button.
    static var remove: String { string("remove") }
    /// The title of the crop editor.
    static var editorTitle: String { string("editorTitle") }
    /// The spoken name of the crop area.
    static var cropArea: String { string("cropArea") }
    /// The instructions of the crop area.
    static var cropHint: String { string("cropHint") }
    /// The zoom slider.
    static var zoom: String { string("zoom") }
    /// "150%".
    static func zoomValue(_ percent: Int) -> String {
        MonarchStrings.string("monarch.avatarUpload.zoomValue", table: table, percent)
    }
    /// Move the photo left.
    static var moveLeft: String { string("moveLeft") }
    /// Move the photo right.
    static var moveRight: String { string("moveRight") }
    /// Move the photo up.
    static var moveUp: String { string("moveUp") }
    /// Move the photo down.
    static var moveDown: String { string("moveDown") }
    /// Centre the photo at no zoom.
    static var reset: String { string("reset") }
    /// The save button.
    static var save: String { string("save") }
    /// The cancel button.
    static var cancel: String { string("cancel") }
    /// Saving failed.
    static var saveFailed: String { string("saveFailed") }
    /// The file is not a picture.
    static var unreadable: String { string("unreadable") }
    /// "Choose a photo smaller than 5 MB."
    static func tooLarge(_ limit: String) -> String {
        MonarchStrings.string("monarch.avatarUpload.tooLarge", table: table, limit)
    }
}
