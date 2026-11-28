/// The size of the tiles or stars of a rating picker. The touch target is at
/// least 44 points at both sizes.
public enum MonarchRatingPickerSize: Sendable, Hashable, CaseIterable {
    /// Tighter tiles and 20-point stars.
    case sm
    /// The default: 24-point stars.
    case md
}
