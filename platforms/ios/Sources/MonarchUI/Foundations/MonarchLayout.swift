import CoreGraphics
import MonarchTokens

/// Widths and strokes for laying out content, from the generated
/// `MonarchTokens.Width` and `MonarchTokens.Layout` (the web's `width` and
/// `layout` tokens), in points.
///
/// ```swift
/// ScrollView {
///     article.frame(maxWidth: MonarchLayout.readingMax)
/// }
/// ```
///
/// On a phone the screen is narrower than every width here; they matter on
/// iPad, in Split View and on Mac with "Designed for iPad".
public enum MonarchLayout {
    /// The longest comfortable line of running text: 672 points (42rem).
    public static let readingMax: CGFloat = MonarchTokens.Layout.readingMax
    /// A narrow column, such as a settings page or a form: 704 points (44rem).
    public static let narrowWidth: CGFloat = MonarchTokens.Width.narrow
    /// The main content column: 1,152 points (72rem).
    public static let contentWidth: CGFloat = MonarchTokens.Width.content
    /// A wide content area, such as a dashboard: 1,280 points (80rem).
    public static let wideWidth: CGFloat = MonarchTokens.Width.wide
    /// The widest an app's content grows: 1,280 points (80rem).
    public static let contentMax: CGFloat = MonarchTokens.Layout.contentMax
    /// The border of a selected surface, such as a chosen card or row: 2 points.
    public static let selectedBorderWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The border of a surface at rest: 1 point.
    public static let borderWidth: CGFloat = MonarchTokens.Space.px
}
