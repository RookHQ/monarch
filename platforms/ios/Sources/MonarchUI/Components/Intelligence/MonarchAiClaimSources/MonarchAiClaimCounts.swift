/// How many claims there are at each support level.
@_spi(Experimental) public struct MonarchAiClaimCounts: Sendable, Hashable {
    /// All claims.
    public let total: Int
    /// The claims that are not fully supported.
    public let needsChecking: Int
    /// Supported claims.
    public let supported: Int
    /// Partly supported claims.
    public let partial: Int
    /// Claims no source supports.
    public let unsupported: Int
    /// Claims the sources disagree about.
    public let conflicting: Int
    /// Claims not checked yet.
    public let unchecked: Int

    /// Counts claims by support level.
    ///
    /// - Parameter claims: The claims.
    /// - Returns: The counts.
    public static func of(_ claims: [MonarchAiClaim]) -> MonarchAiClaimCounts {
        func count(_ support: MonarchAiClaimSupport) -> Int { claims.filter { $0.support == support }.count }
        return MonarchAiClaimCounts(
            total: claims.count,
            needsChecking: claims.filter(\.needsChecking).count,
            supported: count(.supported),
            partial: count(.partial),
            unsupported: count(.unsupported),
            conflicting: count(.conflicting),
            unchecked: count(.unchecked)
        )
    }

    /// The number of claims at one support level.
    ///
    /// - Parameter support: The support level.
    /// - Returns: The count.
    public func count(_ support: MonarchAiClaimSupport) -> Int {
        switch support {
        case .supported: supported
        case .partial: partial
        case .unsupported: unsupported
        case .conflicting: conflicting
        case .unchecked: unchecked
        }
    }
}
