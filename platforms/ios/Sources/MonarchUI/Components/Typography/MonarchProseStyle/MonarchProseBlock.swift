/// The kind of a block of long-form text. It sets the type, colour and
/// spacing of the view it is given to with `monarchProseBlock(_:)`.
public enum MonarchProseBlock: Sendable, Hashable {
    /// A paragraph of body text. The default for a view without a kind.
    case paragraph
    /// A heading, level 1 to 6. Other levels are clamped to that range.
    case heading(_ level: Int)
    /// An item of a list. The marker, such as "•" or "3.", stands in a
    /// gutter before the text; `depth` counts the lists it is nested in,
    /// from 0.
    case listItem(marker: String, depth: Int = 0)
    /// A quotation, indented after a bar.
    case quote
    /// A block of code, monospaced, on a panel that scrolls sideways.
    case code
    /// A caption under a figure or table.
    case caption
    /// A thematic break. Give it to a `Divider`.
    case rule
}
