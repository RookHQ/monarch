/// How ``MonarchPeriodSelector`` shows its presets.
public enum MonarchPeriodSelectorVariant: String, Sendable, Hashable, CaseIterable {
    /// Every preset as a segment of the system segmented control. When the
    /// segments do not fit the width, or at the accessibility text sizes, the
    /// menu is shown instead. The default.
    case segmented
    /// A system pop-up menu showing the chosen preset, for toolbars and phones.
    case menu
}
