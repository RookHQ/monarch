/// The size of the track and thumbs of a range slider. The touch target is
/// 44 points high at both sizes.
public enum MonarchRangeSliderSize: Sendable, Hashable, CaseIterable {
    /// A thinner track and smaller thumbs, for dense filter panels.
    case sm
    /// The default.
    case md
}
