import Foundation

/// What ``MonarchAvatarUpload`` hands over when a crop is saved.
public struct MonarchAvatarUploadResult: Sendable, Hashable {
    /// The cropped square picture, encoded in the chosen format.
    public var data: Data
    /// The format of `data`.
    public var format: MonarchAvatarUploadFormat
    /// The side of the picture in pixels.
    public var pixelSize: Int
    /// The square of the picked image that was kept, in its pixels after
    /// its orientation is applied.
    public var crop: MonarchAvatarCrop
    /// The file the person picked, unchanged.
    public var original: Data

    /// A saved crop.
    ///
    /// - Parameters:
    ///   - data: The cropped square picture.
    ///   - format: The format of `data`.
    ///   - pixelSize: The side of the picture in pixels.
    ///   - crop: The square of the picked image that was kept.
    ///   - original: The file the person picked.
    public init(
        data: Data,
        format: MonarchAvatarUploadFormat,
        pixelSize: Int,
        crop: MonarchAvatarCrop,
        original: Data
    ) {
        self.data = data
        self.format = format
        self.pixelSize = pixelSize
        self.crop = crop
        self.original = original
    }
}
