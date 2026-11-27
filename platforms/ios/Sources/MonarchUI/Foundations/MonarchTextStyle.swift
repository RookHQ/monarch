/// The type roles of Monarch. The same names exist on every platform.
///
/// Apply one with `monarchTextStyle(_:)`. Every role
/// scales with Dynamic Type.
public enum MonarchTextStyle: Sendable, Hashable, CaseIterable {
    /// The largest text on a screen.
    case display
    /// A screen title.
    case title
    /// A section heading.
    case heading
    /// A heading inside a section.
    case subheading
    /// Running text.
    case body
    /// Running text with emphasis.
    case bodyStrong
    /// Control labels and field labels.
    case label
    /// Supporting text.
    case caption
    /// Code, in a monospaced face.
    case code
    /// Small code, in a monospaced face: keyboard keys, IDs and hashes in a
    /// caption. Added after the first nine roles.
    case codeSmall
    /// Running text in the small size, 14 points regular with the body line height: a description under a
    /// title, a row's second line. Added after the first ten roles.
    case bodySmall
    /// Running text in the extra small size, 12 points regular with the body line height: meta lines and
    /// footnotes that wrap. Added after the first ten roles.
    case bodyExtraSmall
}
