/// How ``MonarchConsentPreferences`` asks for the choice.
public enum MonarchConsentPreferencesMode: Sendable, Hashable, CaseIterable {
    /// One switch per category, then Save. The default.
    case categories
    /// No switches: each category is described with its status, and the person
    /// picks Reject all or Accept all. For a product with one optional category,
    /// where a switch would only repeat the buttons.
    case choice
}
