import CoreGraphics

/// The square of the picked image that ``MonarchAvatarUpload`` keeps, in the
/// image's own pixels, measured from its top-leading corner.
public struct MonarchAvatarCrop: Sendable, Hashable {
    /// The distance from the image's left edge.
    public var x: CGFloat
    /// The distance from the image's top edge.
    public var y: CGFloat
    /// The side of the square.
    public var size: CGFloat

    /// A crop square.
    ///
    /// - Parameters:
    ///   - x: The distance from the image's left edge, in pixels.
    ///   - y: The distance from the image's top edge, in pixels.
    ///   - size: The side of the square, in pixels.
    public init(x: CGFloat, y: CGFloat, size: CGFloat) {
        self.x = x
        self.y = y
        self.size = size
    }
}
