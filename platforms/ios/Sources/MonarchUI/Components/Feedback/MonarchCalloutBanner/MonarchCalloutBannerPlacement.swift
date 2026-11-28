/// Where a callout banner sits, which selects its shape.
public enum MonarchCalloutBannerPlacement: Sendable, Hashable, CaseIterable {
    /// A rounded, bordered callout inside the content. The default.
    case section
    /// A full-width strip with square corners and a line below it, for the
    /// top of a screen: trials, maintenance, billing.
    case page
}
