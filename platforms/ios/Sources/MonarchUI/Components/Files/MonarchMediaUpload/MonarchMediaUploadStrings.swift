import Foundation

/// Built-in strings of ``MonarchMediaUpload``, from the `MonarchMediaUpload` strings table.
enum MonarchMediaUploadStrings {
    private static let table = "MonarchMediaUpload"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.mediaUpload.\(key)", table: table)
    }

    private static func format(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    /// The title of the empty media field that takes images.
    static var addImage: String { string("addImage") }
    /// The title of the empty media field that takes images and videos.
    static var addMedia: String { string("addMedia") }
    /// The hint line of a media field that takes images, without a size limit.
    static var images: String { string("images") }
    /// The hint line of a media field that takes videos, without limits.
    static var videos: String { string("videos") }
    /// Shown when the upload fails without a message of its own.
    static var uploadFailed: String { string("uploadFailed") }
    /// The button that stops an upload in progress.
    static var cancel: String { string("cancel") }
    /// Announced after an upload is cancelled.
    static var cancelled: String { string("cancelled") }
    /// Announced after an image is uploaded.
    static var imageUploaded: String { string("imageUploaded") }
    /// Announced after a video is uploaded.
    static var videoUploaded: String { string("videoUploaded") }
    /// The title of the area that replaces the uploaded image.
    static var replaceImage: String { string("replaceImage") }
    /// The title of the area that replaces the uploaded video.
    static var replaceVideo: String { string("replaceVideo") }
    /// The button that removes the uploaded image.
    static var removeImage: String { string("removeImage") }
    /// The button that removes the uploaded video.
    static var removeVideo: String { string("removeVideo") }
    /// Announced after the image is removed.
    static var imageRemoved: String { string("imageRemoved") }
    /// Announced after the video is removed.
    static var videoRemoved: String { string("videoRemoved") }
    /// The spoken name of the preview of the uploaded image.
    static var previewImage: String { string("previewImage") }
    /// The spoken name of the preview of the uploaded video.
    static var previewVideo: String { string("previewVideo") }
    /// The lead-in of the warnings shown after an upload.
    static var warnings: String { string("warnings") }
    /// The button that closes the video player.
    static var close: String { string("close") }
    /// The hint line of a media field that takes images. %@ is the largest size, such as 10 MB.
    static func imagesUpTo(_ size: String) -> String {
        format("imagesUpTo", size)
    }
    /// The hint line of a media field that takes videos. %@ is the largest size.
    static func videosUpTo(_ size: String) -> String {
        format("videosUpTo", size)
    }
    /// The hint line of a media field that takes videos. %@ is the longest length, such as 1 min, 30 sec.
    static func videosAtMost(_ length: String) -> String {
        format("videosAtMost", length)
    }
    /// The hint line of a media field that takes videos. %1$@ is the largest size, %2$@ the longest length.
    static func videosUpToAtMost(_ size: String, _ length: String) -> String {
        format("videosUpToAtMost", size, length)
    }
    /// Shown when a video is picked where only images are taken. %@ is the file name.
    static func videoNotAllowed(_ name: String) -> String {
        format("videoNotAllowed", name)
    }
    /// Shown when a picked file is too large. %1$@ is the file name, %2$@ the largest size.
    static func tooLarge(_ name: String, _ size: String) -> String {
        format("tooLarge", name, size)
    }
    /// Shown when a picked video is too long. %1$@ is the file name, %2$@ the longest length.
    static func tooLong(_ name: String, _ length: String) -> String {
        format("tooLong", name, length)
    }
    /// Shown when the length of a picked video cannot be read. %@ is the file name.
    static func durationUnreadable(_ name: String) -> String {
        format("durationUnreadable", name)
    }
    /// Shown while the length of a picked video is checked. %@ is the file name.
    static func checking(_ name: String) -> String {
        format("checking", name)
    }
    /// Shown while a file uploads. %@ is the file name.
    static func uploading(_ name: String) -> String {
        format("uploading", name)
    }
}
