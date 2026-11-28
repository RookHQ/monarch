/// Which of a message's alternatives is shown. A message has alternatives
/// after a reply was regenerated or a prompt was edited and resent.
public struct MonarchAiBranchPosition: Sendable, Hashable, Codable {
    /// The alternative that is shown, counted from 0.
    public var index: Int
    /// How many alternatives exist.
    public var count: Int

    /// A branch position.
    ///
    /// - Parameters:
    ///   - index: The alternative that is shown, counted from 0.
    ///   - count: How many alternatives exist.
    public init(index: Int, count: Int) {
        self.index = index
        self.count = count
    }
}
