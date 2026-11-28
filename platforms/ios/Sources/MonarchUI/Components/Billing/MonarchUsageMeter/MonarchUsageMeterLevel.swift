/// The level a metered item has reached.
enum MonarchUsageMeterLevel: Equatable, CaseIterable {
    /// Below the warning level.
    case normal
    /// At or above the warning level, and not above the limit.
    case warning
    /// Above the limit.
    case over
    /// The item has no limit.
    case unlimited
}
