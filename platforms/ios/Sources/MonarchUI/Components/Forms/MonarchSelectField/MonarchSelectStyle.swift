/// How a ``MonarchSelectField`` is drawn.
public enum MonarchSelectStyle: Sendable, Hashable, CaseIterable {
    /// A form field: the label above, a full-width box, the help or error line below.
    case field
    /// A compact trigger for a picker that sits in a toolbar, a filter row,
    /// a panel header or a table row: a hairline box 36 points high that is
    /// as wide as its text, with a chevron. It has no visible label, help or
    /// error line; the label is still spoken. The touch area stays 44 points
    /// high.
    case compact
}
