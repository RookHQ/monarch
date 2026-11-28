/// Replacements for the built-in strings of ``MonarchMediaUpload``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchMediaUploadLabels: Sendable {
    /// The title of the empty field that takes images. Built in: "Add an image"
    public var addImage: String?
    /// The title of the empty field that takes images and videos. Built in: "Add an image or video"
    public var addMedia: String?
    /// Shown when the upload throws an error without a description. Built in: "The upload failed. Try again."
    public var uploadFailed: String?
    /// The button that stops an upload. Built in: "Cancel upload"
    public var cancel: String?
    /// Announced after an upload is cancelled. Built in: "Upload cancelled"
    public var cancelled: String?
    /// Announced after an image is uploaded. Built in: "Image uploaded"
    public var imageUploaded: String?
    /// Announced after a video is uploaded. Built in: "Video uploaded"
    public var videoUploaded: String?
    /// The title of the area that replaces the image. Built in: "Replace image"
    public var replaceImage: String?
    /// The title of the area that replaces the video. Built in: "Replace video"
    public var replaceVideo: String?
    /// The button that removes the image. Built in: "Remove image"
    public var removeImage: String?
    /// The button that removes the video. Built in: "Remove video"
    public var removeVideo: String?
    /// Announced after the image is removed. Built in: "Image removed"
    public var imageRemoved: String?
    /// Announced after the video is removed. Built in: "Video removed"
    public var videoRemoved: String?
    /// The spoken name of the image preview. Built in: "Uploaded image"
    public var previewImage: String?
    /// The spoken name of the video preview. Built in: "Uploaded video"
    public var previewVideo: String?
    /// The lead-in of the warnings. Built in: "Uploaded with warnings."
    public var warnings: String?
    /// The button that closes the video player. Built in: "Close"
    public var close: String?
    /// The hint line for images. Receives the largest size, or `nil`. Built in: "Images up to 10 MB".
    public var imageHint: (@Sendable (String?) -> String)?
    /// The hint line for videos. Receives the largest size and the longest length, or `nil`. Built in: "Videos up to 20 MB, 1 min, 30 sec at most".
    public var videoHint: (@Sendable (String?, String?) -> String)?
    /// Shown when a video is picked where only images are taken. Receives the file name.
    public var videoNotAllowed: (@Sendable (String) -> String)?
    /// Shown when a file is too large. Receives the file name and the largest size.
    public var tooLarge: (@Sendable (String, String) -> String)?
    /// Shown when a video is too long. Receives the file name and the longest length.
    public var tooLong: (@Sendable (String, String) -> String)?
    /// Shown when a video's length cannot be read. Receives the file name.
    public var durationUnreadable: (@Sendable (String) -> String)?
    /// Shown while a video's length is checked. Receives the file name. Built in: "Checking clip.mov…".
    public var checking: (@Sendable (String) -> String)?
    /// Shown while a file uploads. Receives the file name. Built in: "Uploading photo.jpg".
    public var uploading: (@Sendable (String) -> String)?
    /// Replacements for the strings of the file picker area inside the field.
    public var fileUpload: MonarchFileUploadLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        addImage: String? = nil,
        addMedia: String? = nil,
        uploadFailed: String? = nil,
        cancel: String? = nil,
        cancelled: String? = nil,
        imageUploaded: String? = nil,
        videoUploaded: String? = nil,
        replaceImage: String? = nil,
        replaceVideo: String? = nil,
        removeImage: String? = nil,
        removeVideo: String? = nil,
        imageRemoved: String? = nil,
        videoRemoved: String? = nil,
        previewImage: String? = nil,
        previewVideo: String? = nil,
        warnings: String? = nil,
        close: String? = nil,
        imageHint: (@Sendable (String?) -> String)? = nil,
        videoHint: (@Sendable (String?, String?) -> String)? = nil,
        videoNotAllowed: (@Sendable (String) -> String)? = nil,
        tooLarge: (@Sendable (String, String) -> String)? = nil,
        tooLong: (@Sendable (String, String) -> String)? = nil,
        durationUnreadable: (@Sendable (String) -> String)? = nil,
        checking: (@Sendable (String) -> String)? = nil,
        uploading: (@Sendable (String) -> String)? = nil,
        fileUpload: MonarchFileUploadLabels = MonarchFileUploadLabels()
    ) {
        self.addImage = addImage
        self.addMedia = addMedia
        self.uploadFailed = uploadFailed
        self.cancel = cancel
        self.cancelled = cancelled
        self.imageUploaded = imageUploaded
        self.videoUploaded = videoUploaded
        self.replaceImage = replaceImage
        self.replaceVideo = replaceVideo
        self.removeImage = removeImage
        self.removeVideo = removeVideo
        self.imageRemoved = imageRemoved
        self.videoRemoved = videoRemoved
        self.previewImage = previewImage
        self.previewVideo = previewVideo
        self.warnings = warnings
        self.close = close
        self.imageHint = imageHint
        self.videoHint = videoHint
        self.videoNotAllowed = videoNotAllowed
        self.tooLarge = tooLarge
        self.tooLong = tooLong
        self.durationUnreadable = durationUnreadable
        self.checking = checking
        self.uploading = uploading
        self.fileUpload = fileUpload
    }
}
