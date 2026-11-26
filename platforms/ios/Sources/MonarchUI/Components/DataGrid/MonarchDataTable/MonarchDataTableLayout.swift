/// How a ``MonarchDataTable`` lays out its rows.
public enum MonarchDataTableLayout: Sendable, Hashable, CaseIterable {
    /// A table at regular width, a list at compact width and at the
    /// accessibility text sizes. The default.
    case automatic
    /// Always a SwiftUI `Table` with a column per field. From iOS 17.4;
    /// earlier systems show the list.
    case table
    /// Always a `List` with one row per record and a labelled line per field.
    case list
}
