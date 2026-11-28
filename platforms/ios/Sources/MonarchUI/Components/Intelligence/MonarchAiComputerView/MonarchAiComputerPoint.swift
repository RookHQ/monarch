/// A position on a screenshot of ``MonarchAiComputerView``, in screenshot
/// pixels from its top-left corner, whatever the layout direction.
@_spi(Experimental) public struct MonarchAiComputerPoint: Sendable, Hashable {
    /// Pixels from the left edge of the screenshot.
    public var x: Double
    /// Pixels from the top edge of the screenshot.
    public var y: Double

    /// A point.
    ///
    /// - Parameters:
    ///   - x: Pixels from the left edge of the screenshot.
    ///   - y: Pixels from the top edge of the screenshot.
    public init(x: Double, y: Double) {
        self.x = x
        self.y = y
    }
}
