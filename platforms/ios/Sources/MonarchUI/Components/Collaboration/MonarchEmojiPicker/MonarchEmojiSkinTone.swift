/// The skin tone applied to emoji that take one.
public enum MonarchEmojiSkinTone: Sendable, Hashable, CaseIterable {
    /// The emoji's own yellow.
    case none
    /// Fitzpatrick type 1 and 2.
    case light
    /// Fitzpatrick type 3.
    case mediumLight
    /// Fitzpatrick type 4.
    case medium
    /// Fitzpatrick type 5.
    case mediumDark
    /// Fitzpatrick type 6.
    case dark

    /// The Unicode modifier of the tone, or `nil` for none.
    public var modifier: Unicode.Scalar? {
        switch self {
        case .none: nil
        case .light: "\u{1F3FB}"
        case .mediumLight: "\u{1F3FC}"
        case .medium: "\u{1F3FD}"
        case .mediumDark: "\u{1F3FE}"
        case .dark: "\u{1F3FF}"
        }
    }
}
