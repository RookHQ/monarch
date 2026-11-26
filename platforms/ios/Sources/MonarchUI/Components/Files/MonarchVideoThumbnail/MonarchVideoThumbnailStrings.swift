/// Built-in strings of the video thumbnail, from the `MonarchVideoThumbnail` strings table.
enum MonarchVideoThumbnailStrings {
    /// The name of a video that has no title.
    static var fallbackTitle: String {
        MonarchStrings.string("monarch.videoThumbnail.fallbackTitle", table: "MonarchVideoThumbnail")
    }

    /// The spoken name of the thumbnail when it is a button.
    static func play(_ name: String) -> String {
        MonarchStrings.string("monarch.videoThumbnail.play", table: "MonarchVideoThumbnail", name)
    }
}
