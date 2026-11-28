/// What a callout banner is for, which selects its density and colours.
public enum MonarchCalloutBannerVariant: Sendable, Hashable, CaseIterable {
    /// A message with a tone, an optional title and an action. The default.
    case callout
    /// A compact, neutral notice that the view is narrowed, for example
    /// "Showing tickets in Support", usually with an action that clears it.
    case scope
}
