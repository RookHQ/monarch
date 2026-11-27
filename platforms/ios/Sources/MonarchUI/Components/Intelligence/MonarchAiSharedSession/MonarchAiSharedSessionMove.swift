/// One move in the queue of a ``MonarchAiSharedSession``, passed to `onReorder` beside the new order.
@_spi(Experimental) public struct MonarchAiSharedSessionMove: Sendable, Hashable {
    /// The id of the request that moved.
    public var id: String
    /// Its old index.
    public var from: Int
    /// Its new index.
    public var to: Int

    /// A move.
    ///
    /// - Parameters:
    ///   - id: The id of the request that moved.
    ///   - from: Its old index.
    ///   - to: Its new index.
    public init(id: String, from: Int, to: Int) {
        self.id = id
        self.from = from
        self.to = to
    }
}
