/// The file format of the picture ``MonarchAvatarUpload`` hands over.
public enum MonarchAvatarUploadFormat: Sendable, Hashable {
    /// PNG, lossless. The default.
    case png
    /// JPEG at a quality from 0 to 1.
    case jpeg(quality: Double)
}
