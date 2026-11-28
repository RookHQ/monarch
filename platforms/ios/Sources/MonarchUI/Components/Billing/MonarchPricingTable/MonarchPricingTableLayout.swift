/// How a ``MonarchPricingTable`` arranges its plans.
public enum MonarchPricingTableLayout: Sendable, Hashable, CaseIterable {
    /// Pages at compact width and accessibility text sizes, a grid otherwise. The default.
    case automatic
    /// One plan per page with its features below; swipe or pick a plan to move.
    case paged
    /// Plans as columns and features as rows.
    case grid
}
