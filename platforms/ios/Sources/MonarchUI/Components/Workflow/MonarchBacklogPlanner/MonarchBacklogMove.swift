/// A finished move in a ``MonarchBacklogPlanner``.
@_spi(Experimental) public struct MonarchBacklogMove<ItemID: Hashable>: Hashable {
    /// The id of the item moved.
    public let itemID: ItemID
    /// The bucket it came from.
    public let fromBucketID: String
    /// Its position there, from 0.
    public let fromIndex: Int
    /// The bucket it went to.
    public let toBucketID: String
    /// Its position there, from 0.
    public let toIndex: Int

    /// A move.
    ///
    /// - Parameters:
    ///   - itemID: The id of the item moved.
    ///   - fromBucketID: The bucket it came from.
    ///   - fromIndex: Its position there.
    ///   - toBucketID: The bucket it went to.
    ///   - toIndex: Its position there.
    public init(itemID: ItemID, fromBucketID: String, fromIndex: Int, toBucketID: String, toIndex: Int) {
        self.itemID = itemID
        self.fromBucketID = fromBucketID
        self.fromIndex = fromIndex
        self.toBucketID = toBucketID
        self.toIndex = toIndex
    }
}

extension MonarchBacklogMove: Sendable where ItemID: Sendable {}
