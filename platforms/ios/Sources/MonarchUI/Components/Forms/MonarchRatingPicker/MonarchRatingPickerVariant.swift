/// How a rating picker draws its scale.
public enum MonarchRatingPickerVariant: Sendable, Hashable, CaseIterable {
    /// A wrapping row of tiles, each with its number and label. The default.
    case scale
    /// A row of stars, with the label of the chosen value beside them.
    case stars
}
